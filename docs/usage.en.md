# NotifyHub Usage Guide

> 中文版：[usage.md](usage.md)

> For first-time users: from "start the server" to "publish, receive and dynamically configure
> platforms in four languages", plus a troubleshooting cheat sheet.
> Spring Boot users can jump straight to [section 5.5](#55-spring-boot-auto-configuration).
> Companion docs: [`config.en.md`](config.en.md) (config reference), [`protocol.en.md`](protocol.en.md)
> (gRPC contract and delivery semantics), [`../README.en.md`](../README.en.md) (overview).

---

## 0. Mental Model (30 seconds)

NotifyHub is a **single-process gRPC service** that does two things:

1. **Forward notifications**: you `Publish(topic, title, content)` → the server routes by topic
   wildcard to the configured push platforms (DingTalk / WeCom / Feishu / any webhook) →
   asynchronous delivery (rate limit + retry + dedup + dead letter).
2. **Subscribe to topics**: you `Subscribe(["alert.*"])` → events published from any language are
   streamed to you in real time (at most once, not persisted).

```
your service ──gRPC(x-api-token)──▶ NotifyHub ──HTTP──▶ DingTalk / WeCom / Feishu / Webhook
                                       │
                                       └──stream──▶ subscribers in other languages
```

Clients need no HTTP, no signing and no knowledge of the bot address — all of that lives in the
server configuration.

---

## 1. Start the Server

### 1.1 Build (Java 21 + Gradle)

The repository does not ship a Gradle wrapper; use a system-installed `gradle`:

```bash
./scripts/gen-protos.sh java     # first run: generate Java gRPC stubs (downloads protoc into tools/)
gradle :server:installDist       # produces the distribution at server/build/install/server/
```

On Windows:

```bat
bash scripts\gen-protos.sh java
gradle :server:installDist
```

### 1.2 Prepare the Config

```bash
cp config.example.yaml config.yaml   # then edit: fill in the real webhook/secret and auth.tokens
```

### 1.3 Run

```bash
# Option A: explicit path
./server/build/install/server/bin/server --config config.yaml

# Option B: environment variable (what the Docker image uses)
export NOTIFYHUB_CONFIG=/etc/notifyhub/config.yaml
./server/build/install/server/bin/server

# Option C: no arguments → reads config.yaml from the working directory
cd server/build/install/server && ./bin/server
```

Windows:

```bat
server\build\install\server\bin\server.bat --config <repo-path>\config.yaml
```

Config path precedence: `--config` / `-c` argument > environment variable `NOTIFYHUB_CONFIG` >
`config.yaml` in the working directory.
Check the version with `server --version` (does not start the service).

Successful startup log:

```
[main] INFO io.notifyhub.Server - NotifyHub 0.1.0 已启动，监听 0.0.0.0:9987 (tokens=1, platforms=3, workers=16)
```

> Port conflicts, wrong platform types and YAML syntax errors all **fail fast at startup** — the
> server never runs in a half-broken state.

### 1.4 Docker

```bash
docker compose up --build -d      # mounts ./config.example.yaml to /etc/notifyhub/config.yaml
docker logs -f notifyhub
docker compose down
```

Change the mount source in `docker-compose.yml` to your own `config.yaml`. The image ships a TCP
9987 health check.

---

## 2. Minimal Runnable Config (for local verification)

Don't want to register a bot first? Use the `webhook` type against a local echo service to prove the
chain works:

```yaml
# config.yaml — minimal verification version (no auth + local webhook)
server:
  host: 127.0.0.1
  port: 9987

auth:
  tokens: []            # empty array = no auth, local debugging only

platforms:
  - name: local-hook
    type: webhook
    url: http://127.0.0.1:19800/hook
    topics: ["*"]
```

Then start an echo service (pick one):

```bash
python -c "from http.server import BaseHTTPRequestHandler,HTTPServer; \
HTTPServer(('127.0.0.1',19800), type('H',(BaseHTTPRequestHandler,),{'do_POST':lambda s:(print(s.rfile.read(int(s.headers['Content-Length']))), s.send_response(200), s.end_headers())})).serve_forever()"
```

For production, fill in `auth.tokens` (see the next section).

---

## 3. Configuration in Depth

The complete example is in [`config.example.yaml`](../config.example.yaml); every key is documented
in [`config.en.md`](config.en.md). This section only highlights the easy-to-miss parts.

### 3.1 server / auth

| Key | Default | Description |
|---|---|---|
| `server.host` | `0.0.0.0` | Listen address; use `127.0.0.1` for local debugging |
| `server.port` | `9987` | gRPC port |
| `server.workers` | CPU count (min 4) | Delivery worker thread count |
| `server.queue_capacity` | `10000` | Async delivery queue capacity; **when full, `Publish` returns `UNAVAILABLE`** |
| `auth.tokens` | `[]` | Token allowlist; **empty means no auth** (`Ping` is always auth-free) |

### 3.2 defaults (global defaults, overridable per platform)

```yaml
defaults:
  retry: { max_attempts: 3, backoff_ms: 500 }   # exponential backoff + jitter, capped at 30s
  rate_limit: { qps: 15 }                        # independent token bucket per platform (DingTalk caps at 20 qps)
  dedup_window_ms: 60000                         # dedup_key deduplication window
```

### 3.3 platforms[] (push platforms)

| Key | Required | Description |
|---|---|---|
| `name` | ✅ | Globally unique; identifies the platform for explicit `platforms` targeting and Admin removal |
| `type` | ✅ | `dingtalk` / `wecom` / `feishu` / `webhook` |
| `webhook` or `url` | ✅ | The two keys are equivalent; must start with `http(s)://` |
| `secret` | — | DingTalk/Feishu signing key; HMAC signing key for the webhook channel |
| `topics` | — | Routing rule array, defaults to `["*"]` |
| `template` | — | Placeholder template, defaults to `title\ncontent` |
| `at_mobiles` | — | DingTalk @ phone numbers |
| `sign_header` | — | webhook only, defaults to `X-Signature` |
| `retry` / `rate_limit_qps` | — | Override the global defaults |

Signing and success checks for the four channels:

| Channel | Signing | Message format | Success check |
|---|---|---|---|
| `dingtalk` | `urlencode(base64(HmacSHA256(secret, ts+"\n"+secret)))` appended to the URL | markdown, supports `at_mobiles` | `errcode==0` |
| `wecom` | None (the key is in the URL) | markdown, ~2000 char body limit | `errcode==0` |
| `feishu` | `base64(HmacSHA256(key=ts+"\n"+secret, data=""))` in the body | text | `code==0` |
| `webhook` | Optional `hex(HmacSHA256(secret, body))` in the signature header | JSON: `{event_id, topic, title, content, params, timestamp}` | HTTP 2xx |

---

## 4. Core Concepts

### 4.1 Topics and Wildcard Routing

Dot-separated segments, AMQP style:

| Rule | Meaning | Example |
|---|---|---|
| `alert` | Exact match | matches only `alert` |
| `alert.*` | `*` is exactly one segment | matches `alert.db`, not `alert` or `alert.db.slow` |
| `ops.#` | `#` is zero or more segments, **last segment only** | matches `ops`, `ops.a`, `ops.a.b` |
| `*` | One segment | commonly used as a catch-all: `"*"` |

A single publish can hit multiple platforms (all of them are queued, in config declaration order).

### 4.2 Template Placeholders

```yaml
template: "**{{title}}**\n{{content}}\n> topic={{topic}} host={{params.host}}"
```

Supported: `{{title}}`, `{{content}}`, `{{topic}}`, `{{event_id}}`, `{{params.xxx}}`, or simply
`{{xxx}}` (equivalent to `params.xxx`). Unknown placeholders are replaced with an empty string.
When the template is empty, each channel uses its own default format.

### 4.3 Deduplication (suppressing alert storms)

Pass a `dedup_key` when publishing:

```python
client.publish("alert.db", "Disk alert", "...", dedup_key="db-01-disk")
```

**Subsequent** publishes with the same key inside the window (default 60s, controlled by
`defaults.dedup_window_ms`) return `accepted=false, deduplicated=true` and are **neither pushed to
platforms nor broadcast to subscribers**.
Typical usage: `dedup_key = f"{alert_name}:{instance}"` so the same alert from the same instance
fires only once per 60s.

### 4.4 Delivery Semantics

1. `Publish` only performs "route + dedup + enqueue" and **returns synchronously**; the actual
   HTTP delivery is asynchronous.
2. Each platform has an independent token bucket rate limit (default 15 qps, DingTalk recommends ≤20).
3. Failures retry with `backoff_ms × 2^n + jitter`, capped at 30s per backoff.
4. Retries exhausted → an ERROR dead-letter log entry is written and the event is broadcast to the
   built-in topic **`deadletter`**. Subscribe to it to monitor delivery failures in real time:

```python
client.subscribe(["deadletter"], lambda e: print("delivery failed:", e.params))
# params contains platform / topic / event_id / error
```

### 4.5 Subscription Semantics (important)

- **At most once**: no persistence, no replay, no redelivery.
- If a subscriber is too slow, events are dropped (the server keeps one queue per subscription, so a
  slow consumer does not block others).
- Disconnecting unsubscribes automatically; you can also `cancel()` / `close()` the handle explicitly.
- For "must not lose messages" scenarios: have the subscriber persist events to its own storage, or
  wait for V2 durable delivery.

---

## 5. SDKs in Four Languages

> Conventions below: server at `localhost:9987`, token `ntf_xxx`.
> All SDKs are **thin wrappers**: the token is injected via the gRPC metadata entry `x-api-token`,
> and underneath it is just those 6 methods from the proto.

### 5.1 Python

```bash
pip install -e sdks/python
```

```python
from notifyhub import NotifyClient, PublishRequest   # PublishRequest is for batch publishing

with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    # 1) Health check (no auth required)
    print(client.ping())              # {'version': '0.1.0', 'uptime_seconds': '12'}

    # 2) Publish
    ack = client.publish(
        "alert.db", "Disk alert", "db-01 at 95%",
        params={"host": "db-01"},     # template variables
        # platforms=["dingtalk-alert"],  # explicit targets, overrides topic routing
        # dedup_key="db-01-disk",        # deduplication
        # skip_subscribers=False, skip_platforms=False,
        # timeout=10.0,
    )
    print(ack.accepted, ack.event_id, ack.matched_platforms, ack.deduplicated, ack.error)

    # 3) Subscribe (callbacks fire on a background thread)
    sub = client.subscribe(["alert.*"], lambda e: print("received:", e.topic, e.title))
    ...
    sub.cancel()                      # or sub.close()

    # 4) Batch publish (bidi stream, a single failure does not break the batch)
    from notifyhub import PublishRequest
    reqs = [PublishRequest(topic="alert.db", title="t1"),
            PublishRequest(title="an illegal request with no topic")]
    for a in client.publish_batch(reqs):
        print(a.accepted, a.error)

    # 5) Admin
    client.upsert_platform(name="ding-alert", type="dingtalk",
                           webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
                           secret="SECxxx", topics=["alert"], template="**{{title}}**\n{{content}}")
    print(client.list_platforms())
    client.remove_platform("ding-alert")
```

Exceptions: `NotifyError` (with `.code` = gRPC StatusCode and `.details`). The client is
**thread-safe and can be shared across threads**.

After changing the proto, regenerate the stubs: `./scripts/gen-protos.sh python`

### 5.2 TypeScript / JavaScript

```bash
cd sdks/typescript && npm install && npm run build
```

```js
const { NotifyClient } = require("./sdks/typescript/dist/client.js");

const client = new NotifyClient("localhost:9987", "ntf_xxx");

const sub = client.subscribe(["alert.*"], (e) => console.log("received:", e.topic, e.title));
await new Promise(r => setTimeout(r, 300));   // wait for the subscription to take effect

const ack = await client.publish("alert.db", "Disk alert", "db-01 at 95%", {
  params: { host: "db-01" },
  // platforms: ["dingtalk-alert"],
  // dedupKey: "db-01-disk",
  // skipSubscribers: false, skipPlatforms: false,
});
console.log(ack.accepted, ack.eventId, ack.matchedPlatforms);

// Batch publish (bidi stream, a single failure does not break the batch)
const acks = await client.publishBatch([
  { topic: "alert.db", title: "t1" },
  { title: "an illegal request with no topic" },     // -> accepted=false, error=INVALID_ARGUMENT: ...
]);
console.log(acks.map(a => a.accepted), acks[1].error);

await client.upsertPlatform({ name: "ding-alert", type: "dingtalk",
  webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx",
  secret: "SECxxx", topics: ["alert"] });
console.log(await client.listPlatforms());
await client.removePlatform("ding-alert");

sub.close();
client.close();
```

> Mind the field names: the TS side uses **camelCase** (`eventId` / `matchedPlatforms` / `dedupKey` /
> `rateLimitQps`), the Python side uses **snake_case**.
> At runtime it loads `proto/notify/v1/notify.proto` via `@grpc/proto-loader` — no code generation needed.

### 5.3 Java

Gradle dependency (inside this repo you can simply use `implementation(project(":sdk-java"))`; for
external projects run `gradle publishNotifyHubToMavenLocal` first, then reference
`io.github.huangwenfu750:sdk-java:0.1.0`):

```java
try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987).token("ntf_xxx").build()) {
    // Publish
    PublishAck ack = client.publish(PublishRequest.newBuilder()
            .setTopic("alert.db").setTitle("Disk alert").setContent("db-01 at 95%")
            .putParams("host", "db-01")
            .setOptions(Options.newBuilder().setDedupKey("db-01-disk").build())
            .build());
    System.out.println(ack.getAccepted() + " " + ack.getEventId());

    // Async publish
    CompletableFuture<PublishAck> f = client.publishAsync(request);

    // Batch publish (bidi stream, avoids per-message RPC overhead; a single error does not break the stream)
    var session = client.publishStream(ack -> log.info("{}", ack.getEventId()));
    session.send(req1);
    session.send(req2);
    session.complete();
    session.await(10, TimeUnit.SECONDS);

    // Subscribe (callback runs on a gRPC thread — do not block!)
    SubscriptionHandle sub = client.subscribe(List.of("alert.*"),
            e -> System.out.println("received: " + e.getTopic()));
    ...
    sub.cancel("done");

    // Admin
    client.upsertPlatform(PlatformConfig.newBuilder()
            .setName("ding-alert").setType("dingtalk")
            .setWebhook("https://oapi.dingtalk.com/robot/send?access_token=xxx")
            .setSecret("SECxxx").addTopics("alert").build());
    client.platforms().forEach(p -> System.out.println(p.getName()));
    client.removePlatform("ding-alert");

    client.ping();
}
```

For TLS: `.usePlaintext(false)`. The token interceptor `NotifyClient.TokenInterceptor` is public and
can be attached to a channel you build yourself.

### 5.4 Go

```bash
./scripts/gen-protos.sh go     # needs a local Go toolchain, generates sdks/go/gen
cd sdks/go && go mod tidy
```

```go
client, err := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
if err != nil { panic(err) }
defer client.Close()
ctx := context.Background()

ack, err := client.Publish(ctx, "alert", "Deployment finished", "v1.2.0 is live")
fmt.Println(ack.Accepted, ack.EventId)

// Batch publish (bidi stream, a single failure does not break the batch)
acks, err := client.PublishBatch(ctx, []*v1.PublishRequest{
    {Topic: "alert.db", Title: "t1"},
    {Title: "an illegal request with no topic"},   // -> Accepted=false, Error="INVALID_ARGUMENT: ..."
})

sub, _ := client.Subscribe(ctx, []string{"alert.*"})
defer sub.Close()
go func() { for ev := range sub.Events() { fmt.Println("received:", ev.Topic, ev.Title) } }()

client.UpsertPlatform(ctx, &v1.PlatformConfig{
    Name: "ding-alert", Type: "dingtalk",
    Webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx",
    Secret: "SECxxx", Topics: []string{"alert"},
})
```

> Go stubs must be generated (`sdks/go/gen` is not committed); once generated, the module compiles.
> Plugins are installed via `go install`; if `proxy.golang.org` is unreachable, the script
> automatically falls back to `goproxy.cn`.

### 5.5 Spring Boot (Auto-Configuration)

The repo ships a starter: `sdks/spring-boot` (`io.github.huangwenfu750:notifyhub-spring-boot-starter`).

```kotlin
implementation(project(":sdk-spring-boot"))   // inside this repo
// implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.0")  // after gradle publishNotifyHubToMavenLocal
```

`application.yml`:

```yaml
notifyhub:
  host: localhost
  port: 9987
  token: ntf_xxx
  platforms:                      # optional: register via Admin RPC at startup
    - name: ding-alert
      type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      secret: SECxxx
      topics: [alert, ops.*]
  subscriber:                     # optional: subscribe at startup
    enabled: true
    topics: [alert.*]
```

Inject and use:

```java
notify.publish("alert.db", "Disk alert", "db-01 at 95%", Map.of("host", host));

// Batch: send everything over one bidi stream and collect the acks (a single failure only shows up on its ack)
List<PublishAck> acks = notify.publishBatch(requests, Duration.ofSeconds(5));

@EventListener
public void onNotify(NotifyEvent e) { log.warn("{} {}", e.getTopic(), e.getTitle()); }
```

A few conventions:

- Auto-configures `NotifyClient` (`destroyMethod="close"`) and `NotifyHubTemplate`, all guarded by
  `@ConditionalOnMissingBean` — **declaring your own `@Bean` with the same name takes over completely**
  (switch to TLS, change the target, add interceptors, ...).
- When `spring-boot-actuator` is present, `NotifyHubHealthIndicator` is registered automatically and
  `/actuator/health` gains a `notifyhub` entry.
- When Micrometer is present, `notifyhub.publish.total` (tagged `topic`, `outcome`) and
  `notifyhub.publish.duration` are recorded automatically; batch publishing is recorded separately
  under `notifyhub.publish.batch.*`. Without Micrometer the overhead is zero, and you can also
  register your own `NotifyPublishListener` bean to take over.
- `notifyhub.platforms` goes through the Admin RPC and **lives only in server memory** — after a
  server restart you must re-register (or write it into the server YAML).
- Master switch `notifyhub.enabled=false` disables everything; `notifyhub.fail-fast=true` makes a
  platform registration failure abort startup.

Full details in [`sdks/spring-boot/README.en.md`](../sdks/spring-boot/README.en.md);
a runnable project is in [`examples/spring-boot/`](../examples/spring-boot/README.md) (includes a
local echo service so you can see the delivered JSON end to end).

> Gotcha: the starter's subscription is wired to Spring's `ApplicationStartedEvent` (earlier than
> `CommandLineRunner`), so messages sent from startup tasks are still received; but establishing the
> gRPC stream still takes tens of milliseconds — in one-shot tasks, sleep 300ms before publishing.

### 5.6 Raw gRPC (no SDK)

Any language with gRPC support can call `proto/notify/v1/notify.proto` directly (fully-qualified
methods `notify.v1.Notify/*`); just attach the `x-api-token` metadata entry on every call:

```bash
grpcurl -plaintext -H 'x-api-token: ntf_xxx' -d '{"topic":"alert.db","title":"t","content":"c"}' \
  localhost:9987 notify.v1.Notify/Publish
grpcurl -plaintext localhost:9987 notify.v1.Notify/Ping
```

For high-frequency batches use the bidi stream `PublishStream`: an error in one message (empty
topic, unknown platform) **does not break the stream** — it returns `accepted=false, error="..."`.
All four SDKs wrap it: Java (`publishStream` / `publishBatch`), Python (`publish_batch`),
TypeScript (`publishBatch`), Go (`PublishBatch`).

---

## 6. Runtime Management (Admin)

Platform configuration has two sources:

| Source | Takes effect | Persisted |
|---|---|---|
| YAML `platforms[]` | Loaded at startup | ✅ in the config file |
| `UpsertPlatform` RPC | Immediately | ❌ memory only, lost on restart |

Recommendation: **write steady-state platforms into YAML, use Admin for temporary/multi-tenant
scenarios** (re-register after a restart).
Admin validates that `name/type/webhook` are non-empty, that `type` is a registered channel, and that
`webhook` starts with `http(s)://`, otherwise `INVALID_ARGUMENT`; same name overwrites;
`RemovePlatform` returns `NOT_FOUND` for unknown names.

---

## 7. Troubleshooting Cheat Sheet

| Symptom / status code | Cause | Fix |
|---|---|---|
| `UNAUTHENTICATED` | Missing `x-api-token` or not in the allowlist | Check that `auth.tokens` and the client token match |
| `INVALID_ARGUMENT: topic 不能为空` | Publish without a topic | Supply a topic |
| `NOT_FOUND: 平台不存在: X` | `platforms` names an unregistered platform / removing an unknown name | Call `list_platforms()` first to confirm |
| `UNAVAILABLE: 投递队列已满` | Traffic burst filled `queue_capacity` | Back off and retry client-side; raise `queue_capacity` / `workers`, or lower per-platform qps |
| `accepted=false, deduplicated=true` | Hit the dedup window | Expected behavior — change the `dedup_key` or wait for the window to pass |
| No message received but `accepted=true` | ① published before the subscription was established ② wildcard does not match ③ the platform really failed | Sleep briefly after subscribing; re-check the wildcard; subscribe to `deadletter` to see delivery errors |
| `未知平台类型` at startup | Typo in `type` | Only `dingtalk / wecom / feishu / webhook` are valid |
| No DingTalk message | Wrong signing secret / bot security settings / 20 qps rate limit | Check the server ERROR log; set `rate_limit_qps` to ≤20 |
| Wrong message format | Template not applied or unknown placeholder used | Only `title/content/topic/event_id/params.xxx` are supported |

Server logs (slf4j-simple, straight to stdout) are the primary investigation tool: successful
delivery is `INFO`, failure is `WARN` (with the attempt number), dead letter is `ERROR`.

### Spring Boot specific

| Symptom | Cause | Fix |
|---|---|---|
| No "NotifyHub 平台已注册" startup log | `notifyhub.platforms` not configured, or registration failed and `fail-fast=false` only logged a WARN | Search the startup log for "NotifyHub 平台注册失败"; for a hard requirement set `fail-fast=true` |
| `notifyhub.platforms[].name 不能为空` at startup | A platform in yml has no name | Add `name` (Admin uses it as the unique identifier) |
| `@EventListener` receives nothing | ① `subscriber.enabled` is not true ② topics do not match the published topic ③ short-lived app exits right after publishing | Check the startup log for "NotifyHub 已订阅"; in one-shot tasks sleep 300ms before publishing |
| `UNAUTHENTICATED` right at startup | Platform registration/health probe used an invalid token | Verify `notifyhub.token` against the server's `auth.tokens` |
| No `notifyhub` in `/actuator/health` | Project does not depend on `spring-boot-actuator` | Add the dependency; the health check uses the auth-free `Ping` |
| Using Spring Boot 4.x | — | No impact: the starter's Spring dependencies are `compileOnly`, so your project's version is used at runtime (3.x or 4.x) |

---

## 8. Testing & Benchmarking

```bash
gradle test                            # unit + E2E (WireMock stubs the platform HTTP calls)
```

Cross-process smoke tests (four language SDKs against a real server):

```bash
./server/build/install/server/bin/server --config smoke/config.yaml &

.venv/Scripts/python.exe smoke/py_smoke.py            # Windows
python3 smoke/py_smoke.py                             # Linux/macOS
node smoke/node_smoke.js                              # requires cd sdks/typescript && npm run build first
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token        # override with -Ptarget=host:port
cd smoke/go && go run .                               # requires ./scripts/gen-protos.sh go first
```

Throughput baseline (laptop CPU, Python client benchmark, client is the bottleneck): ~**5.7k msg/s,
p99=3.8ms**. Re-measure server capacity with `ghz`:

```bash
ghz --insecure -n 100000 -c 64 --call notify.v1.Notify/Publish \
    -d '{"topic":"bench","title":"t","content":"c"}' -H "x-api-token: ntf_xxx" 127.0.0.1:9987
```

`scripts/bench.py` is the bundled Python benchmark script.

---

## 9. Known Limits (current version 0.1.0)

- Subscription is **at most once**: not persisted, not replayed; a slow subscriber drops events.
- Platform delivery is **asynchronous and best-effort**: `Publish` returning `accepted=true` only
  means the message was queued, not that the platform received it.
- Platforms registered via Admin disappear after a restart.
- No web console and no delivery history query yet (planned for V2).
- Browser access requires a gRPC-Web gateway (planned for V2).

---

## 10. Three-Minute Checklist

1. `./scripts/gen-protos.sh java && gradle :server:installDist`
2. Write a minimal `config.yaml` (see section 2: no auth + local webhook)
3. Start the echo service (19800) and NotifyHub (9987)
4. `pip install -e sdks/python`, run `examples/python/publish.py`
5. Once the echo service prints the JSON, the chain works → switch to a real bot and turn on `auth.tokens`

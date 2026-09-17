# NotifyHub Spring Boot Starter

> 中文版：[README.md](README.md)

Configure NotifyHub in `application.yml`, inject `NotifyHubTemplate` and you are done.

## 1. Add the Dependency

Inside this repo:

```kotlin
implementation(project(":sdk-spring-boot"))
```

External projects — **on Maven Central since 0.1.1**, no token needed:

```kotlin
repositories { mavenCentral() }

implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")
```

Or use the local repository (run `gradle publishNotifyHubToMavenLocal` first — it publishes
`protos`, `sdk-java` and the starter together):

```kotlin
repositories { mavenLocal() }

implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")
```

Or pull it straight from GitHub Packages (that registry needs a token even for reads):

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/huangwenfu750/notifyhub")
        credentials {                                  // prefer env vars, do not hardcode
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")   // PAT with read:packages
        }
    }
}
```

Maven users: see the `settings.xml` snippet in [README.en.md](../../README.en.md#install-the-sdks-package-registries).

> Spring-related dependencies are `compileOnly` in the starter; your project provides them at
> runtime, so both 3.x and 4.x work.
> The compiled-against version is controlled by `springBootVersion` in `gradle.properties`.

## 2. application.yml

```yaml
notifyhub:
  host: localhost          # default localhost
  port: 9987               # default 9987
  token: ntf_xxx           # one of the server's auth.tokens entries
  # plaintext: true        # plaintext by default; set to false for TLS
  # fail-fast: false       # whether a platform registration failure aborts startup (default: warn only)

  # Optional: register/overwrite push platforms via Admin RPC at startup
  platforms:
    - name: ding-alert
      type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      secret: SECxxx
      topics: [alert, ops.*]
      template: "**{{title}}**\n{{content}}"
      rate-limit-qps: 8

  # Optional: subscribe at startup; events are broadcast as Spring ApplicationEvents
  subscriber:
    enabled: true
    topics: [alert.*, deploy]
```

## 3. Publish

```java
@Service
public class AlertService {

    private final NotifyHubTemplate notify;

    public AlertService(NotifyHubTemplate notify) {
        this.notify = notify;
    }

    public void alert(String host) {
        notify.publish("alert.db", "Disk alert", "db-01 at 95%", Map.of("host", host));
    }
}
```

## 4. Batch Publish

For high-frequency scenarios use the bidi stream to avoid per-request RPC overhead; **a single
failure does not break the stream**, it only shows up on the corresponding ack:

```java
List<PublishAck> acks = notify.publishBatch(requests, Duration.ofSeconds(5));
acks.forEach(a -> { if (!a.getAccepted()) log.warn("failed: {}", a.getError()); });

// When you want to control the pacing yourself
var session = notify.publishStream(ack -> log.info("{}", ack.getEventId()));
session.send(req1); session.send(req2);
session.complete();
```

## 5. Receive

```java
@Component
class NotifyListener {

    @EventListener
    public void onNotify(NotifyEvent e) {
        log.warn("[{}] {} {}", e.getTopic(), e.getTitle(), e.getContent());
    }
}
```

> The subscription bridge is wired to Spring's `ApplicationStartedEvent` (**earlier than**
> `CommandLineRunner`/`ApplicationRunner`), so messages sent from startup tasks are still received.
> Establishing the gRPC stream still takes tens of milliseconds — in one-shot tasks,
> `sleep(300)` before publishing.

You can also subscribe manually at any time:

```java
var handle = notify.subscribe(List.of("ops.#"), e -> log.info(e.getTitle()));
...
handle.cancel("done");
```

## 6. Operations & Metrics

- When `spring-boot-actuator` is present, `NotifyHubHealthIndicator` is registered automatically and
  `/actuator/health` gains a `notifyhub` entry (using the auth-free `Ping`).
- When `micrometer-core` is present (actuator brings it in), `MicrometerPublishListener` is
  registered automatically and produces:

| Metric | Type | Description |
|---|---|---|
| `notifyhub.publish.total` | Counter | Single `publish`, tagged `topic` and `outcome=accepted\|deduplicated\|rejected\|error` |
| `notifyhub.publish.duration` | Timer | Single `publish` latency (no topic tag, to avoid cardinality explosion) |
| `notifyhub.publish.batch.duration` | Timer | Whole-batch latency of `publishBatch` |
| `notifyhub.publish.batch.size` | DistributionSummary | Number of messages per batch |

> Batches are measured per batch: on a bidi stream you cannot precisely attribute latency to each
> individual request, so the single-publish latency semantics are not reused, and batches are
> **not** counted in `notifyhub.publish.total`.

Skip Micrometer if you don't want metrics; you can also register your own `NotifyPublishListener`
bean to override the default. Exceptions thrown from metric callbacks are swallowed and never
affect your publishes.
- `notify.ping()` / `notify.isReachable()` let you probe liveness explicitly.
- `notify.platforms()` / `notify.upsertPlatform(...)` / `notify.removePlatform(...)` manage
  push platforms at runtime.

## 7. Configure in Code (Full Takeover)

Every auto-configured bean is guarded by `@ConditionalOnMissingBean`; declare a bean with the same
name to override it:

```java
@Configuration
public class NotifyConfig {

    @Bean(destroyMethod = "close")
    public NotifyClient notifyClient(NotifyHubProperties props) {
        return NotifyClient.newBuilder(props.target())
                .token(props.getToken())
                .usePlaintext(false)
                .build();
    }

    @Bean
    public NotifyHubTemplate notifyHubTemplate(NotifyClient client) {
        return new NotifyHubTemplate(client);
    }
}
```

You can also register platforms dynamically in `@PostConstruct` / `ApplicationRunner`:

```java
notify.upsertPlatform(PlatformConfig.newBuilder()
        .setName("wecom-ops").setType("wecom")
        .setWebhook("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx")
        .addTopics("ops.#").build());
```

## 8. Configuration Properties

| Property | Default | Description |
|---|---|---|
| `notifyhub.enabled` | `true` | Master switch |
| `notifyhub.host` / `notifyhub.port` | `localhost` / `9987` | Combined into the gRPC target |
| `notifyhub.token` | — | API token |
| `notifyhub.plaintext` | `true` | false = enable TLS |
| `notifyhub.fail-fast` | `false` | Whether a platform registration failure aborts startup |
| `notifyhub.platforms[].{name,type,webhook,secret,topics,template,at-mobiles,extra,rate-limit-qps}` | — | Platforms registered at startup |
| `notifyhub.subscriber.enabled` / `notifyhub.subscriber.topics` | `false` / `[]` | Subscribe at startup and broadcast Spring events |

> Note: `platforms` goes through the Admin RPC and **lives only in server memory** — after a server
> restart you must re-register (restarting the app is enough, or write it into the server YAML).

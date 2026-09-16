# NotifyHub

[![CI](https://github.com/huangwenfu750/notifyhub/actions/workflows/ci.yml/badge.svg)](https://github.com/huangwenfu750/notifyhub/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/huangwenfu750/notifyhub)](https://github.com/huangwenfu750/notifyhub/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 中文版：[README.md](README.md)

**Multi-language notification push service**: single-process deployment, clients in any language connect over gRPC.
Notifications are pushed to DingTalk / WeCom / Feishu / arbitrary Webhooks, plus topic-wildcard based event subscription.
Push platforms are configured either through a **YAML config file** or from **code** (Admin RPC).

```text
                        ┌─────────────────────────────────────────┐
 Java ┐                 │              NotifyHub Server           │
 Go   │   gRPC          │  Publish ── route (topic wildcard) ─→   │──→ DingTalk bot
 Python ├──────────────→│  Subscribe ←─ registry ── broadcast     │──→ WeCom bot
 JS/TS │  (x-api-token) │  Admin    ── configure platforms        │──→ Feishu bot
 ...  ┘                 │  queue+retry+rate limit+dedup+deadletter│──→ Generic webhook
                        └─────────────────────────────────────────┘
```

## Features

- **Multi-language**: proto3 + gRPC protocol; official SDKs for Java, Python, TypeScript/JS and Go (see `sdks/`)
- **Spring Boot ready**: `notifyhub-spring-boot-starter` — set host/port/token in `application.yml`, inject `NotifyHubTemplate` and go
- **Publish / subscribe**: `Publish` pushes to platforms and broadcasts; `Subscribe` receives a server stream (wildcards `*`/`#`, at-most-once)
- **Pluggable push platforms**: YAML config file, Admin RPC from code, token authentication
- **Production-grade delivery**: per-platform token bucket rate limiting, exponential backoff retry, `dedup_key` to suppress alert storms, dead-letter topic `deadletter`
- **Designed to be rewritten**: the Java implementation is the reference one; the proto contract plus language-agnostic test assets guarantee a smooth future rewrite to Go/Rust/Zig (see [docs/rewrite.en.md](docs/rewrite.en.md))

## Quick Start

### 1. Start the server

```bash
# Generate Java stubs and build (protoc is downloaded automatically on first run)
./scripts/gen-protos.sh java
gradle :server:installDist

# Copy and edit the config
cp config.example.yaml config.yaml   # fill in your bot webhook/secret and auth.tokens

./server/build/install/server/bin/server --config config.yaml
# [main] INFO io.notifyhub.Server - NotifyHub 0.1.0 已启动，监听 0.0.0.0:9987
```

Docker (two images, pick either):

```bash
# 1) Runtime image (recommended): drops the prebuilt distribution into a JRE base image, no compile inside the container
./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.0-linux-x86_64-nojre.tar.gz packaging/docker/
cp config.example.yaml packaging/docker/config.yaml   # fill in auth.tokens / platforms
cd packaging/docker && docker compose up -d --build

# 2) Source image: runs the Gradle build inside the container (good for CI, needs more machine resources)
cp config.example.yaml config.yaml                    # fill in auth.tokens / platforms
docker compose up --build -d
```

Both expose 9987 on the host, mount the config read-only at `/etc/notifyhub/config.yaml`,
and ship a TCP health check; see [packaging/docker/README.en.md](packaging/docker/README.en.md) for details.

Linux distribution package (bundles JRE 21, unpack and run, no preinstalled Java needed):

```bash
./scripts/package-linux.sh   # produces build/linux/notifyhub-0.1.0-linux-x86_64.tar.gz

# On the target machine
tar -xzf notifyhub-0.1.0-linux-x86_64.tar.gz
sudo ./notifyhub-0.1.0-linux-x86_64/install.sh   # installs to /opt/notifyhub and registers systemd
sudo systemctl start notifyhub
```

You can also skip the system service and run `./bin/notifyhub start|stop|status|--config etc/my.yaml` directly.
See [packaging/linux/share/doc/README.md](packaging/linux/share/doc/README.md).

### 2. Publish a notification (any language)

```java
// Java (sdks/java)
try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987).token("ntf_xxx").build()) {
    client.publish("alert", "Deployment finished", "v1.2.0 is live");
}
```
```python
# Python (sdks/python, pip install -e sdks/python)
with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "Deployment finished", "v1.2.0 is live")
```
```ts
// TypeScript/JS (sdks/typescript)
const client = new NotifyClient("localhost:9987", "ntf_xxx");
await client.publish("alert", "Deployment finished", "v1.2.0 is live");
```
```go
// Go (sdks/go, run ./scripts/gen-protos.sh go first)
client, _ := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
client.Publish(ctx, "alert", "Deployment finished", "v1.2.0 is live")
```
```java
// Spring Boot (sdks/spring-boot): configure host/port/token in application.yml, then inject
notify.publish("alert", "Deployment finished", "v1.2.0 is live");
```

### 3. Subscribe to topics

```python
sub = client.subscribe(["alert.*"], print)   # events arrive in real time; sub.cancel() unsubscribes
```

### 4. Configure push platforms from code

```python
client.upsert_platform(
    name="ding-alert", type="dingtalk",
    webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
    secret="SECxxx", topics=["alert"],
)
```

## Configuration

See [docs/config.en.md](docs/config.en.md) and [config.example.yaml](config.example.yaml).

Protocol and delivery semantics (including per-channel signature algorithms) are in
[docs/protocol.en.md](docs/protocol.en.md).

The step-by-step guide lives in [docs/usage.en.md](docs/usage.en.md) (Spring Boot users: section 5.5).

## Install the SDKs (package registries)

| Language | Install | Status |
|---|---|---|
| Go | `go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.0` | ✅ published (distributed via `sdks/go/v*` tags) |
| Java / Kotlin | `implementation("io.github.huangwenfu750:sdk-java:0.1.0")` | ✅ published to GitHub Packages |
| Spring Boot | `implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.0")` | ✅ published to GitHub Packages |
| TypeScript / JS | `npm i notifyhub-client` | ⏳ pending (npm / pnpm / yarn / bun share one registry) |
| Python | `pip install notifyhub-client` | ⏳ pending |

Pulling Maven artifacts from GitHub Packages requires declaring the repository (that registry needs
authentication even for reads):

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/huangwenfu750/notifyhub")
        credentials {                                  // prefer env vars, do not hardcode
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Server distributions are on the [Releases](https://github.com/huangwenfu750/notifyhub/releases) page:
`notifyhub-<ver>-linux-x86_64.tar.gz` (bundles JRE 21, unpack and run) and the `-nojre` slim
variant, each with a `.sha256`.

## Release Process (maintainers)

1. `git tag v0.2.0 && git push origin v0.2.0`
   → `release.yml` builds the Linux packages in CI and creates the Release with the artifacts attached
2. Also tag the Go submodule (the prefix must match the module path, otherwise `go get` fails):
   `git tag sdks/go/v0.2.0 && git push origin sdks/go/v0.2.0`
3. `publish.yml` pushes the language packages once the Release exists. To enable npm / PyPI, add
   `NPM_TOKEN` and `PYPI_API_TOKEN` under Settings → Secrets, then re-run that workflow.

## Build & Test

```bash
./scripts/gen-protos.sh java          # generate Java stubs (protoc is downloaded into tools/)
gradle test                           # all unit + E2E tests (WireMock stubs the platform HTTP calls)
gradle :server:installDist            # distribution: server/build/install/server/
```

CI (`.github/workflows/ci.yml`) runs exactly the above plus the four-language cross-process smoke tests.

## Publishing to Maven

```bash
# Local repository (~/.m2): run this before an external project references the starter; it publishes protos / sdk-java / starter together
gradle publishNotifyHubToMavenLocal

# Remote repository: URL and credentials are read only from env vars or -P, never written into the repo
MAVEN_URL=https://your-repo/releases MAVEN_USER=... MAVEN_PASSWORD=... gradle publishNotifyHub
# Equivalent: gradle publishNotifyHub -PmavenUrl=... -PmavenUser=... -PmavenPassword=...
```

For anonymous repositories (e.g. a local `file:///...` directory) do **not** set `MAVEN_USER`,
otherwise Gradle fails with "authentication is not supported by the protocol".

When signing is required (Maven Central does), set `MAVEN_SIGNING_KEY` (ASCII-armored private key);
`MAVEN_SIGNING_PASSWORD` is only needed if the key is password-protected. If neither is set,
the signing plugin is not enabled at all:

```bash
MAVEN_SIGNING_KEY="$(cat private-key.asc)" \
MAVEN_URL=https://... gradle publishNotifyHub
# produces .asc signatures for every jar/pom/module
```

POM license / scm / developer metadata is already set in `gradle.properties` (Maven Central
requires all of them), so you can publish as-is; override any of them with `-P` when needed:

```bash
gradle publishNotifyHub -PpomDeveloperEmail=you@example.com
```

Maven coordinates use `io.github.huangwenfu750` (the GitHub user namespace, which Central verifies
automatically without a domain): `io.github.huangwenfu750:protos`,
`io.github.huangwenfu750:sdk-java`, `io.github.huangwenfu750:notifyhub-spring-boot-starter`.

Cross-process smoke tests (the four language SDKs against a real server):

```bash
./server/build/install/server/bin/server --config smoke/config.yaml &
.venv/Scripts/python.exe smoke/py_smoke.py     # Python SDK
node smoke/node_smoke.js                       # Node SDK
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token # Java SDK
cd smoke/go && go run .                        # Go SDK (run ./scripts/gen-protos.sh go first)
```

Throughput baseline (laptop CPU, Python client benchmark, client is the bottleneck): ~5.7k msg/s, p99=3.8ms;
server-side gRPC capacity is far higher (re-measure with `ghz`):

```bash
ghz --insecure -n 100000 -c 64 --call notify.v1.Notify/Publish \
    -d '{"topic":"bench","title":"t","content":"c"}' -H "x-api-token: ntf_xxx" 127.0.0.1:9987
```

## Project Layout

```
proto/notify/v1/notify.proto    language-agnostic contract (core asset)
protos/                         generated Java stubs (shared by server and the Java SDK)
server/                         Java 21 server (gRPC, routing, channel adapters, delivery)
sdks/java|python|typescript|go  four thin SDKs
sdks/spring-boot                Spring Boot starter (auto-configuration + template + health check)
examples/spring-boot            standalone Spring Boot sample project (bootRun verifies the whole chain)
examples/                       minimal per-language samples
smoke/                          cross-process end-to-end smoke tests
scripts/                        protoc generation, benchmark scripts
docs/                           protocol, configuration, rewrite guide
```

## Roadmap (V2)

- Durable delivery and at-least-once semantics (event log + replay)
- Web console (channel management, online testing, delivery history)
- More channels: email SMTP, Slack / Telegram / Discord (channel SPI is ready, see `server/src/main/java/io/notifyhub/channel/`)
- gRPC-Web gateway for direct browser access

## License

[MIT](LICENSE)

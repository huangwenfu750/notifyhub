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

## Quick Start

### 1. Start the server

```bash
# Generate Java stubs and build (protoc is downloaded automatically on first run)
./scripts/gen-protos.sh java
gradle :server:installDist

# Copy and edit the config
cp config.example.yaml config.yaml   # fill in your bot webhook/secret and auth.tokens

./server/build/install/server/bin/server --config config.yaml
# [main] INFO io.notifyhub.Server - NotifyHub 0.1.1 已启动，监听 0.0.0.0:9987
```

Docker (two images, pick either):

```bash
# 1) Runtime image (recommended): drops the prebuilt distribution into a JRE base image, no compile inside the container
./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.1-linux-x86_64-nojre.tar.gz packaging/docker/
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
./scripts/package-linux.sh   # produces build/linux/notifyhub-0.1.1-linux-x86_64.tar.gz

# On the target machine
tar -xzf notifyhub-0.1.1-linux-x86_64.tar.gz
sudo ./notifyhub-0.1.1-linux-x86_64/install.sh   # installs to /opt/notifyhub and registers systemd
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
// Go (sdks/go)
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
| Go | `go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1` | ✅ published (distributed via `sdks/go/v*` tags) |
| Java / Kotlin | `implementation("io.github.huangwenfu750:sdk-java:0.1.1")` | ✅ on Maven Central (since 0.1.1) |
| Spring Boot | `implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")` | ✅ on Maven Central (since 0.1.1) |
| TypeScript / JS | `npm i notifyhub-client` | ✅ published (npm 0.1.1) |
| Python | `pip install notifyhub-client` | ✅ published (PyPI 0.1.1) |

> If `pip install` reports `No matching distribution found` while the version does exist on PyPI,
> your mirror has not synced yet (`mirrors.aliyun.com` is measurably behind for new packages).
> Point pip at the official index to work around it:
> `pip install -i https://pypi.org/simple notifyhub-client`

The Java artifacts also go to **Maven Central starting with 0.1.1** — plain `mavenCentral()` is
enough, no token needed:

```kotlin
repositories { mavenCentral() }
dependencies {
    implementation("io.github.huangwenfu750:sdk-java:0.1.1")
}
```

`protos` / `sdk-java` / `notifyhub-spring-boot-starter` all live under `io.github.huangwenfu750`,
each shipping jar / sources / javadoc / pom / module with an `.asc` signature.

> 0.1.0 went to GitHub Packages only and needs a PAT to fetch; upgrading to 0.1.1 removes all of
> that. Keep the setup below only if you want GitHub Packages (e.g. CI snapshots) — that registry
> requires a token even for reads (anonymous requests get 401).

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/huangwenfu750/notifyhub")
        credentials {                                  // prefer env vars, do not hardcode
            username = System.getenv("GITHUB_ACTOR")   // GitHub user name
            password = System.getenv("GITHUB_TOKEN")   // PAT with read:packages
        }
    }
}
```

Gradle (Groovy):

```groovy
repositories {
    mavenCentral()
    maven {
        url = 'https://maven.pkg.github.com/huangwenfu750/notifyhub'
        credentials {
            username = System.getenv('GITHUB_ACTOR')
            password = System.getenv('GITHUB_TOKEN')
        }
    }
}
```

Maven (`~/.m2/settings.xml`; `server.id` must match `repository.id`):

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>your-github-user</username>
      <password>your-pat-with-read-packages</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/huangwenfu750/notifyhub</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>
</settings>
```

> Create the PAT under GitHub → Settings → Developer settings → Personal access tokens with
> `read:packages`; public repos need it for reads too. To skip tokens entirely, publish locally with
> `gradle publishNotifyHubToMavenLocal` and add `mavenLocal()` on the consumer side.

Server distributions are on the [Releases](https://github.com/huangwenfu750/notifyhub/releases) page:
`notifyhub-<ver>-linux-x86_64.tar.gz` (bundles JRE 21, unpack and run) and the `-nojre` slim
variant, each with a `.sha256`.

## License

[MIT](LICENSE)

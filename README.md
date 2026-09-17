# NotifyHub

[![CI](https://github.com/huangwenfu750/notifyhub/actions/workflows/ci.yml/badge.svg)](https://github.com/huangwenfu750/notifyhub/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/huangwenfu750/notifyhub)](https://github.com/huangwenfu750/notifyhub/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> English: [README.en.md](README.en.md)

**多语言通知推送服务**：单进程部署，任意语言客户端通过 gRPC 接入；
把通知推送到钉钉 / 企业微信 / 飞书 / 任意 Webhook，同时支持基于主题通配符的事件订阅。
推送平台可通过 **YAML 配置文件**或**代码**（Admin RPC）配置。

```text
                        ┌─────────────────────────────────────────┐
 Java ┐                 │              NotifyHub Server           │
 Go   │   gRPC          │  Publish ── 路由(topic通配) ─→ 平台投递  │──→ 钉钉机器人
 Python ├──────────────→│  Subscribe ←─ 订阅注册表 ── 广播事件     │──→ 企业微信机器人
 JS/TS │  (x-api-token) │  Admin    ── 运行时配置平台              │──→ 飞书机器人
 ...  ┘                 │  队列+重试+限流+去重+死信                │──→ 通用 Webhook
                        └─────────────────────────────────────────┘
```

## 特性

- **多语言**：协议为 proto3 + gRPC；官方 SDK：Java、Python、TypeScript/JS、Go（见 `sdks/`）
- **Spring Boot 开箱即用**：`notifyhub-spring-boot-starter`，`application.yml` 里配 host/port/token，注入 `NotifyHubTemplate` 即用
- **发布 / 订阅**：`Publish` 推送到平台并广播，`Subscribe` 服务端流式接收（通配符 `*`/`#`、至多一次）
- **推送平台即插即配**：YAML 配置文件、Admin RPC 代码配置、token 鉴权
- **生产化投递**：每平台令牌桶限流、指数退避重试、dedup_key 防告警风暴、死信主题 `deadletter`

## 快速开始

### 1. 启动服务端

```bash
# 生成 Java stub 并构建（首次会自动下载 protoc）
./scripts/gen-protos.sh java
gradle :server:installDist

# 复制并修改配置
cp config.example.yaml config.yaml   # 填入你的机器人 webhook/secret 与 auth.tokens

./server/build/install/server/bin/server --config config.yaml
# [main] INFO io.notifyhub.Server - NotifyHub 0.1.1 已启动，监听 0.0.0.0:9987
```

Docker（两种镜像，任选其一）：

```bash
# 1) 运行时镜像（推荐）：把已构建的发行包塞进 JRE 基础镜像，不在容器里编译
./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.1-linux-x86_64-nojre.tar.gz packaging/docker/
cp config.example.yaml packaging/docker/config.yaml   # 填 auth.tokens / platforms
cd packaging/docker && docker compose up -d --build

# 2) 源码镜像：容器内跑 Gradle 构建（适合 CI，机器资源要求更高）
cp config.example.yaml config.yaml                    # 填 auth.tokens / platforms
docker compose up --build -d
```

两种都把 9987 暴露到宿主机、配置以只读方式挂到 `/etc/notifyhub/config.yaml`，
并带 TCP 健康检查；细节见 [packaging/docker/README.md](packaging/docker/README.md)。

Linux 发行包（自带 JRE 21，解压即用，无需预装 Java）：

```bash
./scripts/package-linux.sh   # 产出 build/linux/notifyhub-0.1.1-linux-x86_64.tar.gz

# 目标机
tar -xzf notifyhub-0.1.1-linux-x86_64.tar.gz
sudo ./notifyhub-0.1.1-linux-x86_64/install.sh   # 安装到 /opt/notifyhub 并注册 systemd
sudo systemctl start notifyhub
```

也可以不装系统服务，直接 `./bin/notifyhub start|stop|status|--config etc/my.yaml`。
详见 [packaging/linux/share/doc/README.md](packaging/linux/share/doc/README.md)。

### 2. 发布通知（任选语言）

```java
// Java（sdks/java）
try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987).token("ntf_xxx").build()) {
    client.publish("alert", "部署完成", "v1.2.0 上线");
}
```
```python
# Python（sdks/python，pip install -e sdks/python）
with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "部署完成", "v1.2.0 上线")
```
```ts
// TypeScript/JS（sdks/typescript）
const client = new NotifyClient("localhost:9987", "ntf_xxx");
await client.publish("alert", "部署完成", "v1.2.0 上线");
```
```go
// Go（sdks/go）
client, _ := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
client.Publish(ctx, "alert", "部署完成", "v1.2.0 上线")
```
```java
// Spring Boot（sdks/spring-boot）：application.yml 配 host/port/token，直接注入
notify.publish("alert", "部署完成", "v1.2.0 上线");
```

### 3. 订阅主题

```python
sub = client.subscribe(["alert.*"], print)   # 事件实时到达；sub.cancel() 退订
```

### 4. 用代码配置推送平台

```python
client.upsert_platform(
    name="ding-alert", type="dingtalk",
    webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
    secret="SECxxx", topics=["alert"],
)
```

## 配置

见 [docs/config.md](docs/config.md) 与 [config.example.yaml](config.example.yaml)。

协议与投递语义（含各渠道签名算法）见 [docs/protocol.md](docs/protocol.md)。

逐步上手手册见 [docs/usage.md](docs/usage.md)（Spring Boot 用户看第 5.5 节）。

## 安装 SDK（包管理平台）

| 语言 | 安装方式 | 状态 |
|---|---|---|
| Go | `go get github.com/huangwenfu750/notifyhub/sdks/go@v0.1.1` | ✅ 已发布（靠 `sdks/go/v*` 标签分发） |
| Java / Kotlin | `implementation("io.github.huangwenfu750:sdk-java:0.1.1")` | ✅ 已在 Maven Central（0.1.1 起） |
| Spring Boot | `implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.1")` | ✅ 已在 Maven Central（0.1.1 起） |
| TypeScript / JS | `npm i notifyhub-client` | ✅ 已发布（npm 0.1.1） |
| Python | `pip install notifyhub-client` | ✅ 已发布（PyPI 0.1.1） |

> 若 `pip install` 报 `No matching distribution found`，而 PyPI 上确实已有该版本，基本是国内镜像还没同步
> （实测 `mirrors.aliyun.com` 对新包会滞后）。指定官方源即可绕开：
> `pip install -i https://pypi.org/simple notifyhub-client`

Java 侧从 **0.1.1 起同时发到 Maven Central**，只要 `mavenCentral()` 就能取到，不需要任何 token：

```kotlin
repositories { mavenCentral() }
dependencies {
    implementation("io.github.huangwenfu750:sdk-java:0.1.1")
}
```

`protos` / `sdk-java` / `notifyhub-spring-boot-starter` 三个坐标都在
`io.github.huangwenfu750` 下，jar / sources / javadoc / pom / module 五种产物各带 `.asc` 签名。

> 0.1.0 只发在 GitHub Packages，需要 PAT 才能取；升级到 0.1.1 就不用管这些了。
> 仍想用 GitHub Packages（比如取 CI 上的快照）按下面配置 —— 该 registry 读写都要 token，匿名请求直接 401。

Gradle（Kotlin DSL）：

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/huangwenfu750/notifyhub")
        credentials {                                  // 也可以用环境变量，别硬编码
            username = System.getenv("GITHUB_ACTOR")   // GitHub 用户名
            password = System.getenv("GITHUB_TOKEN")   // PAT，需 read:packages
        }
    }
}
```

Gradle（Groovy）同理：

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

Maven（`~/.m2/settings.xml`，`server.id` 必须与 `repository.id` 一致）：

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>你的 GitHub 用户名</username>
      <password>你的 PAT（read:packages）</password>
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

> PAT 在 GitHub → Settings → Developer settings → Personal access tokens 里生成，勾 `read:packages` 即可；
> 仓库 public 也不例外，读包同样要带上。不想折腾 token 就走本地仓库：`gradle publishNotifyHubToMavenLocal`，
> 然后在使用方加 `mavenLocal()`。

服务端发行版在 [Releases](https://github.com/huangwenfu750/notifyhub/releases)：
`notifyhub-<ver>-linux-x86_64.tar.gz`（自带 JRE 21，解压即用）与 `-nojre` 精简版，各带 `.sha256`。

## 许可证

[MIT](LICENSE)

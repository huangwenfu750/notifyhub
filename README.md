# NotifyHub

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
- **为重写而设计**：Java 版是参考实现，proto 契约 + 语言无关测试资产保证将来平滑重写为 Go/Rust/Zig（见 docs/rewrite.md）

## 快速开始

### 1. 启动服务端

```bash
# 生成 Java stub 并构建（首次会自动下载 protoc）
./scripts/gen-protos.sh java
gradle :server:installDist

# 复制并修改配置
cp config.example.yaml config.yaml   # 填入你的机器人 webhook/secret 与 auth.tokens

./server/build/install/server/bin/server --config config.yaml
# [main] INFO io.notifyhub.Server - NotifyHub 0.1.0 已启动，监听 0.0.0.0:9987
```

Docker（两种镜像，任选其一）：

```bash
# 1) 运行时镜像（推荐）：把已构建的发行包塞进 JRE 基础镜像，不在容器里编译
./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.0-linux-x86_64-nojre.tar.gz packaging/docker/
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
./scripts/package-linux.sh   # 产出 build/linux/notifyhub-0.1.0-linux-x86_64.tar.gz

# 目标机
tar -xzf notifyhub-0.1.0-linux-x86_64.tar.gz
sudo ./notifyhub-0.1.0-linux-x86_64/install.sh   # 安装到 /opt/notifyhub 并注册 systemd
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
// Go（sdks/go，先 ./scripts/gen-protos.sh go）
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

## 构建与测试

```bash
./scripts/gen-protos.sh java          # 生成 Java stub（tools/ 目录自动下载 protoc）
gradle test                           # 全部单元 + E2E 测试（WireMock 模拟平台 HTTP）
gradle :server:installDist            # 发行版: server/build/install/server/
```

CI（`.github/workflows/ci.yml`）跑的就是上面这套 + 四语言跨进程 smoke。

## 发布到 Maven

```bash
# 本地仓库（~/.m2）：外部工程引用 starter 前先跑它，会连发 protos / sdk-java / starter
gradle publishNotifyHubToMavenLocal

# 远端仓库：地址与凭据只从环境变量或 -P 读取，不写进仓库
MAVEN_URL=https://your-repo/releases MAVEN_USER=... MAVEN_PASSWORD=... gradle publishNotifyHub
# 等价写法：gradle publishNotifyHub -PmavenUrl=... -PmavenUser=... -PmavenPassword=...
```

匿名仓库（如本地 `file:///...` 目录）不要设 `MAVEN_USER`，否则 Gradle 会报「协议不支持认证」。

需要签名时（Maven Central 要求）设置 `MAVEN_SIGNING_KEY`（ASCII-armored 私钥）即可，
`MAVEN_SIGNING_PASSWORD` 仅在私钥带密码时需要；不设则完全不启用签名插件：

```bash
MAVEN_SIGNING_KEY="$(cat private-key.asc)" \
MAVEN_URL=https://... gradle publishNotifyHub
# 产出每个 jar/pom/module 的 .asc 签名
```

POM 的 scm / developer 元信息仍可用 `-P` 补齐（不传则省略；license 已随 `LICENSE` 定为 MIT 并写进 `gradle.properties`）：

```bash
gradle publishNotifyHub \
  -PpomLicenseName=MIT -PpomLicenseUrl=https://opensource.org/licenses/MIT \
  -PpomScmUrl=https://github.com/huangwenfu750/notifyhub.git \
  -PpomDeveloperId=notifyhub -PpomDeveloperName="NotifyHub" -PpomDeveloperEmail=dev@notifyhub.io
```

跨进程烟雾测试（Java/Python/Node/Go 四语言 SDK 对真实服务端）：

```bash
./server/build/install/server/bin/server --config smoke/config.yaml &
.venv/Scripts/python.exe smoke/py_smoke.py     # Python SDK
node smoke/node_smoke.js                       # Node SDK
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token # Java SDK
cd smoke/go && go run .                        # Go SDK（先 ./scripts/gen-protos.sh go）
```

吞吐基线（笔记本 CPU，Python 客户端压测，瓶颈在客户端）：约 5.7k msg/s，p99=3.8ms；服务端 gRPC 容量远高于此（可用 `ghz` 复测）：

```bash
ghz --insecure -n 100000 -c 64 --call notify.v1.Notify/Publish \
    -d '{"topic":"bench","title":"t","content":"c"}' -H "x-api-token: ntf_xxx" 127.0.0.1:9987
```

## 项目结构

```
proto/notify/v1/notify.proto   语言无关协议契约（核心资产）
protos/                        Java 生成 stub（server 与 java sdk 共用）
server/                        Java 21 服务端（gRPC、路由、渠道适配、投递）
sdks/java|python|typescript|go 四语言 SDK（薄封装）
sdks/spring-boot              Spring Boot starter（自动配置 + 模板 + 健康检查）
examples/spring-boot          独立 Spring Boot 示例工程（可 bootRun 验证全链路）
examples/                      各语言最小示例
smoke/                         跨进程端到端烟雾测试
scripts/                       protoc 生成、压测脚本
docs/                          协议、配置、重写指南
```

## Roadmap（V2）

- 投递持久化与至少一次送达（事件日志 + 重放）
- Web 控制台（渠道管理、在线测试、发送记录）
- 更多渠道：邮件 SMTP、Slack / Telegram / Discord（channel SPI 已就绪，见 `server/src/main/java/io/notifyhub/channel/`）
- gRPC-Web 网关支持浏览器直连

## 许可证

[MIT](LICENSE)

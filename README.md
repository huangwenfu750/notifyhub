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

## 更新日志

版本间的差异记在 [CHANGELOG.md](CHANGELOG.md)（English: [CHANGELOG.en.md](CHANGELOG.en.md)），
发版时对应段落会自动成为 Release 说明。

## 发版流程（维护者）

0. 改版本号 —— 散在 10 处（四个 Gradle 模块、打包脚本、Python 的 `pyproject.toml` 与
   `__init__.py`、服务端常量、TypeScript 包、示例工程依赖），一次改全：

   ```bash
   python scripts/check-versions.py 0.2.0 --set   # 不带 --set 则只检查是否一致
   ```

   文档里提到版本号的地方脚本只列出来、不自动改（可能是「自某版本起」这类历史表述），需人工确认。

1. `git tag v0.2.0 && git push origin v0.2.0`
   → `release.yml` 在 CI 构建 Linux 发行包并创建 Release、附上产物
2. 同时给 Go 子模块打标签（前缀必须与模块路径一致，缺了 `go get` 就取不到）：
   `git tag sdks/go/v0.2.0 && git push origin sdks/go/v0.2.0`
3. Release 建好后 `publish.yml` 自动推送各语言包。PyPI 已通；npm 还差一步 ——
   在仓库 Settings → Secrets 里加 `NPM_TOKEN`，然后重跑该工作流即可。
   token 必须带 publish 权限（npmjs.com 的 Classic / Automation Token 最省事）；
   granular token 只给读权限会报 `npm error 403 ... may not perform that action`。

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

POM 的 license / scm / developer 元信息已写进 `gradle.properties`（Maven Central 要求这几项齐全），
直接发布即可；需要覆盖时用 `-P` 传入同名属性：

```bash
gradle publishNotifyHub -PpomDeveloperEmail=you@example.com
```

Maven 坐标是 `io.github.huangwenfu750`（GitHub 用户命名空间，Central 免域名验证）：
`io.github.huangwenfu750:protos`、`io.github.huangwenfu750:sdk-java`、
`io.github.huangwenfu750:notifyhub-spring-boot-starter`。

### 发到 Maven Central

发上去之后使用方就不必配 GitHub Packages 了。前置（每个账号只做一次）：

1. 用 GitHub 账号登录 [Central Portal](https://central.sonatype.com)，注册命名空间
   `io.github.huangwenfu750`（GitHub 命名空间按页面提示建一个指定名称的临时 public 仓库即完成校验）。
2. 生成 Portal 的 User Token（用户名 / 密码两段），只用于上传。
3. 准备 GPG 密钥，把**公钥**传到 keyserver —— Central 从那里取公钥校验 `.asc`。

#### GPG 密钥：三步

```bash
# 1) 生成。sign-only、永不过期，且只有主密钥、不带子密钥 ——
#    Maven / Nexus 只会用主密钥验签；带子密钥时 gpg 会改用子密钥签，Central 校验必失败
gpg --batch --passphrase '<口令>' --quick-generate-key \
    "NotifyHub <huangwenfu750@users.noreply.github.com>" rsa4096 sign never

# 2) 记下 <KEYID>（sec 行 rsa4096/ 后面那串），把公钥传上去
#    注意：<KEYID> 是占位符，尖括号不要一起敲，只留那 16 位十六进制
gpg --list-secret-keys --keyid-format=long
gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
gpg --keyserver keys.openpgp.org     --send-keys <KEYID>   # 可选，多一路更快同步

# 3) 导出私钥：整段（含 BEGIN / END 两行）填进 Secret MAVEN_SIGNING_KEY
gpg --batch --yes --pinentry-mode loopback --passphrase '<口令>' \
    --armor --export-secret-keys '<KEYID>!' > private-key.asc
```

要点：

- `<KEYID>!` 的感叹号表示只导出这一个密钥，不带子密钥。
- `MAVEN_SIGNING_PASSWORD` 填第 1 步的口令；私钥没设口令就留空（不推荐）。
- 密钥到期后续了期，要**再 send-keys 一次**，否则 Central 拿到的还是旧公钥。
- `send-keys` 卡住或超时（公司网络常封 hkp 的 11371 端口）时，换成
  `gpg --keyserver hkps://keys.openpgp.org --send-keys <KEYID>`，或到
  <https://keyserver.ubuntu.com> 网页粘贴 `gpg --armor --export <KEYID>` 的输出。
- `private-key.asc` 千万别提交：仓库 `.gitignore` 已排除 `*.asc`。

#### 需要的 Secrets

| Secret | 填什么 | 用途 |
|---|---|---|
| `NPM_TOKEN` | npmjs.com 的 token | 发 npm 包 |
| `PYPI_API_TOKEN` | PyPI 的 API token | 发 PyPI 包 |
| `GO_TAG_TOKEN` | PAT（Contents: Read and write） | 建 `sdks/go/v*` 标签；`GITHUB_TOKEN` 打 git refs 会被 403 |
| `MAVEN_SIGNING_KEY` | `private-key.asc` 全文 | GPG 签名（Central 强制） |
| `MAVEN_SIGNING_PASSWORD` | 私钥口令（无口令则留空） | GPG 签名 |
| `CENTRAL_USERNAME` / `CENTRAL_PASSWORD` | Portal User Token 的两段 | 上传 bundle 到 Central |

`GITHUB_TOKEN` 是内置的，不用自己配。

打包（Portal 要求一次提交完整的 deployment，所以先落到本地目录再整体上传）：

```bash
MAVEN_SIGNING_KEY="$(gpg --armor --export-secret-keys you@example.com)" \
MAVEN_SIGNING_PASSWORD=... \
gradle publishNotifyHubToCentralBundle
# → build/central/notifyhub-<ver>-central-bundle.zip
```

每个模块在包里是 `jar` / `-sources.jar` / `-javadoc.jar` / `pom` / `module` 各一份并带 `.asc`
（不设 `MAVEN_SIGNING_KEY` 就出无签名版本，本地自测够用，Central 会拒收）。

上传走 API
`POST https://central.sonatype.com/api/v1/publisher/upload`（注意是 **v1**，`v3` 不存在，
POST 过去只会得到无响应体的 500）。认证不是 Basic，而是
`Authorization: Bearer $(printf '%s:%s' "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 -w 0)`；
返回 201 且响应体是 deploymentId。
**已发布过的版本号不能覆盖**，重发前先 bump 版本。

打 `v*` 标签后 `publish.yml` 会构建同一个 zip（也挂在 workflow artifact `maven-central-bundle` 上，
保留 30 天）。配了 Secrets `CENTRAL_USERNAME` / `CENTRAL_PASSWORD`（Portal User Token）时，
CI 会一路走完 **上传 → 等校验（VALIDATED）→ 自动 Publish → 等到 PUBLISHED**，
不需要再进 Portal 点按钮；任何一步失败都会让 job 变红并打印 status 详情。
没配这两个 Secret 就只留 artifact、跳过上传。

Publisher API 的三个坑：

1. 路径是 `v1`，不是 `v3`（`v3` 不存在）。
2. 认证是 `Authorization: Bearer <base64(用户名:口令)>`，不是 HTTP Basic（`curl -u`）。
3. `/status` 是 **POST** 不是 GET —— 用 GET 会拿到 401 `Invalid token`，看着像 token 失效，实际是方法不对。
   状态流转：`PENDING → VALIDATING → VALIDATED →（POST /deployment/<id>）→ PUBLISHING → PUBLISHED`，
   `VALIDATED` 只是「校验通过等你发布」，此时 repo1.maven.org 上查不到任何东西。

跨进程烟雾测试（Java/Python/Node/Go 四语言 SDK 对真实服务端）：

```bash
./server/build/install/server/bin/server --config smoke/config.yaml &
.venv/Scripts/python.exe smoke/py_smoke.py     # Python SDK
node smoke/node_smoke.js                       # Node SDK
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token # Java SDK
cd smoke/go && go run .                        # Go SDK
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

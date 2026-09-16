# NotifyHub 详细使用说明

> English: [usage.en.md](usage.en.md)

> 面向第一次上手的人：从"启动服务端"到"四种语言发通知、收通知、动态配置平台"，以及排错速查。
> Spring Boot 用户可直接看 [第 5.5 节](#55-spring-boot自动配置)。
> 配套文档：[`config.md`](config.md)（配置项手册）、[`protocol.md`](protocol.md)（gRPC 契约与投递语义）、[`../README.md`](../README.md)（总览）。

---

## 0. 心智模型（30 秒）

NotifyHub 是一个**单进程 gRPC 服务**，干两件事：

1. **把通知转发出去**：你 `Publish(topic, title, content)` → 服务端按 topic 通配路由到已配置的推送平台（钉钉/企微/飞书/任意 Webhook）→ 异步投递（限流 + 重试 + 去重 + 死信）。
2. **订阅主题**：你 `Subscribe(["alert.*"])` → 任何语言发布的事件实时推给你（至多一次，不持久化）。

```
你的服务 ──gRPC(x-api-token)──▶ NotifyHub ──HTTP──▶ 钉钉 / 企微 / 飞书 / Webhook
                                   │
                                   └──stream──▶ 其他语言的订阅者
```

客户端不需要 HTTP、不需要签名、不需要知道机器人地址——这些都在服务端配置里。

---

## 1. 启动服务端

### 1.1 构建（Java 21 + Gradle）

仓库没有自带 gradle wrapper，使用系统安装的 `gradle`：

```bash
./scripts/gen-protos.sh java     # 首次：生成 Java gRPC stub（自动下载 protoc 到 tools/）
gradle :server:installDist       # 产出发行版到 server/build/install/server/
```

Windows 下：

```bat
bash scripts\gen-protos.sh java
gradle :server:installDist
```

### 1.2 准备配置

```bash
cp config.example.yaml config.yaml   # 然后编辑：填真实 webhook/secret 与 auth.tokens
```

### 1.3 运行

```bash
# 方式 A：显式指定
./server/build/install/server/bin/server --config config.yaml

# 方式 B：环境变量（Docker 镜像内即此方式）
export NOTIFYHUB_CONFIG=/etc/notifyhub/config.yaml
./server/build/install/server/bin/server

# 方式 C：不带任何参数 → 读工作目录下的 config.yaml
cd server/build/install/server && ./bin/server
```

Windows：

```bat
server\build\install\server\bin\server.bat --config <仓库路径>\config.yaml
```

配置路径优先级：`--config` / `-c` 参数 > 环境变量 `NOTIFYHUB_CONFIG` > 工作目录 `config.yaml`。
查看版本：`server --version`（不启动服务）。

启动成功日志：

```
[main] INFO io.notifyhub.Server - NotifyHub 0.1.0 已启动，监听 0.0.0.0:9987 (tokens=1, platforms=3, workers=16)
```

> 端口占用、平台类型写错、YAML 语法错误都会在启动时**立即报错退出**，不会带病运行。

### 1.4 Docker

```bash
docker compose up --build -d      # 挂载 ./config.example.yaml 到 /etc/notifyhub/config.yaml
docker logs -f notifyhub
docker compose down
```

把 `docker-compose.yml` 的挂载源改成你自己的 `config.yaml` 即可。镜像内置了 TCP 9987 健康检查。

---

## 2. 最小可跑配置（本地验证用）

不想先申请机器人？用 `webhook` 类型打到本地回显服务，先跑通链路：

```yaml
# config.yaml —— 最小验证版（无鉴权 + 本地 webhook）
server:
  host: 127.0.0.1
  port: 9987

auth:
  tokens: []            # 空数组 = 不鉴权，仅本机调试用

platforms:
  - name: local-hook
    type: webhook
    url: http://127.0.0.1:19800/hook
    topics: ["*"]
```

再起一个回显服务（任选其一）：

```bash
python -c "from http.server import BaseHTTPRequestHandler,HTTPServer; \
HTTPServer(('127.0.0.1',19800), type('H',(BaseHTTPRequestHandler,),{'do_POST':lambda s:(print(s.rfile.read(int(s.headers['Content-Length']))), s.send_response(200), s.end_headers())})).serve_forever()"
```

生产配置请把 `auth.tokens` 填上（见下节）。

---

## 3. 配置详解

完整示例见 [`config.example.yaml`](../config.example.yaml)，逐项说明见 [`config.md`](config.md)。这里只强调容易踩的点。

### 3.1 server / auth

| 键 | 默认 | 说明 |
|---|---|---|
| `server.host` | `0.0.0.0` | 监听地址；本机调试建议 `127.0.0.1` |
| `server.port` | `9987` | gRPC 端口 |
| `server.workers` | CPU 核数（最小 4） | 投递 worker 线程数 |
| `server.queue_capacity` | `10000` | 异步投递队列容量；**打满时 `Publish` 返回 `UNAVAILABLE`** |
| `auth.tokens` | `[]` | token 白名单；**为空则不鉴权**（`Ping` 永远免鉴权） |

### 3.2 defaults（全局默认，平台可覆盖）

```yaml
defaults:
  retry: { max_attempts: 3, backoff_ms: 500 }   # 指数退避 + 抖动，单次上限 30s
  rate_limit: { qps: 15 }                        # 每平台独立令牌桶（钉钉官方限 20 qps）
  dedup_window_ms: 60000                         # dedup_key 去重窗口
```

### 3.3 platforms[]（推送平台）

| 键 | 必填 | 说明 |
|---|---|---|
| `name` | ✅ | 全局唯一，作为 `platforms` 显式指定和 Admin 删除的标识 |
| `type` | ✅ | `dingtalk` / `wecom` / `feishu` / `webhook` |
| `webhook` 或 `url` | ✅ | 两键等价，必须 `http(s)://` 开头 |
| `secret` | — | 钉钉/飞书加签密钥；webhook 渠道为 HMAC 签名密钥 |
| `topics` | — | 路由规则数组，默认 `["*"]` |
| `template` | — | 占位符模板，默认 `title\ncontent` |
| `at_mobiles` | — | 钉钉 @ 手机号 |
| `sign_header` | — | 仅 webhook，默认 `X-Signature` |
| `retry` / `rate_limit_qps` | — | 覆盖全局默认 |

四种渠道的加签与成功判定：

| 渠道 | 加签方式 | 消息格式 | 成功判定 |
|---|---|---|---|
| `dingtalk` | `urlencode(base64(HmacSHA256(secret, ts+"\n"+secret)))` 拼入 URL | markdown，支持 `at_mobiles` | `errcode==0` |
| `wecom` | 无（key 在 URL 里） | markdown，正文约 2000 字符上限 | `errcode==0` |
| `feishu` | `base64(HmacSHA256(key=ts+"\n"+secret, data=""))` 放 body | text | `code==0` |
| `webhook` | 可选 `hex(HmacSHA256(secret, body))` 放入签名头 | JSON：`{event_id, topic, title, content, params, timestamp}` | HTTP 2xx |

---

## 4. 核心概念

### 4.1 topic 与通配路由

以 `.` 分段，AMQP 风格：

| 规则 | 含义 | 示例 |
|---|---|---|
| `alert` | 精确匹配 | 只匹配 `alert` |
| `alert.*` | `*` 恰好一段 | 匹配 `alert.db`，不匹配 `alert`、`alert.db.slow` |
| `ops.#` | `#` 零或多段，**只能作末段** | 匹配 `ops`、`ops.a`、`ops.a.b` |
| `*` | 一段 | 兜底常用 `"*"` |

一条发布可以命中多个平台（按配置文件声明顺序全部入队）。

### 4.2 模板占位符

```yaml
template: "**{{title}}**\n{{content}}\n> topic={{topic}} host={{params.host}}"
```

支持：`{{title}}`、`{{content}}`、`{{topic}}`、`{{event_id}}`、`{{params.xxx}}`，也可以直接写 `{{xxx}}`（等价于 `params.xxx`）。未知占位符替换为空字符串。模板为空时各渠道用自己的默认格式。

### 4.3 去重（防告警风暴）

发布时带 `dedup_key`：

```python
client.publish("alert.db", "磁盘告警", "...", dedup_key="db-01-disk")
```

窗口期（默认 60s，由 `defaults.dedup_window_ms` 控制）内相同 key 的**后续**发布：
返回 `accepted=false, deduplicated=true`，**既不推平台也不广播订阅者**。
典型用法：`dedup_key = f"{告警名}:{实例}"`，让同一实例的同种告警 60s 内只响一次。

### 4.4 投递语义

1. `Publish` 只做「路由 + 去重 + 入队」，**同步返回**；真正的 HTTP 投递是异步的。
2. 每平台独立令牌桶限流（默认 15 qps，钉钉建议 ≤20）。
3. 失败按 `backoff_ms × 2^n + 抖动` 指数退避重试，单次退避上限 30s。
4. 重试耗尽 → 写 ERROR 死信日志，并广播到内置主题 **`deadletter`**。订阅它就能实时监控投递失败：

```python
client.subscribe(["deadletter"], lambda e: print("投递失败:", e.params))
# params 含 platform / topic / event_id / error
```

### 4.5 订阅语义（重要）

- **至多一次**：不持久化、不重放、不补发。
- 订阅者处理太慢 → 事件被丢弃（服务端每订阅一个队列，慢消费者不阻塞别人）。
- 客户端断开即自动退订；也可用句柄主动 `cancel()` / `close()`。
- 需要"不丢消息"的场景：请让订阅端把事件落到自己的存储里，或等 V2 的持久化投递。

---

## 5. 四种语言 SDK

> 约定：服务端 `localhost:9987`，token `ntf_xxx`。
> 所有 SDK 都是**薄封装**：token 通过 gRPC metadata `x-api-token` 注入，底层就是 proto 里的那 6 个方法。

### 5.1 Python

```bash
pip install -e sdks/python
```

```python
from notifyhub import NotifyClient, PublishRequest   # PublishRequest 用于批量发布

with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    # 1) 健康检查（免鉴权）
    print(client.ping())              # {'version': '0.1.0', 'uptime_seconds': '12'}

    # 2) 发布
    ack = client.publish(
        "alert.db", "磁盘告警", "db-01 使用率 95%",
        params={"host": "db-01"},     # 模板变量
        # platforms=["dingtalk-alert"],  # 显式指定目标，覆盖 topic 路由
        # dedup_key="db-01-disk",        # 去重
        # skip_subscribers=False, skip_platforms=False,
        # timeout=10.0,
    )
    print(ack.accepted, ack.event_id, ack.matched_platforms, ack.deduplicated, ack.error)

    # 3) 订阅（回调在后台线程触发）
    sub = client.subscribe(["alert.*"], lambda e: print("收到:", e.topic, e.title))
    ...
    sub.cancel()                      # 或 sub.close()

    # 4) 批量发布（双向流，单条失败不中断）
    from notifyhub import PublishRequest
    reqs = [PublishRequest(topic="alert.db", title="t1"),
            PublishRequest(title="缺 topic 的非法请求")]
    for a in client.publish_batch(reqs):
        print(a.accepted, a.error)

    # 5) Admin
    client.upsert_platform(name="ding-alert", type="dingtalk",
                           webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
                           secret="SECxxx", topics=["alert"], template="**{{title}}**\n{{content}}")
    print(client.list_platforms())
    client.remove_platform("ding-alert")
```

异常：`NotifyError`（含 `.code` = gRPC StatusCode、`.details`）。客户端**线程安全，可多线程共享**。

改了 proto 后重新生成 stub：`./scripts/gen-protos.sh python`

### 5.2 TypeScript / JavaScript

```bash
cd sdks/typescript && npm install && npm run build
```

```js
const { NotifyClient } = require("./sdks/typescript/dist/client.js");

const client = new NotifyClient("localhost:9987", "ntf_xxx");

const sub = client.subscribe(["alert.*"], (e) => console.log("收到:", e.topic, e.title));
await new Promise(r => setTimeout(r, 300));   // 等订阅生效

const ack = await client.publish("alert.db", "磁盘告警", "db-01 使用率 95%", {
  params: { host: "db-01" },
  // platforms: ["dingtalk-alert"],
  // dedupKey: "db-01-disk",
  // skipSubscribers: false, skipPlatforms: false,
});
console.log(ack.accepted, ack.eventId, ack.matchedPlatforms);

// 批量发布（双向流，单条失败不中断）
const acks = await client.publishBatch([
  { topic: "alert.db", title: "t1" },
  { title: "缺 topic 的非法请求" },     // -> accepted=false, error=INVALID_ARGUMENT: ...
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

> 注意字段名：TS 侧是 **camelCase**（`eventId` / `matchedPlatforms` / `dedupKey` / `rateLimitQps`），Python 侧是 **snake_case**。
> 运行时用 `@grpc/proto-loader` 加载 `proto/notify/v1/notify.proto`，无需代码生成。

### 5.3 Java

Gradle 依赖（本仓库内可直接 `implementation(project(":sdk-java"))`；外部工程先 `gradle publishNotifyHubToMavenLocal`，再引用 `io.notifyhub:sdk-java:0.1.0`）：

```java
try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987).token("ntf_xxx").build()) {
    // 发布
    PublishAck ack = client.publish(PublishRequest.newBuilder()
            .setTopic("alert.db").setTitle("磁盘告警").setContent("db-01 使用率 95%")
            .putParams("host", "db-01")
            .setOptions(Options.newBuilder().setDedupKey("db-01-disk").build())
            .build());
    System.out.println(ack.getAccepted() + " " + ack.getEventId());

    // 异步发布
    CompletableFuture<PublishAck> f = client.publishAsync(request);

    // 批量发布（双向流，省掉每条的 RPC 开销；单条错误不中断流）
    var session = client.publishStream(ack -> log.info("{}", ack.getEventId()));
    session.send(req1);
    session.send(req2);
    session.complete();
    session.await(10, TimeUnit.SECONDS);

    // 订阅（回调在 gRPC 线程，不要阻塞！）
    SubscriptionHandle sub = client.subscribe(List.of("alert.*"),
            e -> System.out.println("收到: " + e.getTopic()));
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

若走 TLS：`.usePlaintext(false)`。token 拦截器 `NotifyClient.TokenInterceptor` 是 public 的，可挂到自建 channel 上。

### 5.4 Go

```bash
./scripts/gen-protos.sh go     # 需要本机 Go 工具链，生成 sdks/go/gen
cd sdks/go && go mod tidy
```

```go
client, err := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
if err != nil { panic(err) }
defer client.Close()
ctx := context.Background()

ack, err := client.Publish(ctx, "alert", "部署完成", "v1.2.0 上线")
fmt.Println(ack.Accepted, ack.EventId)

// 批量发布（双向流，单条失败不中断）
acks, err := client.PublishBatch(ctx, []*v1.PublishRequest{
    {Topic: "alert.db", Title: "t1"},
    {Title: "缺 topic 的非法请求"},   // -> Accepted=false, Error="INVALID_ARGUMENT: ..."
})

sub, _ := client.Subscribe(ctx, []string{"alert.*"})
defer sub.Close()
go func() { for ev := range sub.Events() { fmt.Println("收到:", ev.Topic, ev.Title) } }()

client.UpsertPlatform(ctx, &v1.PlatformConfig{
    Name: "ding-alert", Type: "dingtalk",
    Webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx",
    Secret: "SECxxx", Topics: []string{"alert"},
})
```

> Go stub 需要生成（`sdks/go/gen` 不入仓库），生成后即可编译。
> 插件走 `go install`，若 `proxy.golang.org` 不可达，脚本会自动回落 `goproxy.cn`。

### 5.5 Spring Boot（自动配置）

仓库自带 starter：`sdks/spring-boot`（`io.notifyhub:notifyhub-spring-boot-starter`）。

```kotlin
implementation(project(":sdk-spring-boot"))   // 仓库内
// implementation("io.notifyhub:notifyhub-spring-boot-starter:0.1.0")  // gradle publishNotifyHubToMavenLocal 之后
```

`application.yml`：

```yaml
notifyhub:
  host: localhost
  port: 9987
  token: ntf_xxx
  platforms:                      # 可选：启动时用 Admin RPC 注册
    - name: ding-alert
      type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      secret: SECxxx
      topics: [alert, ops.*]
  subscriber:                     # 可选：启动即订阅
    enabled: true
    topics: [alert.*]
```

代码里注入即用：

```java
notify.publish("alert.db", "磁盘告警", "db-01 使用率 95%", Map.of("host", host));

// 批量：一条双向流发完并收集回执（单条失败只体现在对应回执上）
List<PublishAck> acks = notify.publishBatch(requests, Duration.ofSeconds(5));

@EventListener
public void onNotify(NotifyEvent e) { log.warn("{} {}", e.getTopic(), e.getTitle()); }
```

几个约定：

- 自动装配 `NotifyClient`（`destroyMethod="close"`）与 `NotifyHubTemplate`，全部带 `@ConditionalOnMissingBean` —— **自己声明同名 `@Bean` 即可完全接管**（换 TLS、换 target、加拦截器等）。
- 引入了 `spring-boot-actuator` 时自动注册 `NotifyHubHealthIndicator`，`/actuator/health` 里多出 `notifyhub` 一项。
- 引入了 Micrometer 时自动记录 `notifyhub.publish.total`（带 `topic`、`outcome` 标签）与 `notifyhub.publish.duration`；批量发布单独记 `notifyhub.publish.batch.*`。不引则零开销，也可自定义 `NotifyPublishListener` Bean 接管。
- `notifyhub.platforms` 走 Admin RPC，**仅存于服务端内存**，服务端重启后需重新注册（或写进服务端 YAML）。
- 总开关 `notifyhub.enabled=false` 可整体关闭；`notifyhub.fail-fast=true` 让平台注册失败直接中断启动。

完整说明见 [`sdks/spring-boot/README.md`](../sdks/spring-boot/README.md)；
可直接运行的工程见 [`examples/spring-boot/`](../examples/spring-boot/README.md)（含本地回显服务，能端到端看到投递 JSON）。

> 踩坑提醒：starter 的订阅挂在 Spring 的 `ApplicationStartedEvent` 上（早于 `CommandLineRunner`），
> 所以启动任务里发出的消息也能收到；但 gRPC 流建立仍有几十毫秒延迟，一次性任务里建议先 sleep 300ms 再发。

### 5.6 直接用 gRPC（无 SDK）

任何支持 gRPC 的语言都能直接调 `proto/notify/v1/notify.proto`（包的完整方法名 `notify.v1.Notify/*`），只需每次调用带上 metadata `x-api-token`：

```bash
grpcurl -plaintext -H 'x-api-token: ntf_xxx' -d '{"topic":"alert.db","title":"t","content":"c"}' \
  localhost:9987 notify.v1.Notify/Publish
grpcurl -plaintext localhost:9987 notify.v1.Notify/Ping
```

批量高频场景用双向流 `PublishStream`：一条错误（topic 为空、平台不存在）**不中断流**，而是回执 `accepted=false, error="..."`。
四语言 SDK 均已封装：Java（`publishStream` / `publishBatch`）、Python（`publish_batch`）、
TypeScript（`publishBatch`）、Go（`PublishBatch`）。

---

## 6. 运行时管理（Admin）

平台配置有两条来源：

| 来源 | 生效时机 | 是否持久化 |
|---|---|---|
| YAML `platforms[]` | 启动时加载 | ✅ 配置文件里 |
| `UpsertPlatform` RPC | 立即生效 | ❌ 仅内存，重启丢失 |

实践建议：**稳态平台写 YAML，临时/多租户场景用 Admin 动态注册**（重启后需重新注册）。
Admin 会校验 `name/type/webhook` 非空、`type` 必须是已注册渠道、`webhook` 必须 `http(s)://` 开头，否则 `INVALID_ARGUMENT`；同名覆盖；`RemovePlatform` 不存在的名字返回 `NOT_FOUND`。

---

## 7. 排错速查

| 现象 / 状态码 | 原因 | 处理 |
|---|---|---|
| `UNAUTHENTICATED` | 缺 `x-api-token` 或不在白名单 | 检查 `auth.tokens` 与客户端 token 是否一致 |
| `INVALID_ARGUMENT: topic 不能为空` | 发布未带 topic | 补 topic |
| `NOT_FOUND: 平台不存在: X` | `platforms` 指定了未注册的名字 / 删除了不存在的名字 | `list_platforms()` 先确认 |
| `UNAVAILABLE: 投递队列已满` | 瞬时洪峰，`queue_capacity` 打满 | 客户端退避重试；调大 `queue_capacity` / `workers`，或降低各平台 qps 压力 |
| `accepted=false, deduplicated=true` | 命中去重窗口 | 正常行为，换 `dedup_key` 或等窗口过去 |
| 没收到消息但 `accepted=true` | ① 订阅还没建立就发布 ② 通配规则不匹配 ③ 平台侧真的失败了 | 订阅后 sleep 片刻再发；核对通配；订阅 `deadletter` 看投递错误 |
| 启动时报 `未知平台类型` | `type` 拼错 | 只能是 `dingtalk / wecom / feishu / webhook` |
| 收不到钉钉消息 | 加签 secret 错 / 机器人安全设置 / 限流 20qps | 看服务端 ERROR 日志，`rate_limit_qps` 调到 ≤20 |
| 消息格式不对 | 模板没生效或用了未知占位符 | 占位符只支持 `title/content/topic/event_id/params.xxx` |

服务端日志（slf4j-simple，直接打到 stdout）是主要排查手段：投递成功 `INFO`、失败 `WARN`（带 attempt）、死信 `ERROR`。

### Spring Boot 专属

| 现象 | 原因 | 处理 |
|---|---|---|
| 启动日志没有「NotifyHub 平台已注册」 | `notifyhub.platforms` 没配，或注册失败但 `fail-fast=false` 只打了 WARN | 搜启动日志里的 `NotifyHub 平台注册失败`；需要强约束就设 `fail-fast=true` |
| 启动报 `notifyhub.platforms[].name 不能为空` | yml 里平台缺 name | 补 `name`（Admin 用它做唯一标识） |
| `@EventListener` 收不到事件 | ① `subscriber.enabled` 未设为 true ② topics 与发布 topic 不匹配 ③ 短命应用发完就退出 | 看启动日志是否有「NotifyHub 已订阅」；一次性任务发消息前先 sleep 300ms |
| 应用启动就报 `UNAUTHENTICATED` | 平台注册/探活用了无效 token | 核对 `notifyhub.token` 与服务端 `auth.tokens` |
| `/actuator/health` 里没有 notifyhub | 项目没引 `spring-boot-actuator` | 加上依赖；健康检查走免鉴权的 `Ping` |
| 用的是 Spring Boot 4.x | — | 无影响：starter 的 Spring 依赖是 `compileOnly`，运行期用你项目的版本（3.x / 4.x 均可） |

---

## 8. 测试与压测

```bash
gradle test                            # 单元 + E2E（WireMock 模拟平台 HTTP）
```

跨进程烟雾测试（四语言 SDK 对真实服务端）：

```bash
./server/build/install/server/bin/server --config smoke/config.yaml &

.venv/Scripts/python.exe smoke/py_smoke.py            # Windows
python3 smoke/py_smoke.py                             # Linux/macOS
node smoke/node_smoke.js                              # 需先 cd sdks/typescript && npm run build
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token        # 可用 -Ptarget=host:port 覆盖
cd smoke/go && go run .                               # 需先 ./scripts/gen-protos.sh go
```

吞吐基线（笔记本 CPU，Python 客户端压测，瓶颈在客户端）：约 **5.7k msg/s，p99=3.8ms**。服务端容量用 `ghz` 复测：

```bash
ghz --insecure -n 100000 -c 64 --call notify.v1.Notify/Publish \
    -d '{"topic":"bench","title":"t","content":"c"}' -H "x-api-token: ntf_xxx" 127.0.0.1:9987
```

`scripts/bench.py` 是仓库自带的 Python 压测脚本。

---

## 9. 已知边界（当前版本 0.1.0）

- 订阅为**至多一次**，不持久化、不重放；订阅端慢消费会丢事件。
- 平台投递**异步、尽力而为**：`Publish` 返回 `accepted=true` 只代表已入队，不代表平台已收到。
- Admin 注册的平台重启即失效。
- 尚无 Web 控制台、无投递记录查询（V2 规划中）。
- 浏览器直连需 gRPC-Web 网关（V2 规划中）。

---

## 10. 三分钟上手清单

1. `./scripts/gen-protos.sh java && gradle :server:installDist`
2. 写一份最小 `config.yaml`（见第 2 节：无鉴权 + 本地 webhook）
3. 起回显服务（19800）+ 起 NotifyHub（9987）
4. `pip install -e sdks/python`，跑 `examples/python/publish.py`
5. 看到回显服务打印出 JSON，链路就通了 → 换成真实机器人 + 打开 `auth.tokens`

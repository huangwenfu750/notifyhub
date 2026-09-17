# NotifyHub Java SDK

> English: [README.en.md](README.en.md)

gRPC 薄封装，无 Spring 依赖（Spring Boot 用户请用 [`sdks/spring-boot`](../spring-boot)）。

```kotlin
implementation(project(":sdk-java"))                              // 仓库内
implementation("io.github.huangwenfu750:sdk-java:0.1.0")                     // 外部工程
```

> 外部工程取包二选一：
> ① 先在本仓库跑 `gradle publishNotifyHubToMavenLocal`，使用方加 `repositories { mavenLocal() }`；
> ② 直接拉 GitHub Packages —— 该坐标**不在 Maven Central 上**，报 `Could not find ... in central`
> 就是没声明那个仓库（读也要带 `read:packages` 的 token），写法见
> [README.md](../../README.md#安装-sdk包管理平台)。

## 用法

```java
try (NotifyClient client = NotifyClient.newBuilder("localhost:9987")
        .token("ntf_xxx")
        .usePlaintext(true)      // 默认就是明文；用 TLS 时传 false
        .build()) {

    // 发布（带模板变量就自己构造 PublishRequest）
    PublishAck ack = client.publish("alert", "部署完成", "v1.2.0 上线");
    System.out.println(ack.getAccepted() + " " + ack.getEventId());

    PublishAck withParams = client.publish(PublishRequest.newBuilder()
            .setTopic("alert.db").setTitle("磁盘告警").setContent("db-01 使用率 95%")
            .putParams("env", "prod")
            .build());

    // 批量发布（一条双向流，单条失败不中断）
    var session = client.publishStream(a -> System.out.println(a.getEventId()));
    session.send(PublishRequest.newBuilder().setTopic("alert.db").setTitle("t1").build());
    session.send(PublishRequest.newBuilder().setTitle("缺 topic 的非法请求").build());
    session.complete();
    session.await(10, TimeUnit.SECONDS);

    // 订阅主题（至多一次、不持久化、不重放）
    var sub = client.subscribe(List.of("alert.*"), e -> System.out.println(e.getTitle()));
    ...
    sub.cancel("done");
}
```

## 能力

| 方法 | 说明 |
|---|---|
| `publish` / `publishAsync` | 单条发布（同步 / `CompletableFuture`） |
| `publishStream` | 双向流批量发布，返回 `PublishStreamSession`（`send` / `complete` / `await`） |
| `subscribe` | 订阅主题，返回 `SubscriptionHandle`（`await` / `cancel`） |
| `upsertPlatform` / `removePlatform` / `platforms` | 运行时管理推送平台（仅存于服务端内存） |
| `ping` | 免鉴权探活 |

异常：`io.grpc.StatusRuntimeException`（`INVALID_ARGUMENT` / `UNAUTHENTICATED` / `UNAVAILABLE` 等）。
客户端**线程安全，可多线程共享**；所有 RPC 都走同一条长连接。

## 本地验证

```bash
./scripts/gen-protos.sh java                       # 生成 stub（tools/ 自动下载 protoc）
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token     # 对真实服务端跑烟雾测试
```

# NotifyHub Spring Boot Starter

> English: [README.en.md](README.en.md)

在 `application.yml` 里配置 NotifyHub，注入 `NotifyHubTemplate` 即用。

## 1. 引入

本仓库内：

```kotlin
implementation(project(":sdk-spring-boot"))
```

外部项目（先 `gradle publishNotifyHubToMavenLocal`，该任务会连同 `protos`、`sdk-java` 一起发到本地仓库）：

```kotlin
repositories { mavenLocal() }

implementation("io.github.huangwenfu750:notifyhub-spring-boot-starter:0.1.0")
```

也可以直接从 GitHub Packages 取（**这个坐标不在 Maven Central 上**，报
`Could not find ... in central` 就是没声明该仓库；该 registry 读也要 token）：

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/huangwenfu750/notifyhub")
        credentials {                                  // 建议走环境变量，别硬编码
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")   // PAT 需 read:packages
        }
    }
}
```

Maven 用户见 [README.md](../../README.md#安装-sdk包管理平台) 里的 `settings.xml` 写法。

> Spring 相关依赖在 starter 里是 `compileOnly`，运行期由你项目的 Spring Boot 版本提供，兼容 3.x / 4.x。
> 编译版本由 `gradle.properties` 的 `springBootVersion` 控制。

## 2. application.yml

```yaml
notifyhub:
  host: localhost          # 默认 localhost
  port: 9987               # 默认 9987
  token: ntf_xxx           # 服务端 auth.tokens 中的一项
  # plaintext: true        # 默认明文；用 TLS 时设 false
  # fail-fast: false       # 启动时注册平台失败是否让应用启动失败（默认只告警）

  # 可选：启动时用 Admin RPC 注册/覆盖推送平台
  platforms:
    - name: ding-alert
      type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      secret: SECxxx
      topics: [alert, ops.*]
      template: "**{{title}}**\n{{content}}"
      rate-limit-qps: 8

  # 可选：启动时订阅，事件以 Spring ApplicationEvent 广播
  subscriber:
    enabled: true
    topics: [alert.*, deploy]
```

## 3. 发通知

```java
@Service
public class AlertService {

    private final NotifyHubTemplate notify;

    public AlertService(NotifyHubTemplate notify) {
        this.notify = notify;
    }

    public void alert(String host) {
        notify.publish("alert.db", "磁盘告警", "db-01 使用率 95%", Map.of("host", host));
    }
}
```

## 4. 批量发布

高频场景用双向流，省掉每条请求的 RPC 开销；**单条失败不会中断流**，只体现在对应回执上：

```java
List<PublishAck> acks = notify.publishBatch(requests, Duration.ofSeconds(5));
acks.forEach(a -> { if (!a.getAccepted()) log.warn("失败: {}", a.getError()); });

// 需要自己控制节奏时
var session = notify.publishStream(ack -> log.info("{}", ack.getEventId()));
session.send(req1); session.send(req2);
session.complete();
```

## 5. 收通知

```java
@Component
class NotifyListener {

    @EventListener
    public void onNotify(NotifyEvent e) {
        log.warn("[{}] {} {}", e.getTopic(), e.getTitle(), e.getContent());
    }
}
```

> 订阅桥挂在 Spring 的 `ApplicationStartedEvent` 上（**早于** `CommandLineRunner`/`ApplicationRunner`），
> 所以启动任务里发出的消息也能收到。gRPC 流建立仍有几十毫秒延迟，一次性任务里建议先 `sleep(300)` 再发。

也可以随时手动订阅：

```java
var handle = notify.subscribe(List.of("ops.#"), e -> log.info(e.getTitle()));
...
handle.cancel("done");
```

## 6. 运维与埋点

- 引入了 `spring-boot-actuator` 时自动注册 `NotifyHubHealthIndicator`，`/actuator/health` 里会出现 `notifyhub` 一项（走免鉴权的 `Ping`）。
- 引入了 `micrometer-core`（actuator 已自带）时自动注册 `MicrometerPublishListener`，产出指标：

| 指标 | 类型 | 说明 |
|---|---|---|
| `notifyhub.publish.total` | Counter | 单条 `publish`，标签 `topic`、`outcome=accepted\|deduplicated\|rejected\|error` |
| `notifyhub.publish.duration` | Timer | 单条 `publish` 耗时（无 topic 标签，避免基数爆炸） |
| `notifyhub.publish.batch.duration` | Timer | 批量 `publishBatch` 整批耗时 |
| `notifyhub.publish.batch.size` | DistributionSummary | 每批条数 |

> 批量走整批维度：双向流上无法把回执与请求逐一精确对应耗时，所以不复用单条耗时语义，
> 也**不会**计入 `notifyhub.publish.total`。

不想打点就别引 Micrometer；也可自定义 `NotifyPublishListener` Bean 覆盖默认实现。
埋点回调抛异常会被吞掉，绝不影响业务发布。
- `notify.ping()` / `notify.isReachable()` 可主动探活。
- `notify.platforms()` / `notify.upsertPlatform(...)` / `notify.removePlatform(...)` 运行时管理推送平台。

## 7. 在代码里配（完全接管）

所有自动配置的 Bean 都带 `@ConditionalOnMissingBean`，自己声明同名 Bean 即覆盖：

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

也可以在 `@PostConstruct` / `ApplicationRunner` 里动态注册平台：

```java
notify.upsertPlatform(PlatformConfig.newBuilder()
        .setName("wecom-ops").setType("wecom")
        .setWebhook("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx")
        .addTopics("ops.#").build());
```

## 8. 测试

```bash
gradle :sdk-spring-boot:test
```

除了纯配置断言，还包含 4 个**端到端用例**（`NotifyHubSpringEndToEndTest`）：在进程内拉起真实 NotifyHub 服务端
（`Server.create` + 随机端口）与真实 Spring 应用（走 `SpringApplication` 完整生命周期），
用 JDK 内置 `HttpServer` 接收投递，验证「发布 → 平台收到 HTTP → 事件回灌 `@EventListener` → 去重 → 批量发布 → 健康检查」整条链路。

## 9. 配置项

| 配置 | 默认 | 说明 |
|---|---|---|
| `notifyhub.enabled` | `true` | 总开关 |
| `notifyhub.host` / `notifyhub.port` | `localhost` / `9987` | 组合成 gRPC target |
| `notifyhub.token` | — | API token |
| `notifyhub.plaintext` | `true` | false = 启用 TLS |
| `notifyhub.fail-fast` | `false` | 注册平台失败时是否中断启动 |
| `notifyhub.platforms[].{name,type,webhook,secret,topics,template,at-mobiles,extra,rate-limit-qps}` | — | 启动时注册的平台 |
| `notifyhub.subscriber.enabled` / `notifyhub.subscriber.topics` | `false` / `[]` | 启动即订阅并广播 Spring 事件 |

> 注意：`platforms` 走的是 Admin RPC，**仅存于服务端内存**，服务端重启后需要重新注册（应用重启即可，或写进服务端 YAML）。

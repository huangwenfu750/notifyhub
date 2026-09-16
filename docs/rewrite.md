# 服务端重写指南（Java → Go / Rust / Zig）

> English: [rewrite.en.md](rewrite.en.md)

本项目最重要的架构约束就是为重写服务的。Java 版是参考实现，重写时以行为一致为验收标准。

## 必须遵守的硬约束

1. **proto 是唯一契约**：`proto/notify/v1/notify.proto` 的字段编号、名称、语义冻结；
   所有 SDK 只封装生成的 stub，不内嵌业务逻辑——重写只替换服务进程，SDK 与客户端不感知。
2. **分层等价物**：Java 版刻意保持核心层零框架依赖（无 Spring），重写时按层翻译：

   | Java 包 | 职责 | Go/Rust 对应物 |
   |---|---|---|
   | `io.notifyhub.transport` | gRPC 实现、proto↔内部模型转换、鉴权拦截 | `server/`（grpc-go / tonic） |
   | `io.notifyhub.core` | 路由、模板、令牌桶、去重、投递队列与重试 | 纯逻辑包/模块，无 IO 依赖 |
   | `io.notifyhub.channel` | `ChannelSender` SPI + 4 个适配器（含签名算法） | trait / interface |
   | `io.notifyhub.pubsub` | 订阅注册表（有界队列、慢消费者丢弃） | 每订阅有界 mpsc channel |
   | `io.notifyhub.config` | YAML 解析与校验 | 同键同默认值 |

3. **行为细节必须一致**（验收即比对行为）：
   - 路由通配符语义：`*` 恰好一段、`#` 零或多段且仅作末段；
   - 钉钉/飞书签名算法（见 docs/protocol.md 表格，两者密钥与被签数据相反）；
   - webhook 的 HMAC 对**原始请求体字节**计算；
   - ack 语义：`deduplicated`、`skip_platforms` 时 `matched_platforms` 为空、
     `PublishStream` 按条回执错误不中断流；
   - 错误码：空 topic → `INVALID_ARGUMENT`、显式平台不存在 → `NOT_FOUND`、
     队列满 → `UNAVAILABLE`、token 错 → `UNAUTHENTICATED`、`Ping` 免鉴权；
   - 死信：最终失败广播到 `deadletter`，params 含 `platform/topic/event_id/error`。

## 验收：语言无关的测试资产

- `server/src/test/java/io/notifyhub/e2e/ServerE2ETest.java`：WireMock 模拟平台 + 全量行为断言，
  重写后把"被测服务端"换成新实现即可复用场景清单（或按同名场景重写）。
- `smoke/`：Java/Python/Node/Go 四个跨进程烟雾测试，对任何实现直接运行。
- 契约测试基线：让 Java 版跑出请求/响应快照，作为新实现的黄金样本。

## 建议路线

1. 先实现 Go 版（生态里 grpc-go 与 grpc-java 行为最接近），
2. 用 `smoke/` 与 E2E 场景清单做双实现对拍（同一配置、同一请求序列、比对 ack 与 HTTP 出站），
3. 灰度切换端口，Java 版保留为参考实现。

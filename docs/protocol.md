# gRPC API 参考（notify.v1）

> English: [protocol.en.md](protocol.en.md)

协议契约：[`proto/notify/v1/notify.proto`](../proto/notify/v1/notify.proto)。该文件是唯一契约——字段编号与语义冻结，服务端重写（Go/Rust/Zig）时不得变更。

## 鉴权

- 服务端配置 `auth.tokens` 后，除 `Ping` 外所有方法要求 metadata 携带 `x-api-token: <token>`，否则返回 `UNAUTHENTICATED`。
- `auth.tokens` 为空时不鉴权。

## Notify.Publish（一元）

发布一条通知：按路由推送到平台 + 广播给在线订阅者。

| 字段 | 说明 |
|---|---|
| `topic` | 必填。业务主题，如 `alert.db`；为空返回 `INVALID_ARGUMENT` |
| `title` / `content` | 通知标题与正文 |
| `params` | 模板变量 `map<string,string>` |
| `platforms` | 显式指定目标平台名，覆盖路由；不存在返回 `NOT_FOUND` |
| `options.dedup_key` | 非空时启用去重：窗口期内重复 key 返回 `accepted=false, deduplicated=true`，不投递 |
| `options.skip_subscribers` | 不广播给订阅者 |
| `options.skip_platforms` | 不推送平台；此时 ack 的 `matched_platforms` 为空 |

成功返回 `PublishAck{event_id, accepted=true, matched_platforms}`（实际入队投递的平台）。
投递队列打满返回 `UNAVAILABLE`。

## Notify.Subscribe（服务端流）

订阅主题，实时接收事件。

- `topics` 支持 AMQP 风格通配符：`*` 恰好一段、`#` 零或多段（`.` 分隔）；`#` 只能作为末段。
- 语义：**至多一次**。慢消费者事件丢弃、不重放、不持久化。
- 客户端断开即自动退订。

## Notify.PublishStream（双向流）

批量发布：每收到一条 `PublishRequest` 回执一条 `PublishAck`。与 `Publish` 的差异：
单条错误（如 `topic` 为空、平台不存在）不中断流，而是返回 `accepted=false, error="..."` 的回执。

## Notify.UpsertPlatform / ListPlatforms / RemovePlatform（Admin）

用代码配置推送平台：

- `UpsertPlatform`：校验 `name/type/webhook`，`type` 必须是已注册渠道（`dingtalk | wecom | feishu | webhook`），非法返回 `INVALID_ARGUMENT`。同名覆盖。
- 平台配置**运行时生效，不持久化**——重启后需重新注册或写入配置文件。
- `RemovePlatform` 对不存在的名字返回 `NOT_FOUND`。

## Notify.Ping

健康检查，免鉴权。返回版本与运行时长。

## 投递语义（服务端 → 平台）

1. `Publish` 同步完成路由/去重/入队后即返回 ack；平台投递**异步**进行。
2. 每平台独立令牌桶限流（默认 15 qps，可配）。
3. 失败指数退避重试（默认 3 次，`backoff_ms` 基数 + 随机抖动，单次上限 30s）。
4. 最终失败写入死信日志，并广播到内置主题 `deadletter`（订阅 `deadletter` 可实时监控投递失败），事件 params 含 `platform/topic/event_id/error`。

## 各渠道实现细节

| 渠道 | 加签 | 请求 | 成功判定 |
|---|---|---|---|
| dingtalk | `urlencode(base64(HmacSHA256(key=secret, data=timestamp+"\n"+secret)))` 拼入 URL | `msgtype=markdown`，支持 `at_mobiles` | `errcode==0` |
| wecom | 无（key 在 URL） | `msgtype=markdown`，正文上限约 2000 字符 | `errcode==0` |
| feishu | `base64(HmacSHA256(key=timestamp+"\n"+secret, data=""))` 放入 body | `msg_type=text` | `code==0` 或 `StatusCode==0` |
| webhook | 可选 `extra.sign_header`（默认 `X-Signature`），值为 `hex(HmacSHA256(key=secret, data=原始请求体))` | JSON：`{event_id, topic, title, content, params, timestamp}` | HTTP 2xx |

模板占位符：`{{title}}`、`{{content}}`、`{{topic}}`、`{{event_id}}`、`{{params.xxx}}`（或直接 `{{xxx}}`）；未知占位符替换为空。

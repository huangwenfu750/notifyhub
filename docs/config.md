# 配置手册

> English: [config.en.md](config.en.md)

服务端读取 YAML 配置，优先级：`--config` 参数 > 环境变量 `NOTIFYHUB_CONFIG` > 工作目录 `config.yaml`。
完整示例见 [`config.example.yaml`](../config.example.yaml)。

## server

| 键 | 默认 | 说明 |
|---|---|---|
| `host` | `0.0.0.0` | gRPC 监听地址 |
| `port` | `9987` | gRPC 监听端口 |
| `workers` | CPU 核数（最小 4） | 平台投递 worker 线程数 |
| `queue_capacity` | `10000` | 异步投递队列容量；打满时 `Publish` 返回 `UNAVAILABLE` |

## auth

`tokens`: API token 白名单。**为空数组时服务不鉴权**（仅建议内网/本机调试使用）。

## defaults（全局默认，可被平台覆盖）

| 键 | 默认 | 说明 |
|---|---|---|
| `retry.max_attempts` | `3` | 单条投递最大尝试次数（含首次） |
| `retry.backoff_ms` | `500` | 退避基数，指数增长 + 随机抖动，单次上限 30s |
| `rate_limit.qps` | `15` | 每平台令牌桶限流（钉钉官方上限 20 qps） |
| `dedup_window_ms` | `60000` | `dedup_key` 去重窗口 |

## platforms[]（推送平台）

| 键 | 说明 |
|---|---|
| `name` | 必填，全局唯一（重复启动报错） |
| `type` | 必填：`dingtalk` / `wecom` / `feishu` / `webhook` |
| `webhook` / `url` | 必填（两键等价），目标地址，http(s):// |
| `secret` | 加签密钥：钉钉/飞书机器人的 SEC...；webhook 渠道为 HMAC 签名密钥 |
| `topics` | 路由规则数组，通配符 `*`（一段）/ `#`（多段）；缺省 `["*"]` 全匹配 |
| `template` | 可选。占位符模板；缺省 `title\ncontent` |
| `at_mobiles` | 可选。钉钉 @ 手机号 |
| `sign_header` | 可选（webhook）。签名头名称，默认 `X-Signature` |
| `retry` | 可选 `{max_attempts, backoff_ms}` 覆盖默认 |
| `rate_limit_qps` | 可选。覆盖默认 qps |

## 运行时管理

Admin RPC（`UpsertPlatform/ListPlatforms/RemovePlatform`）可在不重启的情况下增删改平台；
运行时注册的平台仅存于内存，重启后以配置文件为准。持久化与 Web 控制台规划在 V2。

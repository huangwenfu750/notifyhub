# Configuration Reference

> 中文版：[config.md](config.md)

The server reads a YAML config, resolved in this order: `--config` argument > environment
variable `NOTIFYHUB_CONFIG` > `config.yaml` in the working directory.
See [`config.example.yaml`](../config.example.yaml) for a complete example.

## server

| Key | Default | Description |
|---|---|---|
| `host` | `0.0.0.0` | gRPC listen address |
| `port` | `9987` | gRPC listen port |
| `workers` | CPU count (min 4) | Platform delivery worker thread count |
| `queue_capacity` | `10000` | Async delivery queue capacity; when full, `Publish` returns `UNAVAILABLE` |

## auth

`tokens`: API token allowlist. **When it is an empty array, authentication is disabled**
(recommended only for intranet/local debugging).

## defaults (global defaults, overridable per platform)

| Key | Default | Description |
|---|---|---|
| `retry.max_attempts` | `3` | Max attempts for a single delivery (including the first one) |
| `retry.backoff_ms` | `500` | Backoff base; exponential growth + jitter, capped at 30s per attempt |
| `rate_limit.qps` | `15` | Per-platform token bucket rate limit (DingTalk's official cap is 20 qps) |
| `dedup_window_ms` | `60000` | `dedup_key` deduplication window |

## platforms[] (push platforms)

| Key | Description |
|---|---|
| `name` | Required, globally unique (duplicate names fail startup) |
| `type` | Required: `dingtalk` / `wecom` / `feishu` / `webhook` |
| `webhook` / `url` | Required (the two keys are equivalent), target address, must be `http(s)://` |
| `secret` | Signing key: the `SEC...` of a DingTalk/Feishu bot; HMAC signing key for the webhook channel |
| `topics` | Routing rule array, wildcards `*` (one segment) / `#` (many segments); defaults to `["*"]` (match all) |
| `template` | Optional. Placeholder template; defaults to `title\ncontent` |
| `at_mobiles` | Optional. DingTalk @ phone numbers |
| `sign_header` | Optional (webhook). Signature header name, defaults to `X-Signature` |
| `retry` | Optional `{max_attempts, backoff_ms}` overriding the defaults |
| `rate_limit_qps` | Optional. Overrides the default qps |

## Runtime Management

The Admin RPCs (`UpsertPlatform/ListPlatforms/RemovePlatform`) add, change and remove platforms
without a restart; platforms registered at runtime live only in memory, and after a restart
the config file is authoritative again. Persistence and a web console are planned for V2.

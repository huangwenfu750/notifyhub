# gRPC API Reference (notify.v1)

> 中文版：[protocol.md](protocol.md)

Contract: [`proto/notify/v1/notify.proto`](../proto/notify/v1/notify.proto). This file is the single
source of truth — field numbers and semantics are frozen; a server rewrite (Go/Rust/Zig) must not change them.

## Authentication

- Once `auth.tokens` is configured, every method except `Ping` requires the metadata entry
  `x-api-token: <token>`, otherwise it returns `UNAUTHENTICATED`.
- When `auth.tokens` is empty, authentication is disabled.

## Notify.Publish (unary)

Publish one notification: route it to platforms and broadcast it to online subscribers.

| Field | Description |
|---|---|
| `topic` | Required. Business topic, e.g. `alert.db`; empty returns `INVALID_ARGUMENT` |
| `title` / `content` | Notification title and body |
| `params` | Template variables `map<string,string>` |
| `platforms` | Explicit target platform names, overriding routing; unknown name returns `NOT_FOUND` |
| `options.dedup_key` | When non-empty, enables deduplication: a repeated key inside the window returns `accepted=false, deduplicated=true` and is not delivered |
| `options.skip_subscribers` | Do not broadcast to subscribers |
| `options.skip_platforms` | Do not push to platforms; the ack's `matched_platforms` is then empty |

On success it returns `PublishAck{event_id, accepted=true, matched_platforms}` (the platforms
actually queued for delivery). A full delivery queue returns `UNAVAILABLE`.

## Notify.Subscribe (server streaming)

Subscribe to topics and receive events in real time.

- `topics` supports AMQP-style wildcards: `*` matches exactly one segment, `#` matches zero or
  more segments (separated by `.`); `#` may only appear as the last segment.
- Semantics: **at most once**. Slow consumers have events dropped; no replay, no persistence.
- Disconnecting unsubscribes automatically.

## Notify.PublishStream (bidi streaming)

Batch publishing: one `PublishAck` is returned per `PublishRequest` received. Difference from
`Publish`: a per-message error (empty `topic`, unknown platform) does not break the stream — it
returns an ack with `accepted=false, error="..."`.

## Notify.UpsertPlatform / ListPlatforms / RemovePlatform (Admin)

Configure push platforms from code:

- `UpsertPlatform`: validates `name/type/webhook`; `type` must be a registered channel
  (`dingtalk | wecom | feishu | webhook`), otherwise `INVALID_ARGUMENT`. Same name overwrites.
- Platform configuration **takes effect at runtime and is not persisted** — you must re-register
  after a restart or write it into the config file.
- `RemovePlatform` returns `NOT_FOUND` for a name that does not exist.

## Notify.Ping

Health check, no authentication required. Returns the version and uptime.

## Delivery Semantics (server → platform)

1. `Publish` returns the ack as soon as routing/dedup/queueing is done; platform delivery is **asynchronous**.
2. Each platform has an independent token bucket rate limit (default 15 qps, configurable).
3. Failures retry with exponential backoff (default 3 attempts, `backoff_ms` base + jitter, capped at 30s).
4. Final failure writes a dead-letter log entry and broadcasts to the built-in topic `deadletter`
   (subscribe to `deadletter` to monitor delivery failures in real time); the event params contain
   `platform/topic/event_id/error`.

## Per-Channel Implementation Details

| Channel | Signing | Request | Success check |
|---|---|---|---|
| dingtalk | `urlencode(base64(HmacSHA256(key=secret, data=timestamp+"\n"+secret)))` appended to the URL | `msgtype=markdown`, supports `at_mobiles` | `errcode==0` |
| wecom | None (the key is in the URL) | `msgtype=markdown`, body limit ~2000 chars | `errcode==0` |
| feishu | `base64(HmacSHA256(key=timestamp+"\n"+secret, data=""))` placed in the body | `msg_type=text` | `code==0` or `StatusCode==0` |
| webhook | Optional `extra.sign_header` (default `X-Signature`), value is `hex(HmacSHA256(key=secret, data=raw request body))` | JSON: `{event_id, topic, title, content, params, timestamp}` | HTTP 2xx |

Template placeholders: `{{title}}`, `{{content}}`, `{{topic}}`, `{{event_id}}`, `{{params.xxx}}`
(or simply `{{xxx}}`); unknown placeholders are replaced with an empty string.

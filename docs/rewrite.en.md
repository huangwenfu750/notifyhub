# Server Rewrite Guide (Java → Go / Rust / Zig)

> 中文版：[rewrite.md](rewrite.md)

The most important architectural constraint of this project exists to serve a rewrite.
The Java implementation is the reference one; behavioral equivalence is the acceptance criterion.

## Hard Constraints

1. **proto is the only contract**: field numbers, names and semantics of
   `proto/notify/v1/notify.proto` are frozen; every SDK merely wraps the generated stubs and embeds
   no business logic — a rewrite only replaces the server process, SDKs and clients are unaffected.
2. **Layered equivalents**: the Java version deliberately keeps the core layer framework-free
   (no Spring); translate layer by layer when rewriting:

   | Java package | Responsibility | Go/Rust counterpart |
   |---|---|---|
   | `io.notifyhub.transport` | gRPC implementation, proto↔internal model conversion, auth interceptor | `server/` (grpc-go / tonic) |
   | `io.notifyhub.core` | routing, templates, token bucket, dedup, delivery queue and retry | pure logic package/module, no IO dependency |
   | `io.notifyhub.channel` | `ChannelSender` SPI + 4 adapters (including signing algorithms) | trait / interface |
   | `io.notifyhub.pubsub` | subscription registry (bounded queues, slow consumers dropped) | one bounded mpsc channel per subscription |
   | `io.notifyhub.config` | YAML parsing and validation | same keys, same defaults |

3. **Behavioral details must match** (acceptance = behavior comparison):
   - Routing wildcard semantics: `*` is exactly one segment, `#` is zero or more segments and only as the last segment;
   - DingTalk/Feishu signing algorithms (see the table in [docs/protocol.en.md](protocol.en.md) — the key and the signed data are swapped between the two);
   - webhook HMAC is computed over the **raw request body bytes**;
   - ack semantics: `deduplicated`, `matched_platforms` empty when `skip_platforms` is set,
     `PublishStream` returns a per-message error without breaking the stream;
   - error codes: empty topic → `INVALID_ARGUMENT`, unknown explicit platform → `NOT_FOUND`,
     queue full → `UNAVAILABLE`, bad token → `UNAUTHENTICATED`, `Ping` requires no auth;
   - dead letter: final failure broadcasts to `deadletter`, params contain `platform/topic/event_id/error`.

## Acceptance: Language-Agnostic Test Assets

- `server/src/test/java/io/notifyhub/e2e/ServerE2ETest.java`: WireMock-stubbed platforms plus full
  behavioral assertions; after a rewrite, swap the "server under test" for the new implementation
  and reuse the scenario list (or rewrite it with identical scenario names).
- `smoke/`: four cross-process smoke tests (Java/Python/Node/Go) that run against any implementation.
- Contract test baseline: capture request/response snapshots from the Java version and use them as
  golden samples for the new implementation.

## Suggested Path

1. Implement the Go version first (grpc-go is the closest in behavior to grpc-java),
2. Use `smoke/` and the E2E scenario list for a head-to-head diff of both implementations
   (same config, same request sequence, compare acks and outbound HTTP),
3. Gradually shift traffic by port, keeping the Java version as the reference implementation.

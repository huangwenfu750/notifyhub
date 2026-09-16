# NotifyHub Java SDK

> 中文版：[README.md](README.md)

A thin gRPC wrapper with no Spring dependency (Spring Boot users should use
[`sdks/spring-boot`](../spring-boot)).

```kotlin
implementation(project(":sdk-java"))                              // inside this repo
implementation("io.notifyhub:sdk-java:0.1.0")                     // external projects
```

## Usage

```java
try (NotifyClient client = NotifyClient.newBuilder("localhost:9987")
        .token("ntf_xxx")
        .usePlaintext(true)      // plaintext is the default; pass false for TLS
        .build()) {

    // Publish (build a PublishRequest yourself when you need template variables)
    PublishAck ack = client.publish("alert", "Deployment finished", "v1.2.0 is live");
    System.out.println(ack.getAccepted() + " " + ack.getEventId());

    PublishAck withParams = client.publish(PublishRequest.newBuilder()
            .setTopic("alert.db").setTitle("Disk alert").setContent("db-01 at 95%")
            .putParams("env", "prod")
            .build());

    // Batch publish (a single bidi stream; one failure does not break the batch)
    var session = client.publishStream(a -> System.out.println(a.getEventId()));
    session.send(PublishRequest.newBuilder().setTopic("alert.db").setTitle("t1").build());
    session.send(PublishRequest.newBuilder().setTitle("an illegal request with no topic").build());
    session.complete();
    session.await(10, TimeUnit.SECONDS);

    // Subscribe to topics (at most once, not persisted, not replayed)
    var sub = client.subscribe(List.of("alert.*"), e -> System.out.println(e.getTitle()));
    ...
    sub.cancel("done");
}
```

## API

| Method | Description |
|---|---|
| `publish` / `publishAsync` | Single publish (sync / `CompletableFuture`) |
| `publishStream` | Bidi-stream batch publish, returns a `PublishStreamSession` (`send` / `complete` / `await`) |
| `subscribe` | Subscribe to topics, returns a `SubscriptionHandle` (`await` / `cancel`) |
| `upsertPlatform` / `removePlatform` / `platforms` | Runtime platform management (server memory only) |
| `ping` | Auth-free liveness probe |

Exceptions: `io.grpc.StatusRuntimeException` (`INVALID_ARGUMENT` / `UNAUTHENTICATED` / `UNAVAILABLE`, ...).
The client is **thread-safe and can be shared across threads**; all RPCs share one long-lived connection.

## Local Verification

```bash
./scripts/gen-protos.sh java                       # generate stubs (protoc is downloaded into tools/)
gradle :sdk-java:smoke -Ptoken=ntf_smoke_token     # smoke test against a real server
```

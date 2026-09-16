package io.notifyhub.spring;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.notifyhub.v1.Options;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifyHubMetricsTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotifyHubAutoConfiguration.class));

    private static PublishRequest req(String topic) {
        return PublishRequest.newBuilder().setTopic(topic).setTitle("t")
                .setOptions(Options.newBuilder().setDedupKey("k")).build();
    }

    @Test
    void 按结果分别计数并记耗时() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerPublishListener listener = new MicrometerPublishListener(registry);

        listener.onPublish(req("a.b"), 1_000_000,
                PublishAck.newBuilder().setAccepted(true).build(), null);
        listener.onPublish(req("a.b"), 1_000_000,
                PublishAck.newBuilder().setDeduplicated(true).build(), null);
        listener.onPublish(req("a.b"), 1_000_000,
                PublishAck.newBuilder().setAccepted(false).build(), null);
        listener.onPublish(req("a.b"), 1_000_000, null, new RuntimeException("boom"));

        assertEquals(1.0, registry.get("notifyhub.publish.total")
                .tag("topic", "a.b").tag("outcome", "accepted").counter().count());
        assertEquals(1.0, registry.get("notifyhub.publish.total")
                .tag("topic", "a.b").tag("outcome", "deduplicated").counter().count());
        assertEquals(1.0, registry.get("notifyhub.publish.total")
                .tag("topic", "a.b").tag("outcome", "rejected").counter().count());
        assertEquals(1.0, registry.get("notifyhub.publish.total")
                .tag("topic", "a.b").tag("outcome", "error").counter().count());
        assertEquals(4L, registry.get("notifyhub.publish.duration").timer().count());
    }

    @Test
    void 有MeterRegistry时监听器能取到并记数() {
        runner.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
                .run(ctx -> {
                    MicrometerPublishListener listener = ctx.getBean(MicrometerPublishListener.class);
                    assertNotNull(listener);

                    // 惰性取 registry：首次上报时才解析，应当能拿到容器里的 SimpleMeterRegistry
                    listener.onPublish(req("a.b"), 1_000_000,
                            PublishAck.newBuilder().setAccepted(true).build(), null);

                    SimpleMeterRegistry registry = ctx.getBean(SimpleMeterRegistry.class);
                    assertEquals(1.0, registry.get("notifyhub.publish.total")
                            .tag("topic", "a.b").tag("outcome", "accepted").counter().count());
                });
    }

    @Test
    void 批量发布走独立的整批指标() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerPublishListener listener = new MicrometerPublishListener(registry);

        listener.onBatch(3, 2_000_000);
        listener.onBatch(5, 4_000_000);

        assertEquals(2L, registry.get("notifyhub.publish.batch.duration").timer().count());
        assertEquals(2L, registry.get("notifyhub.publish.batch.size").summary().count());
        assertEquals(8.0, registry.get("notifyhub.publish.batch.size").summary().totalAmount());
        // 批量不计入单条指标
        assertEquals(0, registry.find("notifyhub.publish.total").counters().size());
    }

    @Test
    void 没有MeterRegistry时埋点静默跳过() {
        runner.run(ctx -> {
            MicrometerPublishListener listener = ctx.getBean(MicrometerPublishListener.class);
            assertNotNull(listener);
            // registry 未就绪时不应抛异常，也不应影响调用方
            listener.onPublish(req("a.b"), 1, PublishAck.newBuilder().setAccepted(true).build(), null);
        });
    }

    @Test
    void 埋点异常不影响业务发布() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NotifyHubTemplate template = new NotifyHubTemplate(
                io.notifyhub.sdk.NotifyClient.newBuilder("127.0.0.1:1").build(),
                java.util.List.of((r, nanos, ack, err) -> {
                    throw new RuntimeException("埋点挂了");
                }));
        // 监听器抛异常被吞掉，最终抛出的是 gRPC 本身错误（连不上 127.0.0.1:1），而非埋点错误
        org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException.class,
                () -> template.publish(req("x.y")));
        assertEquals(0, registry.getMeters().size());
    }
}

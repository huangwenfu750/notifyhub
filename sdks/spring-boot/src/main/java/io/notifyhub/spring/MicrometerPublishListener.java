package io.notifyhub.spring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 把发布行为记到 Micrometer（需 micrometer-core 在 classpath，由自动配置按需装配）。
 *
 * <p>指标：
 * <ul>
 *   <li>{@code notifyhub.publish.total}（Counter，tag: topic / outcome=accepted|deduplicated|rejected|error）— 仅单条 {@code publish}</li>
 *   <li>{@code notifyhub.publish.duration}（Timer，无 topic tag，避免标签基数爆炸）— 仅单条 {@code publish}</li>
 *   <li>{@code notifyhub.publish.batch.duration}（Timer）与 {@code notifyhub.publish.batch.size}（DistributionSummary）— 批量发布</li>
 * </ul>
 *
 * <p>registry 采用惰性解析：自动配置的装配顺序可能早于 MeterRegistry 就绪，
 * 因此首次上报时才取 registry；取不到就静默跳过，绝不因埋点影响业务。</p>
 */
public class MicrometerPublishListener implements NotifyPublishListener {

    private static final String METRIC = "notifyhub.publish";

    private final Supplier<MeterRegistry> registrySupplier;
    private volatile Meters meters;

    public MicrometerPublishListener(MeterRegistry registry) {
        this(() -> registry);
    }

    public MicrometerPublishListener(Supplier<MeterRegistry> registrySupplier) {
        this.registrySupplier = registrySupplier;
    }

    @Override
    public void onPublish(PublishRequest request, long durationNanos, PublishAck ack, Throwable error) {
        Meters m = meters();
        if (m == null) {
            return;
        }
        m.timer.record(durationNanos, TimeUnit.NANOSECONDS);
        String outcome = error != null ? "error"
                : (ack != null && ack.getDeduplicated() ? "deduplicated"
                : (ack != null && ack.getAccepted() ? "accepted" : "rejected"));
        String topic = request.getTopic();
        m.counters.computeIfAbsent(outcome + "|" + topic, key ->
                        Counter.builder(METRIC + ".total")
                                .description("NotifyHub 发布次数")
                                .tag("topic", topic)
                                .tag("outcome", outcome)
                                .register(m.registry))
                .increment();
    }

    private Meters meters() {
        Meters m = meters;
        if (m == null) {
            synchronized (this) {
                if (meters == null) {
                    MeterRegistry registry = registrySupplier.get();
                    if (registry == null) {
                        return null;
                    }
                    meters = new Meters(registry);
                }
                m = meters;
            }
        }
        return m;
    }

    @Override
    public void onBatch(int size, long durationNanos) {
        Meters m = meters();
        if (m == null) {
            return;
        }
        m.batchTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        m.batchSize.record(size);
    }

    private static final class Meters {
        private final MeterRegistry registry;
        private final Timer timer;
        private final Timer batchTimer;
        private final DistributionSummary batchSize;
        private final Map<String, Counter> counters = new ConcurrentHashMap<>();

        Meters(MeterRegistry registry) {
            this.registry = registry;
            this.timer = Timer.builder(METRIC + ".duration")
                    .description("NotifyHub 单条发布耗时")
                    .register(registry);
            this.batchTimer = Timer.builder(METRIC + ".batch.duration")
                    .description("NotifyHub 批量发布整批耗时")
                    .register(registry);
            this.batchSize = DistributionSummary.builder(METRIC + ".batch.size")
                    .description("NotifyHub 批量发布每批条数")
                    .register(registry);
        }
    }
}

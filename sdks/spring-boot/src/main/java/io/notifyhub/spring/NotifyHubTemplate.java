package io.notifyhub.spring;

import io.grpc.StatusRuntimeException;
import io.notifyhub.sdk.NotifyClient;
import io.notifyhub.v1.Event;
import io.notifyhub.v1.PlatformConfig;
import io.notifyhub.v1.Pong;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Spring 风格的客户端模板。
 *
 * <pre>{@code
 * template.publish("alert.db", "磁盘告警", "db-01 使用率 95%");
 * template.publish("alert.db", "磁盘告警", "...", Map.of("host", "db-01"));
 * }</pre>
 *
 * <p>底层 {@link NotifyClient} 线程安全、全局共享；本模板本身也是无状态的，可放心注入。</p>
 */
public class NotifyHubTemplate {

    private static final List<NotifyPublishListener> NO_LISTENERS = List.of();

    private final NotifyClient client;
    private final List<NotifyPublishListener> listeners;

    public NotifyHubTemplate(NotifyClient client) {
        this(client, NO_LISTENERS);
    }

    public NotifyHubTemplate(NotifyClient client, List<NotifyPublishListener> listeners) {
        this.client = client;
        this.listeners = listeners == null || listeners.isEmpty() ? NO_LISTENERS : List.copyOf(listeners);
    }

    // ---------- 发布 ----------

    public PublishAck publish(String topic, String title, String content) {
        return publish(topic, title, content, Collections.emptyMap());
    }

    public PublishAck publish(String topic, String title, String content, Map<String, String> params) {
        return publish(PublishRequest.newBuilder()
                .setTopic(topic)
                .setTitle(title)
                .setContent(content)
                .putAllParams(params)
                .build());
    }

    public PublishAck publish(PublishRequest request) {
        if (listeners.isEmpty()) {
            return client.publish(request);
        }
        long start = System.nanoTime();
        try {
            PublishAck ack = client.publish(request);
            fire(request, System.nanoTime() - start, ack, null);
            return ack;
        } catch (RuntimeException e) {
            fire(request, System.nanoTime() - start, null, e);
            throw e;
        }
    }

    private void fire(PublishRequest request, long nanos, PublishAck ack, Throwable error) {
        for (NotifyPublishListener l : listeners) {
            try {
                l.onPublish(request, nanos, ack, error);
            } catch (RuntimeException ignored) {
                // 埋点异常绝不能影响业务发布
            }
        }
    }

    public CompletableFuture<PublishAck> publishAsync(PublishRequest request) {
        return client.publishAsync(request);
    }

    /**
     * 批量发布（双向流）：一次性发送并在超时前收集全部回执。
     *
     * <p>单条失败（topic 为空、平台不存在）不会中断，对应回执的 {@code accepted=false} 且带 error。</p>
     *
     * @return 与入参顺序一致的回执列表（服务端按序回执）；超时则只返回已收到的部分
     */
    public List<PublishAck> publishBatch(Collection<PublishRequest> requests, Duration timeout) {
        List<PublishAck> acks = Collections.synchronizedList(new ArrayList<>(requests.size()));
        if (requests.isEmpty()) {
            return acks;
        }
        long start = System.nanoTime();
        NotifyClient.PublishStreamSession session = client.publishStream(acks::add);
        for (PublishRequest r : requests) {
            session.send(r);
        }
        session.complete();
        try {
            session.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (NotifyPublishListener l : listeners) {
            try {
                l.onBatch(requests.size(), System.nanoTime() - start);
            } catch (RuntimeException ignored) {
                // 埋点异常不影响业务
            }
        }
        return acks;
    }

    /** 需要自己控制发送节奏时用这个：拿到会话后自行 send/complete。 */
    public NotifyClient.PublishStreamSession publishStream(Consumer<PublishAck> onAck) {
        return client.publishStream(onAck);
    }

    // ---------- 订阅 ----------

    /** 订阅主题，回调在 gRPC 线程触发，请勿阻塞。 */
    public NotifyClient.SubscriptionHandle subscribe(Collection<String> topics, Consumer<Event> onEvent) {
        return client.subscribe(topics, onEvent);
    }

    // ---------- Admin ----------

    public PlatformConfig upsertPlatform(PlatformConfig config) {
        return client.upsertPlatform(config);
    }

    public List<PlatformConfig> platforms() {
        return client.platforms();
    }

    public void removePlatform(String name) {
        client.removePlatform(name);
    }

    // ---------- 运维 ----------

    /** 健康检查（免鉴权）。 */
    public Pong ping() {
        return client.ping();
    }

    /** @return true 表示连通且 token 有效 */
    public boolean isReachable() {
        try {
            client.ping();
            return true;
        } catch (StatusRuntimeException e) {
            return false;
        }
    }

    /** 需要直接用底层能力（双向流 PublishStream 等）时逃生用。 */
    public <T> T execute(Function<NotifyClient, T> callback) {
        return callback.apply(client);
    }

    public NotifyClient unwrap() {
        return client;
    }
}

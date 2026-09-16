package io.notifyhub.spring;

import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;

/**
 * 发布回调：用于埋点（Micrometer 等），不引入任何第三方类型。
 *
 * <p>实现放入 Spring 容器即可被 {@link NotifyHubTemplate} 自动发现；
 * 没有实现时发布路径零额外开销。</p>
 */
public interface NotifyPublishListener {

    /**
     * @param request   原始请求
     * @param durationNanos 耗时（纳秒）
     * @param ack       成功回执，失败时为 null
     * @param error     异常，成功时为 null
     */
    void onPublish(PublishRequest request, long durationNanos, PublishAck ack, Throwable error);

    /**
     * 批量发布（双向流）整体完成情况。
     *
     * <p>流上无法把回执与请求逐一精确对应耗时，因此批量只报整批维度，
     * 不复用 {@link #onPublish} 的单条耗时语义。</p>
     *
     * @param size          本批条数
     * @param durationNanos 整批耗时（纳秒）
     */
    default void onBatch(int size, long durationNanos) {
    }
}

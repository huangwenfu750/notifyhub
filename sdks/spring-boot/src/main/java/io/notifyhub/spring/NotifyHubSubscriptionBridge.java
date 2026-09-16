package io.notifyhub.spring;

import io.notifyhub.sdk.NotifyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;

/**
 * 应用启动后按 {@code notifyhub.subscriber.topics} 建立订阅，
 * 把收到的事件转成 {@link NotifyEvent} 在 Spring 容器内广播（可用 {@code @EventListener} 接收）。
 *
 * <p>监听 {@link ApplicationStartedEvent}（在 CommandLineRunner / ApplicationRunner <em>之前</em>触发），
 * 这样启动任务里发出的通知也能被收到；长驻应用与一次性任务表现一致。</p>
 *
 * <p>未配置 topics 或 enabled=false 时不做任何事。</p>
 */
public class NotifyHubSubscriptionBridge implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NotifyHubSubscriptionBridge.class);

    private final NotifyClient client;
    private final NotifyHubProperties props;
    private final ApplicationEventPublisher publisher;

    private volatile NotifyClient.SubscriptionHandle handle;

    public NotifyHubSubscriptionBridge(NotifyClient client, NotifyHubProperties props,
                                       ApplicationEventPublisher publisher) {
        this.client = client;
        this.props = props;
        this.publisher = publisher;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        NotifyHubProperties.Subscriber sub = props.getSubscriber();
        if (!sub.isEnabled() || sub.getTopics().isEmpty()) {
            return;
        }
        handle = client.subscribe(sub.getTopics(), ev -> {
            try {
                publisher.publishEvent(new NotifyEvent(this, ev));
            } catch (RuntimeException e) {
                log.warn("NotifyEvent 处理异常 topic={}: {}", ev.getTopic(), e.toString());
            }
        });
        log.info("NotifyHub 已订阅: topics={}", sub.getTopics());
    }

    @Override
    public void destroy() {
        NotifyClient.SubscriptionHandle h = handle;
        if (h != null) {
            h.cancel("application shutdown");
            handle = null;
        }
    }
}

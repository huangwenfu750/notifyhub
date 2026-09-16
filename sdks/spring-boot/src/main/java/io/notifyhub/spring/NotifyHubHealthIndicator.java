package io.notifyhub.spring;

import io.notifyhub.sdk.NotifyClient;
import io.notifyhub.v1.Pong;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * Actuator 健康检查（引入了 spring-boot-actuator 时自动生效）：调用免鉴权的 Ping。
 */
public class NotifyHubHealthIndicator extends AbstractHealthIndicator {

    private final NotifyClient client;

    public NotifyHubHealthIndicator(NotifyClient client) {
        this.client = client;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            Pong pong = client.ping();
            builder.up()
                    .withDetail("version", pong.getVersion())
                    .withDetail("uptimeSeconds", pong.getUptimeSeconds());
        } catch (Exception e) {
            builder.down(e);
        }
    }
}

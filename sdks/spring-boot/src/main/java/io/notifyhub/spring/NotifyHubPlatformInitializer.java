package io.notifyhub.spring;

import io.grpc.StatusRuntimeException;
import io.notifyhub.sdk.NotifyClient;
import io.notifyhub.v1.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 启动时把 {@code notifyhub.platforms} 通过 Admin RPC 注册到服务端。
 *
 * <p>注意：Admin 注册仅存于服务端内存，重启服务端后需重新注册（应用启动、或写进服务端 YAML）。
 * 注册失败默认只告警，除非 {@code notifyhub.fail-fast=true}。</p>
 */
public class NotifyHubPlatformInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(NotifyHubPlatformInitializer.class);

    private final NotifyClient client;
    private final NotifyHubProperties props;

    public NotifyHubPlatformInitializer(NotifyClient client, NotifyHubProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void afterPropertiesSet() {
        for (NotifyHubProperties.Platform p : props.getPlatforms()) {
            if (p.getName() == null || p.getName().isBlank()) {
                throw new IllegalStateException("notifyhub.platforms[].name 不能为空");
            }
            PlatformConfig.Builder b = PlatformConfig.newBuilder()
                    .setName(p.getName())
                    .addAllTopics(p.getTopics().isEmpty() ? java.util.List.of("*") : p.getTopics());
            if (p.getType() != null) b.setType(p.getType());
            if (p.getWebhook() != null) b.setWebhook(p.getWebhook());
            if (p.getSecret() != null) b.setSecret(p.getSecret());
            if (p.getTemplate() != null) b.setTemplate(p.getTemplate());
            if (p.getAtMobiles() != null) b.addAllAtMobiles(p.getAtMobiles());
            if (p.getExtra() != null) b.putAllExtra(p.getExtra());
            if (p.getRateLimitQps() > 0) b.setRateLimitQps(p.getRateLimitQps());
            try {
                client.upsertPlatform(b.build());
                log.info("NotifyHub 平台已注册: name={} type={} topics={}", p.getName(), p.getType(), p.getTopics());
            } catch (StatusRuntimeException e) {
                String msg = "NotifyHub 平台注册失败: name=" + p.getName() + " -> " + e.getStatus();
                if (props.isFailFast()) {
                    throw new IllegalStateException(msg, e);
                }
                log.warn(msg);
            }
        }
    }
}

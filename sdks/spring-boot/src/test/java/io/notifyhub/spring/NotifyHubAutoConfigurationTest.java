package io.notifyhub.spring;

import io.notifyhub.sdk.NotifyClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyHubAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotifyHubAutoConfiguration.class));

    @Test
    void 默认装配客户端与模板() {
        runner.run(ctx -> {
            assertNotNull(ctx.getBean(NotifyClient.class));
            assertNotNull(ctx.getBean(NotifyHubTemplate.class));
            assertNotNull(ctx.getBean(NotifyHubPlatformInitializer.class));
            assertNotNull(ctx.getBean(NotifyHubSubscriptionBridge.class));
            assertEquals("localhost:9987", ctx.getBean(NotifyHubProperties.class).target());
        });
    }

    @Test
    void 关闭开关后不装配() {
        runner.withPropertyValues("notifyhub.enabled=false")
                .run(ctx -> assertEquals(0, ctx.getBeanNamesForType(NotifyClient.class).length));
    }

    @Test
    void 绑定地址token平台与订阅() {
        runner.withPropertyValues(
                        "notifyhub.host=10.0.0.5",
                        "notifyhub.port=1234",
                        "notifyhub.token=ntf_x",
                        "notifyhub.platforms[0].name=ding-alert",
                        "notifyhub.platforms[0].type=dingtalk",
                        "notifyhub.platforms[0].webhook=https://oapi.dingtalk.com/robot/send?access_token=x",
                        "notifyhub.platforms[0].secret=SECx",
                        "notifyhub.platforms[0].topics[0]=alert",
                        "notifyhub.platforms[0].topics[1]=ops.*",
                        "notifyhub.platforms[0].rate-limit-qps=8",
                        "notifyhub.subscriber.enabled=true",
                        "notifyhub.subscriber.topics[0]=alert.*")
                .run(ctx -> {
                    NotifyHubProperties p = ctx.getBean(NotifyHubProperties.class);
                    assertEquals("10.0.0.5:1234", p.target());
                    assertEquals("ntf_x", p.getToken());
                    assertEquals(1, p.getPlatforms().size());

                    NotifyHubProperties.Platform pf = p.getPlatforms().get(0);
                    assertEquals("ding-alert", pf.getName());
                    assertEquals("dingtalk", pf.getType());
                    assertEquals("SECx", pf.getSecret());
                    assertEquals(2, pf.getTopics().size());
                    assertEquals("ops.*", pf.getTopics().get(1));
                    assertEquals(8, pf.getRateLimitQps());

                    assertTrue(p.getSubscriber().isEnabled());
                    assertEquals("alert.*", p.getSubscriber().getTopics().get(0));
                });
    }

    @Test
    void 自定义客户端优先于自动配置() {
        NotifyClient custom = NotifyClient.newBuilder("127.0.0.1:9987").build();
        runner.withBean(NotifyClient.class, () -> custom)
                .run(ctx -> {
                    assertSame(custom, ctx.getBean(NotifyClient.class));
                    assertSame(custom, ctx.getBean(NotifyHubTemplate.class).unwrap());
                });
    }

    @Test
    void 订阅桥监听的是ApplicationStartedEvent而非Ready() {
        // 回归保护：ApplicationReadyEvent 在 CommandLineRunner 之后才发，短命应用会漏收事件
        org.springframework.core.ResolvableType type = org.springframework.core.ResolvableType
                .forClass(ApplicationListener.class, NotifyHubSubscriptionBridge.class);
        assertEquals(org.springframework.boot.context.event.ApplicationStartedEvent.class,
                type.getGeneric(0).resolve());
    }

    @Test
    void 引入actuator时装配健康检查() {
        runner.run(ctx -> assertNotNull(ctx.getBean(NotifyHubHealthIndicator.class)));
    }
}

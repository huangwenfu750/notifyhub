package io.notifyhub.spring;

import io.notifyhub.sdk.NotifyClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NotifyHub 自动配置：读 {@code notifyhub.*} 配置，装配 {@link NotifyClient} 与 {@link NotifyHubTemplate}。
 *
 * <p>所有 Bean 都标注 {@code @ConditionalOnMissingBean}，因此在任意 {@code @Configuration} 里
 * 自己声明一个 {@link NotifyClient} 或 {@link NotifyHubTemplate} Bean 即可完全接管。</p>
 */
@AutoConfiguration
@ConditionalOnClass(NotifyClient.class)
@ConditionalOnProperty(prefix = "notifyhub", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NotifyHubProperties.class)
public class NotifyHubAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public NotifyClient notifyHubClient(NotifyHubProperties props) {
        return NotifyClient.newBuilder(props.target())
                .token(props.getToken())
                .usePlaintext(props.isPlaintext())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyHubTemplate notifyHubTemplate(NotifyClient client,
                                               ObjectProvider<NotifyPublishListener> listeners) {
        return new NotifyHubTemplate(client, listeners.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyHubPlatformInitializer notifyHubPlatformInitializer(NotifyClient client,
                                                                     NotifyHubProperties props) {
        return new NotifyHubPlatformInitializer(client, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyHubSubscriptionBridge notifyHubSubscriptionBridge(NotifyClient client,
                                                                   NotifyHubProperties props,
                                                                   ApplicationEventPublisher publisher) {
        return new NotifyHubSubscriptionBridge(client, props, publisher);
    }

    /** 仅当引入了 spring-boot-actuator 时生效。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    static class NotifyHubActuatorConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public NotifyHubHealthIndicator notifyHubHealthIndicator(NotifyClient client) {
            return new NotifyHubHealthIndicator(client);
        }
    }

    /** 仅当引入了 micrometer-core 时生效（starter 不强制依赖 Micrometer）。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class NotifyHubMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MicrometerPublishListener notifyHubMicrometerListener(
                ObjectProvider<io.micrometer.core.instrument.MeterRegistry> registry) {
            // 惰性取 registry：本自动配置的装配可能早于 MeterRegistry 就绪
            return new MicrometerPublishListener(registry::getIfAvailable);
        }
    }
}

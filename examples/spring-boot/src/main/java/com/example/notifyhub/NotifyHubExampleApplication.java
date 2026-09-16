package com.example.notifyhub;

import io.notifyhub.spring.NotifyEvent;
import io.notifyhub.spring.NotifyHubTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring Boot 集成示例：yml 配置 + 注入 {@link NotifyHubTemplate} + HTTP 触发 + {@code @EventListener} 收事件。
 *
 * <p>前置条件：
 * <ol>
 *   <li>启动 NotifyHub 服务端：{@code ./server/build/install/server/bin/server --config smoke/config.yaml}</li>
 *   <li>（可选）启动本地回显服务：{@code python echo_server.py}，否则投递失败并进入 deadletter</li>
 *   <li>{@code gradle publishNotifyHubToMavenLocal}</li>
 *   <li>{@code cd examples/spring-boot && gradle bootRun}</li>
 * </ol>
 *
 * <p>接口：
 * <ul>
 *   <li>{@code POST /api/notify} —— 发通知，body 含 topic/title/content/params/dedupKey</li>
 *   <li>{@code GET  /api/platforms} —— 服务端已注册平台</li>
 *   <li>{@code GET  /api/ping} —— 探活</li>
 * </ul>
 */
@SpringBootApplication
public class NotifyHubExampleApplication {

    private static final Logger log = LoggerFactory.getLogger(NotifyHubExampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NotifyHubExampleApplication.class, args);
    }

    /** 启动自检：探活 + 列出平台 + 打印可用的 curl。 */
    @Bean
    public CommandLineRunner startup(NotifyHubTemplate notify) {
        return args -> {
            log.info("ping -> {}", notify.ping().getVersion());
            log.info("已注册平台 -> {}", notify.platforms().stream().map(p -> p.getName()).toList());
            log.info("试试: curl -X POST localhost:8080/api/notify -H \"Content-Type: application/json\" "
                    + "-d \"{\\\"topic\\\":\\\"demo.deploy\\\",\\\"title\\\":\\\"部署完成\\\","
                    + "\\\"content\\\":\\\"v1.2.0 上线\\\"}\"");
        };
    }

    /** 收到的通知（notifyhub.subscriber 开启后由 starter 自动广播成 Spring 事件）。 */
    @Component
    static class DemoListener {

        private static final Logger log = LoggerFactory.getLogger(DemoListener.class);

        @EventListener
        public void onNotify(NotifyEvent e) {
            log.info("收到事件 -> topic={} title={} content={}", e.getTopic(), e.getTitle(), e.getContent());
        }
    }
}

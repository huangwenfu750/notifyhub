package io.notifyhub.spring;

import com.sun.net.httpserver.HttpServer;
import io.notifyhub.Server;
import io.notifyhub.config.HubConfig;
import io.notifyhub.config.PlatformConf;
import io.notifyhub.v1.Options;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端测试：进程内拉起真实 NotifyHub 服务端 + 真实 Spring 应用（走 SpringApplication 完整生命周期）。
 *
 * <p>覆盖：yml 装配 → 发布 → 平台真实投递 → 事件回灌到 {@code @EventListener} → 去重 → 健康检查。</p>
 */
class NotifyHubSpringEndToEndTest {

    private static HttpServer webhook;
    private static final List<String> delivered = Collections.synchronizedList(new ArrayList<>());
    private static Server server;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        webhook = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        webhook.createContext("/hook", exchange -> {
            delivered.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        webhook.start();

        HubConfig cfg = new HubConfig();
        cfg.host = "127.0.0.1";
        cfg.port = 0;                       // 让 gRPC 自选端口
        cfg.tokens = new ArrayList<>(List.of("ntf_e2e"));
        cfg.platforms = new ArrayList<>(List.of(PlatformConf.builder()
                .name("e2e-hook")
                .type("webhook")
                .webhook("http://127.0.0.1:" + webhook.getAddress().getPort() + "/hook")
                .topics(List.of("e2e.*"))
                .maxAttempts(2)
                .backoffMs(50)
                .build()));

        server = Server.create(cfg);
        server.start();
        port = server.actualPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.close();
        if (webhook != null) webhook.stop(0);
    }

    private ConfigurableApplicationContext runApp() {
        return new SpringApplicationBuilder(TestApp.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "notifyhub.host=127.0.0.1",
                        "notifyhub.port=" + port,
                        "notifyhub.token=ntf_e2e",
                        "notifyhub.subscriber.enabled=true",
                        "notifyhub.subscriber.topics[0]=e2e.*")
                .run();
    }

    @Test
    void 发布后平台收到投递且事件回灌到监听器() throws Exception {
        delivered.clear();
        try (ConfigurableApplicationContext ctx = runApp()) {
            NotifyHubTemplate template = ctx.getBean(NotifyHubTemplate.class);
            Thread.sleep(300);   // 等订阅流建立

            var ack = template.publish("e2e.test", "端到端标题", "端到端内容", Map.of("env", "test"));

            assertTrue(ack.getAccepted(), "发布应被接受");
            assertEquals(List.of("e2e-hook"), ack.getMatchedPlatformsList(), "应命中 e2e-hook");

            // 1) 事件回灌：starter 把它转成了 Spring ApplicationEvent
            NotifyEvent event = ctx.getBean(Listener.class).events.poll(5, TimeUnit.SECONDS);
            assertNotNull(event, "应收到回灌事件");
            assertEquals("e2e.test", event.getTopic());
            assertEquals("端到端标题", event.getTitle());
            assertEquals("test", event.getParams().get("env"));

            // 2) 平台真的收到了 HTTP 投递
            waitUntil(() -> !delivered.isEmpty());
            String body = delivered.get(0);
            assertTrue(body.contains("e2e.test"), "投递内容应含 topic: " + body);
            assertTrue(body.contains("端到端标题"), "投递内容应含 title: " + body);

            // 3) 健康检查
            assertEquals(Status.UP, ctx.getBean(NotifyHubHealthIndicator.class).health().getStatus());
        }
    }

    @Test
    void 相同dedupKey第二次被去重() throws Exception {
        try (ConfigurableApplicationContext ctx = runApp()) {
            NotifyHubTemplate template = ctx.getBean(NotifyHubTemplate.class);
            PublishRequest req = PublishRequest.newBuilder()
                    .setTopic("e2e.dup").setTitle("重复告警")
                    .setOptions(Options.newBuilder().setDedupKey("e2e-fixed-key").build())
                    .build();

            assertTrue(template.publish(req).getAccepted(), "首次应被接受");
            var second = template.publish(req);
            assertFalse(second.getAccepted(), "重复应被拒绝");
            assertTrue(second.getDeduplicated(), "应标记为去重");
        }
    }

    @Test
    void Admin注册平台后可用() throws Exception {
        delivered.clear();
        try (ConfigurableApplicationContext ctx = runApp()) {
            NotifyHubTemplate template = ctx.getBean(NotifyHubTemplate.class);
            template.upsertPlatform(io.notifyhub.v1.PlatformConfig.newBuilder()
                    .setName("runtime-hook")
                    .setType("webhook")
                    .setWebhook("http://127.0.0.1:" + webhook.getAddress().getPort() + "/hook")
                    .addTopics("runtime.*")
                    .build());

            assertTrue(template.platforms().stream()
                    .anyMatch(p -> p.getName().equals("runtime-hook")), "应能看到运行时注册的平台");

            var ack = template.publish("runtime.only", "运行时平台", "内容");
            assertEquals(List.of("runtime-hook"), ack.getMatchedPlatformsList());
            waitUntil(() -> delivered.stream().anyMatch(s -> s.contains("runtime.only")));
        }
    }

    @Test
    void 批量发布逐条回执且单条错误不中断流() throws Exception {
        delivered.clear();
        try (ConfigurableApplicationContext ctx = runApp()) {
            NotifyHubTemplate template = ctx.getBean(NotifyHubTemplate.class);

            List<PublishRequest> reqs = List.of(
                    req("e2e.batch", "第1条"),
                    PublishRequest.newBuilder().setTitle("缺 topic 的非法请求").build(),
                    req("e2e.batch", "第3条"));

            List<PublishAck> acks = template.publishBatch(reqs, Duration.ofSeconds(5));

            assertEquals(3, acks.size(), "单条错误不应中断流，仍应收到 3 条回执");
            assertTrue(acks.get(0).getAccepted(), "第 1 条应成功");
            assertFalse(acks.get(1).getAccepted(), "第 2 条（缺 topic）应失败");
            assertTrue(acks.get(1).getError().contains("INVALID_ARGUMENT"),
                    "应带上错误原因，实际: " + acks.get(1).getError());
            assertTrue(acks.get(2).getAccepted(), "第 3 条应成功（流未被打断）");

            waitUntil(() -> delivered.size() == 2);
        }
    }

    private static PublishRequest req(String topic, String title) {
        return PublishRequest.newBuilder().setTopic(topic).setTitle(title).build();
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("等待条件超时");
    }

    /** 测试用应用：开启自动配置 + 注册事件监听器。 */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApp {

        @Bean
        Listener listener() {
            return new Listener();
        }
    }

    static class Listener {
        final BlockingQueue<NotifyEvent> events = new LinkedBlockingQueue<>();

        @EventListener
        void on(NotifyEvent e) {
            events.add(e);
        }
    }
}

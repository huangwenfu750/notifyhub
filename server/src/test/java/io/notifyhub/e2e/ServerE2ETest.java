package io.notifyhub.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.notifyhub.Server;
import io.notifyhub.config.HubConfig;
import io.notifyhub.config.PlatformConf;
import io.notifyhub.sdk.NotifyClient;
import io.notifyhub.v1.Event;
import io.notifyhub.v1.NotifyGrpc;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 端到端集成测试：进程内启动服务端 + WireMock 模拟平台 HTTP API + Java SDK 客户端。 */
class ServerE2ETest {

    static WireMockServer platform;
    static Server server;
    static int port;
    static NotifyClient client;
    static NotifyClient anonClient;

    @BeforeAll
    static void setUp() throws Exception {
        platform = new WireMockServer(wireMockConfig().dynamicPort());
        platform.start();

        int wm = platform.port();

        HubConfig cfg = new HubConfig();
        cfg.host = "127.0.0.1";
        cfg.port = 0; // 随机端口
        cfg.tokens.add("secret-token");
        cfg.dedupWindowMs = 60_000;
        cfg.platforms = List.of(
                PlatformConf.builder().name("hook").type("webhook")
                        .webhook("http://127.0.0.1:" + wm + "/hook")
                        .topics(List.of("alert.*", "deploy"))
                        .extra(Map.of("sign_header", "X-Signature")).secret("ws3cr3t")
                        .maxAttempts(3).backoffMs(50).build(),
                PlatformConf.builder().name("ding").type("dingtalk")
                        .webhook("http://127.0.0.1:" + wm + "/dingtalk")
                        .secret("SECding").topics(List.of("alert"))
                        .maxAttempts(1).backoffMs(10).rateLimitQps(1000).build(),
                PlatformConf.builder().name("flaky").type("webhook")
                        .webhook("http://127.0.0.1:" + wm + "/flaky")
                        .topics(List.of("flaky")).maxAttempts(3).backoffMs(50).build(),
                PlatformConf.builder().name("dead").type("webhook")
                        .webhook("http://127.0.0.1:" + wm + "/dead")
                        .topics(List.of("never")).maxAttempts(1).backoffMs(10).build());
        server = Server.create(cfg);
        server.start();
        port = server.actualPort();

        client = NotifyClient.newBuilder("127.0.0.1", port).token("secret-token").build();
        anonClient = NotifyClient.newBuilder("127.0.0.1", port).build();

        platform.stubFor(post(urlEqualTo("/hook"))
                .willReturn(aResponse().withStatus(200).withBody("{\"ok\":true}")));
        platform.stubFor(post(urlEqualTo("/dingtalk"))
                .willReturn(aResponse().withStatus(200).withBody("{\"errcode\":0,\"errmsg\":\"ok\"}")));
        // flaky: 失败一次后成功（模拟重试路径）
        platform.stubFor(post(urlEqualTo("/flaky"))
                .willReturn(aResponse().withStatus(500).withBody("boom")
                        .withFixedDelay(0)));
        platform.stubFor(post(urlEqualTo("/dead"))
                .willReturn(aResponse().withStatus(500).withBody("always broken")));
    }

    @AfterAll
    static void tearDown() {
        client.close();
        anonClient.close();
        server.close();
        platform.stop();
    }

    private static void awaitAssert(Runnable assertion, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        RuntimeException last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                assertion.run();
                return;
            } catch (AssertionError | RuntimeException e) {
                last = e instanceof RuntimeException re ? re : new RuntimeException(e);
                Thread.sleep(50);
            }
        }
        if (last != null) throw last;
    }

    // ---------- 基础发布 + 路由 + webhook 签名 ----------

    @Test
    void publishRoutesToWebhookPlatformWithSignature() throws Exception {
        PublishAck ack = client.publish(PublishRequest.newBuilder()
                .setTopic("alert.db")
                .setTitle("磁盘告警")
                .setContent("磁盘使用率 95%")
                .putParams("host", "db-01")
                .build());
        assertTrue(ack.getAccepted());
        assertEquals(List.of("hook"), ack.getMatchedPlatformsList());
        assertFalse(ack.getEventId().isBlank());

        awaitAssert(() -> {
            var req = platform.findAll(postRequestedFor(urlEqualTo("/hook")));
            assertFalse(req.isEmpty());
            var captured = req.get(req.size() - 1);
            String body = captured.getBodyAsString();
            assertTrue(body.contains("alert.db"), "应包含 topic: " + body);
            assertTrue(body.contains("磁盘告警"), "应包含 title: " + body);
            assertTrue(body.contains("db-01"), "应包含 params: " + body);
            assertNotNull(captured.getHeader("X-Signature"), "应携带签名头");
            assertEquals(64, captured.getHeader("X-Signature").length());
        }, 5000);
    }

    @Test
    void dingtalkRequestCarriesSignatureParams() throws Exception {
        PublishAck ack = client.publish("alert", "Ding", "hello dingtalk");
        assertTrue(ack.getAccepted());
        assertTrue(ack.getMatchedPlatformsList().contains("ding"));

        awaitAssert(() -> {
            var captured = platform.findAll(postRequestedFor(urlMatching("/dingtalk.*"))).get(0);
            // 钉钉签名在 URL query 上
            assertTrue(captured.getUrl().contains("timestamp="), "URL 应带 timestamp: " + captured.getUrl());
            assertTrue(captured.getUrl().contains("sign="), "URL 应带 sign: " + captured.getUrl());
            assertTrue(captured.getBodyAsString().contains("msgtype"));
        }, 5000);
    }

    // ---------- 鉴权 ----------

    @Test
    void requiresToken() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> anonClient.publish("t", "a", "b"));
        assertEquals(Status.Code.UNAUTHENTICATED, e.getStatus().getCode());
        // Ping 免鉴权
        assertNotNull(anonClient.ping());
    }

    @Test
    void pingReturnsVersion() {
        var pong = client.ping();
        assertFalse(pong.getVersion().isBlank());
    }

    // ---------- 订阅 ----------

    @Test
    void subscribeReceivesMatchingEvents() throws Exception {
        List<Event> received = new CopyOnWriteArrayList<>();
        CountDownLatch got = new CountDownLatch(1);
        NotifyClient.SubscriptionHandle sub = client.subscribe(List.of("alert.*", "deploy"), e -> {
            received.add(e);
            got.countDown();
        });
        Thread.sleep(300); // 等订阅生效

        client.publish("ops.cpu", "不该收到", "");       // 不匹配
        client.publish("alert.db", "收到我", "v=1");      // alert.*
        assertTrue(got.await(5, TimeUnit.SECONDS), "应在超时前收到匹配事件");
        sub.cancel("done");

        assertEquals(1, received.size());
        Event e = received.get(0);
        assertEquals("alert.db", e.getTopic());
        assertEquals("收到我", e.getTitle());
        assertFalse(e.getEventId().isBlank());
        assertTrue(e.getTimestamp() > 0);
    }

    @Test
    void deadLetterOnPersistentFailure() throws Exception {
        List<Event> dead = new CopyOnWriteArrayList<>();
        CountDownLatch got = new CountDownLatch(1);
        NotifyClient.SubscriptionHandle sub = client.subscribe(List.of("deadletter"), e -> {
            dead.add(e);
            got.countDown();
        });
        Thread.sleep(300);

        PublishAck ack = client.publish("never", "会失败", "");
        assertTrue(ack.getAccepted());
        assertTrue(got.await(5, TimeUnit.SECONDS), "应收到死信事件");
        sub.cancel("done");

        Event e = dead.get(0);
        assertEquals("deadletter", e.getTopic());
        assertEquals("never", e.getParamsMap().get("topic"));
        assertEquals("dead", e.getParamsMap().get("platform"));
        assertTrue(e.getContent().contains("HTTP 500"), "死信应包含错误原因: " + e.getContent());
    }

    // ---------- 去重 ----------

    @Test
    void dedupKeyBlocksWindowedRepeats() {
        var opt = io.notifyhub.v1.Options.newBuilder().setDedupKey("same-key").build();
        PublishAck first = client.publish(PublishRequest.newBuilder()
                .setTopic("deploy").setTitle("1").setOptions(opt).build());
        PublishAck second = client.publish(PublishRequest.newBuilder()
                .setTopic("deploy").setTitle("2").setOptions(opt).build());
        assertTrue(first.getAccepted());
        assertFalse(second.getAccepted());
        assertTrue(second.getDeduplicated());
    }

    // ---------- 重试 ----------

    @Test
    void retriesFailedPlatformDelivery() throws Exception {
        // flaky 平台恒定返回 500 → 每次尝试失败都会重试（maxAttempts=3）
        client.publish("flaky", "重试测试", "");
        awaitAssert(() -> {
            var all = platform.findAll(postRequestedFor(urlEqualTo("/flaky")));
            assertTrue(all.size() >= 2, "至少重试过一次，实际 " + all.size());
            
        }, 8000);
    }

    // ---------- 显式指定平台 + NOT_FOUND ----------

    @Test
    void explicitPlatformOverrideAndNotFound() {
        PublishAck ack = client.publish(PublishRequest.newBuilder()
                .setTopic("anything")  // 不匹配任何路由
                .addPlatforms("hook")  // 显式指定
                .setTitle("直发").build());
        assertTrue(ack.getAccepted());
        assertEquals(List.of("hook"), ack.getMatchedPlatformsList());

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.publish(
                PublishRequest.newBuilder().setTopic("x").addPlatforms("no-such").build()));
        assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    // ---------- Admin：代码配置平台 ----------

    @Test
    void adminUpsertListRemove() throws Exception {
        int wm = platform.port();
        var created = client.upsertPlatform(io.notifyhub.v1.PlatformConfig.newBuilder()
                .setName("run-hook")
                .setType("webhook")
                .setWebhook("http://127.0.0.1:" + wm + "/run-hook")
                .addTopics("runtime.*")
                .build());
        assertEquals("run-hook", created.getName());

        assertTrue(client.platforms().stream().anyMatch(p -> p.getName().equals("run-hook")));

        // 新平台立刻可用
        PublishAck ack = client.publish(PublishRequest.newBuilder()
                .setTopic("runtime.job").setTitle("运行时注册的平台也能收到").build());
        assertTrue(ack.getMatchedPlatformsList().contains("run-hook"));
        awaitAssert(() -> assertEquals(1, platform.findAll(postRequestedFor(urlEqualTo("/run-hook"))).size()), 5000);

        client.removePlatform("run-hook");
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> client.publish(PublishRequest.newBuilder().setTopic("runtime.job").addPlatforms("run-hook").build()));
        assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    // ---------- 批量流 ----------

    @Test
    void publishStreamAcksEachMessage() throws Exception {
        ManagedChannel ch = ManagedChannelBuilder.forAddress("127.0.0.1", port)
                .usePlaintext()
                .intercept(new io.notifyhub.sdk.NotifyClient.TokenInterceptor("secret-token"))
                .build();
        try {
            NotifyGrpc.NotifyStub stub = NotifyGrpc.newStub(ch);
            List<PublishAck> acks = new ArrayList<>();
            CountDownLatch done = new CountDownLatch(1);
            var requestObserver = stub.publishStream(new io.grpc.stub.StreamObserver<>() {
                @Override public void onNext(PublishAck value) { acks.add(value); }
                @Override public void onError(Throwable t) { done.countDown(); }
                @Override public void onCompleted() { done.countDown(); }
            });
            requestObserver.onNext(PublishRequest.newBuilder().setTopic("alert.batch").setTitle("m1").build());
            requestObserver.onNext(PublishRequest.newBuilder().setTopic("alert.batch").setTitle("m2").build());
            requestObserver.onNext(PublishRequest.newBuilder().setTopic("bad topic!") // 合法 topic；仅测多条回执
                    .build());
            requestObserver.onCompleted();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(3, acks.size());
            assertTrue(acks.stream().allMatch(a -> a.getEventId() != null && !a.getEventId().isBlank()));
        } finally {
            ch.shutdownNow();
        }
    }

    // ---------- skip 开关 ----------

    @Test
    void skipFlagsWork() {
        var opt = io.notifyhub.v1.Options.newBuilder().setSkipSubscribers(true).setSkipPlatforms(true).build();
        PublishAck ack = client.publish(PublishRequest.newBuilder()
                .setTopic("deploy").setTitle("静默").setOptions(opt).build());
        assertTrue(ack.getAccepted());
        assertEquals(0, ack.getMatchedPlatformsCount()); // 未入队平台

        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> client.publish(
                PublishRequest.newBuilder().setTopic("").setTitle("无主题").build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
    }
}

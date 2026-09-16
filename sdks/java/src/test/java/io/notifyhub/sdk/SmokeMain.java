package io.notifyhub.sdk;

import io.notifyhub.v1.PublishAck;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Java SDK 跨进程烟雾测试（对运行中的 NotifyHub 执行）。
 *
 * 用法: gradle :sdk-java:smoke -Ptarget=127.0.0.1:9987 -Ptoken=ntf_smoke_token
 */
public final class SmokeMain {

    public static void main(String[] args) throws Exception {
        String target = System.getProperty("notifyhub.target", "127.0.0.1:9987");
        String token = System.getProperty("notifyhub.token", "");

        NotifyClient.Builder b = NotifyClient.newBuilder(target);
        if (!token.isBlank()) b.token(token);
        try (NotifyClient client = b.build()) {
            // 1. ping
            var pong = client.ping();
            check(!pong.getVersion().isBlank(), "ping");
            System.out.println("[1] ping ok: " + pong.getVersion());

            // 2. 发布 + 订阅
            List<io.notifyhub.v1.Event> received = new CopyOnWriteArrayList<>();
            CountDownLatch got = new CountDownLatch(1);
            var sub = client.subscribe(List.of("smoke.*"), e -> {
                received.add(e);
                got.countDown();
            });
            Thread.sleep(500);
            PublishAck ack = client.publish("smoke.java", "Java 发布成功", "来自 SmokeMain");
            check(ack.getAccepted() && ack.getMatchedPlatformsList().contains("smoke-hook"),
                    "publish 路由失败: " + ack);
            System.out.println("[2] publish ok: " + ack.getEventId());

            check(got.await(5, TimeUnit.SECONDS) && received.get(0).getTopic().equals("smoke.java"),
                    "订阅未收到事件");
            System.out.println("[3] subscribe ok: " + received.get(0).getTitle());
            sub.cancel("done");

            // 3. dedup
            String key = "java-" + System.nanoTime();
            var a1 = client.publish(io.notifyhub.v1.PublishRequest.newBuilder()
                    .setTopic("smoke.dedup").setTitle("1")
                    .setOptions(io.notifyhub.v1.Options.newBuilder().setDedupKey(key)).build());
            var a2 = client.publish(io.notifyhub.v1.PublishRequest.newBuilder()
                    .setTopic("smoke.dedup").setTitle("2")
                    .setOptions(io.notifyhub.v1.Options.newBuilder().setDedupKey(key)).build());
            check(a1.getAccepted() && !a2.getAccepted() && a2.getDeduplicated(), "去重异常");
            System.out.println("[4] dedup ok");

            // 4. admin
            client.upsertPlatform(io.notifyhub.v1.PlatformConfig.newBuilder()
                    .setName("java-hook").setType("webhook")
                    .setWebhook("http://127.0.0.1:19800/java")
                    .addTopics("java.*").build());
            var ack2 = client.publish(io.notifyhub.v1.PublishRequest.newBuilder()
                    .setTopic("java.event").setTitle("运行时注册").build());
            check(ack2.getMatchedPlatformsList().contains("java-hook"), "运行时平台未生效");
            client.removePlatform("java-hook");
            System.out.println("[5] admin ok: " + ack2.getMatchedPlatformsList());
        }
        System.out.println("\nJava 烟雾测试全部通过 ✓");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    private SmokeMain() {}
}

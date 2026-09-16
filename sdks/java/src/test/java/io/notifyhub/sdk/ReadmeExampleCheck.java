package io.notifyhub.sdk;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;

/** README 示例代码的编译校验（不执行，只保证 API 签名没写错）。 */
final class ReadmeExampleCheck {

    private ReadmeExampleCheck() {}

    static void example() throws Exception {
        try (NotifyClient client = NotifyClient.newBuilder("localhost:9987")
                .token("ntf_xxx")
                .usePlaintext(true)
                .build()) {

            PublishAck ack = client.publish("alert", "部署完成", "v1.2.0 上线");
            System.out.println(ack.getAccepted() + " " + ack.getEventId());

            PublishAck withParams = client.publish(PublishRequest.newBuilder()
                    .setTopic("alert.db").setTitle("磁盘告警").setContent("db-01 使用率 95%")
                    .putParams("env", "prod")
                    .build());
            System.out.println(withParams.getEventId());

            var session = client.publishStream(a -> System.out.println(a.getEventId()));
            session.send(PublishRequest.newBuilder().setTopic("alert.db").setTitle("t1").build());
            session.send(PublishRequest.newBuilder().setTitle("缺 topic 的非法请求").build());
            session.complete();
            session.await(10, TimeUnit.SECONDS);

            var sub = client.subscribe(List.of("alert.*"), e -> System.out.println(e.getTitle()));
            sub.cancel("done");

            System.out.println(client.platforms().size());
            System.out.println(client.ping().getVersion());
        }
    }
}

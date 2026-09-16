package com.example.notifyhub;

import io.notifyhub.spring.NotifyHubTemplate;
import io.notifyhub.v1.Options;
import io.notifyhub.v1.PlatformConfig;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** HTTP 触发通知的示例接口。 */
@RestController
@RequestMapping("/api")
public class NotifyController {

    private final NotifyHubTemplate notify;

    public NotifyController(NotifyHubTemplate notify) {
        this.notify = notify;
    }

    public record PublishBody(String topic, String title, String content,
                              Map<String, String> params, String dedupKey) {}

    public record BatchBody(List<PublishBody> items) {}

    /** POST /api/notify —— 发一条通知。 */
    @PostMapping("/notify")
    public Map<String, Object> publish(@RequestBody PublishBody body) {
        return toResult(notify.publish(toRequest(body)));
    }

    /** POST /api/notify/batch —— 一条双向流发多条（单条失败不中断流）。 */
    @PostMapping("/notify/batch")
    public List<Map<String, Object>> publishBatch(@RequestBody BatchBody body) {
        List<PublishRequest> reqs = body.items().stream().map(this::toRequest).toList();
        return notify.publishBatch(reqs, Duration.ofSeconds(5)).stream().map(this::toResult).toList();
    }

    /** GET /api/platforms —— 服务端已注册的平台。 */
    @GetMapping("/platforms")
    public List<String> platforms() {
        return notify.platforms().stream().map(PlatformConfig::getName).toList();
    }

    /** GET /api/ping —— 免鉴权探活。 */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        var pong = notify.ping();
        return Map.of("version", pong.getVersion(), "uptimeSeconds", pong.getUptimeSeconds());
    }

    /** 注意：protobuf 的 setter 不接受 null，缺字段要跳过（让服务端按非法请求回执，而不是 NPE）。 */
    private PublishRequest toRequest(PublishBody body) {
        PublishRequest.Builder b = PublishRequest.newBuilder();
        if (body.topic() != null) b.setTopic(body.topic());
        if (body.title() != null) b.setTitle(body.title());
        if (body.content() != null) b.setContent(body.content());
        if (body.params() != null) b.putAllParams(body.params());
        b.setOptions(Options.newBuilder()
                .setDedupKey(body.dedupKey() == null ? "" : body.dedupKey())
                .build());
        return b.build();
    }

    private Map<String, Object> toResult(PublishAck ack) {
        return Map.of(
                "accepted", ack.getAccepted(),
                "deduplicated", ack.getDeduplicated(),
                "eventId", ack.getEventId(),
                "matchedPlatforms", (List<String>) ack.getMatchedPlatformsList(),
                "error", ack.getError());
    }
}

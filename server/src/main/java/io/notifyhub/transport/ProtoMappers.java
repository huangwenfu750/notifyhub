package io.notifyhub.transport;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;
import io.notifyhub.v1.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** proto(v1) 与内部模型互转。proto 是对外契约，内部模型是将来重写时的等价物。 */
final class ProtoMappers {

    private ProtoMappers() {}

    static Message toMessage(io.notifyhub.v1.PublishRequest req, String eventId) {
        Map<String, String> params = new HashMap<>();
        req.getParamsMap().forEach(params::put);
        return new Message(eventId, req.getTopic(), req.getTitle(), req.getContent(),
                Map.copyOf(params), System.currentTimeMillis());
    }

    static Event toEvent(Message msg) {
        return Event.newBuilder()
                .setTopic(msg.topic())
                .setTitle(nullToEmpty(msg.title()))
                .setContent(nullToEmpty(msg.content()))
                .putAllParams(msg.params())
                .setEventId(msg.eventId())
                .setTimestamp(msg.timestamp())
                .build();
    }

    static PlatformConf toConf(io.notifyhub.v1.PlatformConfig proto, int defaultAttempts,
                               long defaultBackoffMs, int defaultQps) {
        PlatformConf.Builder b = PlatformConf.builder()
                .name(proto.getName().trim())
                .type(proto.getType().trim().toLowerCase(java.util.Locale.ROOT))
                .webhook(proto.getWebhook().trim())
                .secret(blankToNull(proto.getSecret()))
                .topics(proto.getTopicsList().isEmpty() ? List.of("*") : proto.getTopicsList())
                .template(blankToNull(proto.getTemplate()))
                .atMobiles(proto.getAtMobilesList())
                .extra(proto.getExtraMap());
        if (proto.getRetry() != null && proto.getRetry().getMaxAttempts() > 0) {
            b.maxAttempts(proto.getRetry().getMaxAttempts());
        } else {
            b.maxAttempts(defaultAttempts);
        }
        if (proto.getRetry() != null && proto.getRetry().getBackoffMs() > 0) {
            b.backoffMs(proto.getRetry().getBackoffMs());
        } else {
            b.backoffMs(defaultBackoffMs);
        }
        b.rateLimitQps(proto.getRateLimitQps() > 0 ? proto.getRateLimitQps() : defaultQps);
        return b.build();
    }

    static io.notifyhub.v1.PlatformConfig toProto(PlatformConf conf) {
        io.notifyhub.v1.PlatformConfig.Builder b = io.notifyhub.v1.PlatformConfig.newBuilder()
                .setName(conf.name())
                .setType(conf.type())
                .setWebhook(conf.webhook())
                .setSecret(nullToEmpty(conf.secret()))
                .addAllTopics(conf.topics())
                .setTemplate(nullToEmpty(conf.template()))
                .addAllAtMobiles(conf.atMobiles())
                .putAllExtra(conf.extra())
                .setRateLimitQps(conf.rateLimitQps())
                .setRetry(io.notifyhub.v1.RetryPolicy.newBuilder()
                        .setMaxAttempts(conf.maxAttempts())
                        .setBackoffMs(conf.backoffMs()));
        return b.build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

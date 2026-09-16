package io.notifyhub.channel;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用 Webhook（兜底渠道）：POST 结构化 JSON，可配 HMAC-SHA256 签名头。
 * extra.sign_header 指定签名头名称（默认 X-Signature），签名为 hex(HmacSHA256(secret, 原始请求体))。
 * 2xx 视为成功。可对接任意未专门适配的平台。
 */
public final class WebhookSender implements ChannelSender {

    private final HttpPoster poster;

    public WebhookSender(HttpPoster poster) {
        this.poster = poster;
    }

    @Override
    public String type() {
        return "webhook";
    }

    @Override
    public SendResult send(PlatformConf conf, Message msg, String rendered) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event_id", msg.eventId());
        body.put("topic", msg.topic());
        body.put("title", Texts.orEmpty(msg.title()));
        body.put("content", Texts.orEmpty(msg.content()));
        body.put("params", msg.params());
        body.put("timestamp", msg.timestamp());
        if (rendered != null && !rendered.isBlank()) {
            body.put("rendered", rendered);
        }
        String json = HttpPoster.toJson(body);

        Map<String, String> headers = new LinkedHashMap<>();
        String signHeader = conf.extra().getOrDefault("sign_header", "X-Signature");
        if (conf.secret() != null && !conf.secret().isBlank()) {
            headers.put(signHeader, SignUtil.webhookSignHex(conf.secret(), json));
        }
        return poster.postRawJson(conf.webhook(), json, headers);
    }
}

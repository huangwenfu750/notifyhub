package io.notifyhub.channel;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 钉钉自定义机器人。
 * 文档: https://open.dingtalk.com/document/robots/custom-robot-access
 * 加签: timestamp + "\n" + secret 作为被签名串，secret 为密钥，结果 base64 后 urlencode，
 *      拼接为 &amp;timestamp=xxx&amp;sign=xxx。
 * 响应: {"errcode":0,"errmsg":"ok"}
 */
public final class DingTalkSender implements ChannelSender {

    /** 钉钉 markdown 消息 text 上限约 20000 字节，留余量 */
    private static final int MAX_TEXT = 9000;

    private final HttpPoster poster;

    public DingTalkSender(HttpPoster poster) {
        this.poster = poster;
    }

    @Override
    public String type() {
        return "dingtalk";
    }

    @Override
    public SendResult send(PlatformConf conf, Message msg, String rendered) {
        long ts = System.currentTimeMillis();
        String url = conf.webhook();
        if (conf.secret() != null && !conf.secret().isBlank()) {
            String sign = SignUtil.dingTalkSign(conf.secret(), ts);
            url = url + (url.contains("?") ? "&" : "?") + "timestamp=" + ts + "&sign=" + sign;
        }

        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("title", Texts.orEmpty(msg.title()).isBlank() ? "通知" : Texts.truncate(msg.title(), 64));
        // template 未配置时 TemplateRenderer 返回 null，回退到默认格式，避免发出空消息
        String text = rendered != null ? rendered : Texts.renderOr(conf, msg);
        markdown.put("text", Texts.truncate(text, MAX_TEXT));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);
        if (!conf.atMobiles().isEmpty()) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("atMobiles", conf.atMobiles());
            at.put("isAtAll", false);
            body.put("at", at);
        }

        SendResult r = poster.postJson(url, body, Map.of());
        if (!r.ok()) return r;
        String errcode = HttpPoster.readField(r.responseBody(), "errcode");
        if (!"0".equals(errcode)) {
            return SendResult.failure("钉钉返回 errcode=" + errcode + " body=" + r.responseBody());
        }
        return r;
    }

    /** 供测试与展示用：构造完整签名 URL。 */
    static String signedUrl(String webhook, String secret, long ts) {
        String sign = SignUtil.dingTalkSign(secret, ts);
        return webhook + (webhook.contains("?") ? "&" : "?") + "timestamp=" + ts + "&sign=" + sign;
    }
}

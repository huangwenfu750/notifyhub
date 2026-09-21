package io.notifyhub.channel;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 飞书自定义机器人。
 * 文档: https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot
 * 加签: 把 timestamp + "\n" + secret 当作密钥，对空字符串做 HmacSHA256，结果 base64。
 * 响应: {"code":0,"msg":"success"}（新）或 {"StatusCode":0,"StatusMsg":"success"}（旧）
 */
public final class FeishuSender implements ChannelSender {

    /** 飞书 text 消息上限约 150KB，保守截断 */
    private static final int MAX_TEXT = 9000;

    private final HttpPoster poster;

    public FeishuSender(HttpPoster poster) {
        this.poster = poster;
    }

    @Override
    public String type() {
        return "feishu";
    }

    @Override
    public SendResult send(PlatformConf conf, Message msg, String rendered) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (conf.secret() != null && !conf.secret().isBlank()) {
            long tsSec = System.currentTimeMillis() / 1000;
            body.put("timestamp", String.valueOf(tsSec));
            body.put("sign", SignUtil.feishuSign(conf.secret(), tsSec));
        }
        body.put("msg_type", "text");
        Map<String, Object> content = new LinkedHashMap<>();
        // template 未配置时 TemplateRenderer 返回 null，回退到默认格式，避免发出空消息
        String text = rendered != null ? rendered : Texts.renderOr(conf, msg);
        content.put("text", Texts.truncate(text, MAX_TEXT));
        body.put("content", content);

        SendResult r = poster.postJson(conf.webhook(), body, Map.of());
        if (!r.ok()) return r;
        String resp = r.responseBody();
        String code = HttpPoster.readField(resp, "code");
        String status = HttpPoster.readField(resp, "StatusCode");
        if (!"0".equals(code) && !"0".equals(status)) {
            return SendResult.failure("飞书返回 code=" + code + "/" + status + " body=" + resp);
        }
        return r;
    }
}

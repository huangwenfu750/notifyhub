package io.notifyhub.channel;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 企业微信群机器人。
 * 文档: https://developer.work.weixin.qq.com/document/path/91770
 * 请求: 直接 POST webhook 地址（key 已含在 URL 中），{"msgtype":"markdown","markdown":{"content":...}}
 * 响应: {"errcode":0,"errmsg":"ok"}
 */
public final class WeComSender implements ChannelSender {

    /** 企业微信 markdown content 上限 4096 字节 */
    private static final int MAX_CONTENT = 2000;

    private final HttpPoster poster;

    public WeComSender(HttpPoster poster) {
        this.poster = poster;
    }

    @Override
    public String type() {
        return "wecom";
    }

    @Override
    public SendResult send(PlatformConf conf, Message msg, String rendered) {
        Map<String, Object> markdown = new LinkedHashMap<>();
        // template 未配置时 TemplateRenderer 返回 null，回退到默认格式，避免发出空消息
        String text = rendered != null ? rendered : Texts.renderOr(conf, msg);
        markdown.put("content", Texts.truncate(text, MAX_CONTENT));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);

        SendResult r = poster.postJson(conf.webhook(), body, Map.of());
        if (!r.ok()) return r;
        String errcode = HttpPoster.readField(r.responseBody(), "errcode");
        if (!"0".equals(errcode)) {
            return SendResult.failure("企业微信返回 errcode=" + errcode + " body=" + r.responseBody());
        }
        return r;
    }
}

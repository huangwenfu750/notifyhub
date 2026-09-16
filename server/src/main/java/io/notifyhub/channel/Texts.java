package io.notifyhub.channel;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;
import io.notifyhub.core.TemplateRenderer;

/** 渠道适配器公共工具。 */
final class Texts {

    private Texts() {}

    /** 渲染模板；未配置模板时返回默认格式 "title\ncontent"。 */
    static String renderOr(PlatformConf conf, Message msg) {
        String rendered = TemplateRenderer.render(conf.template, msg);
        if (rendered != null) return rendered;
        String title = orEmpty(msg.title());
        String content = orEmpty(msg.content());
        return title.isBlank() ? content : title + "\n" + content;
    }

    /** 按字符数截断（各平台对文本长度均有限制）。 */
    static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}

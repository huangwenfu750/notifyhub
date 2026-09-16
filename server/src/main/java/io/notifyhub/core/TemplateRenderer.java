package io.notifyhub.core;

import java.util.Map;

/** 轻量模板渲染：支持占位符 {{title}}、{{content}}、{{topic}}、{{params.xxx}}。 */
public final class TemplateRenderer {

    private TemplateRenderer() {}

    /**
     * @return 渲染结果；template 为空白时返回 null（由渠道适配器采用默认格式）
     */
    public static String render(String template, Message msg) {
        if (template == null || template.isBlank()) return null;
        StringBuilder out = new StringBuilder(template.length() + 64);
        int i = 0;
        while (i < template.length()) {
            int open = template.indexOf("{{", i);
            if (open < 0) {
                out.append(template, i, template.length());
                break;
            }
            out.append(template, i, open);
            int close = template.indexOf("}}", open + 2);
            if (close < 0) {
                out.append(template, open, template.length());
                break;
            }
            String key = template.substring(open + 2, close).trim();
            out.append(resolve(key, msg));
            i = close + 2;
        }
        return out.toString();
    }

    private static String resolve(String key, Message msg) {
        return switch (key) {
            case "title" -> nullToEmpty(msg.title());
            case "content" -> nullToEmpty(msg.content());
            case "topic" -> nullToEmpty(msg.topic());
            case "event_id" -> nullToEmpty(msg.eventId());
            default -> {
                if (key.startsWith("params.")) {
                    yield nullToEmpty(msg.params().get(key.substring(7)));
                }
                // 允许直接写参数名，如 {{env}}
                yield nullToEmpty(msg.params().get(key));
            }
        };
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

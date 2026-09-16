package io.notifyhub.core;

import io.notifyhub.config.PlatformConf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * topic 路由器，AMQP 风格通配符：
 * <ul>
 *   <li>以 {@code .} 分段；</li>
 *   <li>{@code *} 匹配恰好一段；</li>
 *   <li>{@code #} 匹配零或多段（仅可作为最后一段）。</li>
 * </ul>
 * 例：pattern "alert.*" 匹配 "alert.db"，"logs.#" 匹配 "logs"、"logs.a"、"logs.a.b"。
 */
public final class Router {

    private Router() {}

    public static boolean matches(String topic, String pattern) {
        if (topic == null || topic.isBlank() || pattern == null || pattern.isBlank()) return false;
        return match(topic.split("\\."), pattern.split("\\."));
    }

    static boolean match(String[] t, String[] p) {
        int ti = 0, pi = 0;
        // 单段回溯即可：'#' 只在末段出现，用贪心 + 尾部校验
        int lastHash = -1;
        for (int i = 0; i < p.length; i++) if ("#".equals(p[i])) lastHash = i;
        if (lastHash != -1 && lastHash != p.length - 1) return false; // '#' 仅允许在末段

        while (pi < p.length) {
            String seg = p[pi];
            if ("#".equals(seg)) {
                return true; // 末段 '#' 吞掉剩余全部（含零段）
            }
            if (ti >= t.length) return false;
            if (!"*".equals(seg) && !seg.equals(t[ti])) return false;
            ti++; pi++;
        }
        return ti == t.length;
    }

    /** 返回与 topic 匹配的全部平台，保持声明顺序。 */
    public static List<PlatformConf> route(Collection<PlatformConf> platforms, String topic) {
        List<PlatformConf> out = new ArrayList<>();
        for (PlatformConf p : platforms) {
            for (String pattern : p.topics) {
                if (matches(topic, pattern)) {
                    out.add(p);
                    break;
                }
            }
        }
        return out;
    }
}

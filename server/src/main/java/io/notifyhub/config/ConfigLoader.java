package io.notifyhub.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** YAML 配置加载与校验。 */
public final class ConfigLoader {

    private ConfigLoader() {}

    public static HubConfig load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("配置文件不存在: " + file.toAbsolutePath());
        }
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(file)) {
            Object root = yaml.load(in);
            if (!(root instanceof Map<?, ?> map)) {
                throw new IOException("配置文件格式错误: 顶层必须是 YAML 映射");
            }
            return parse(map);
        }
    }

    @SuppressWarnings("unchecked")
    static HubConfig parse(Map<?, ?> root) {
        HubConfig cfg = new HubConfig();

        Map<?, ?> server = map(root.get("server"));
        cfg.host = str(server.get("host"), cfg.host);
        cfg.port = (int) longVal(server.get("port"), cfg.port);
        cfg.workers = (int) longVal(server.get("workers"), cfg.workers);
        cfg.queueCapacity = (int) longVal(server.get("queue_capacity"), cfg.queueCapacity);

        Map<?, ?> auth = map(root.get("auth"));
        Object tokens = auth.get("tokens");
        if (tokens instanceof List<?> list) {
            list.forEach(t -> { String s = String.valueOf(t); if (!s.isBlank()) cfg.tokens.add(s); });
        }

        Map<?, ?> defaults = map(root.get("defaults"));
        Map<?, ?> retry = map(defaults.get("retry"));
        cfg.defaultMaxAttempts = (int) longVal(retry.get("max_attempts"), cfg.defaultMaxAttempts);
        cfg.defaultBackoffMs = longVal(retry.get("backoff_ms"), cfg.defaultBackoffMs);
        Map<?, ?> rateLimit = map(defaults.get("rate_limit"));
        cfg.defaultQps = (int) longVal(rateLimit.get("qps"), cfg.defaultQps);
        cfg.dedupWindowMs = longVal(defaults.get("dedup_window_ms"), cfg.dedupWindowMs);

        Object platforms = root.get("platforms");
        if (platforms instanceof List<?> list) {
            for (Object o : list) {
                cfg.platforms.add(parsePlatform((Map<?, ?>) o, cfg));
            }
        }

        validate(cfg);
        return cfg;
    }

    static PlatformConf parsePlatform(Map<?, ?> m, HubConfig cfg) {
        String name = str(m.get("name"), "");
        String type = str(m.get("type"), "").toLowerCase(Locale.ROOT);
        // webhook / url 两个键等价
        String webhook = str(m.get("webhook"), str(m.get("url"), ""));
        String secret = str(m.get("secret"), null);
        Object topicsObj = m.get("topics");
        List<String> topics = new ArrayList<>();
        if (topicsObj instanceof List<?> list) {
            list.forEach(t -> { String s = String.valueOf(t); if (!s.isBlank()) topics.add(s); });
        }
        if (topics.isEmpty()) topics.add("*");

        Map<?, ?> extraObj = map(m.get("extra"));
        Map<String, String> extra = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : extraObj.entrySet()) {
            extra.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
        }
        // webhook 渠道允许把 sign_header 写在顶层，收进 extra
        if (m.containsKey("sign_header")) {
            extra.put("sign_header", str(m.get("sign_header"), "X-Signature"));
        }

        List<String> atMobiles = new ArrayList<>();
        Object atObj = m.get("at_mobiles");
        if (atObj instanceof List<?> list) {
            list.forEach(t -> { String s = String.valueOf(t); if (!s.isBlank()) atMobiles.add(s); });
        }

        Map<?, ?> retry = map(m.get("retry"));
        int maxAttempts = (int) longVal(retry.get("max_attempts"), cfg.defaultMaxAttempts);
        long backoffMs = longVal(retry.get("backoff_ms"), cfg.defaultBackoffMs);
        int qps = (int) longVal(m.get("rate_limit_qps"), cfg.defaultQps);

        return PlatformConf.builder()
                .name(name).type(type).webhook(webhook).secret(secret)
                .topics(topics).template(str(m.get("template"), null))
                .atMobiles(atMobiles).extra(extra)
                .maxAttempts(maxAttempts).backoffMs(backoffMs).rateLimitQps(qps)
                .build();
    }

    static void validate(HubConfig cfg) {
        if (cfg.port < 1 || cfg.port > 65535) throw new IllegalArgumentException("server.port 非法: " + cfg.port);
        if (cfg.workers < 1) throw new IllegalArgumentException("server.workers 必须 >= 1");
        if (cfg.queueCapacity < 1) throw new IllegalArgumentException("server.queue_capacity 必须 >= 1");
        var seen = new java.util.HashSet<String>();
        for (PlatformConf p : cfg.platforms) {
            if (p.name == null || p.name.isBlank()) throw new IllegalArgumentException("平台缺少 name");
            if (!seen.add(p.name)) throw new IllegalArgumentException("平台名重复: " + p.name);
            if (p.type == null || p.type.isBlank()) throw new IllegalArgumentException("平台 " + p.name + " 缺少 type");
            if (p.webhook == null || p.webhook.isBlank()) throw new IllegalArgumentException("平台 " + p.name + " 缺少 webhook");
            if (!p.webhook.startsWith("http://") && !p.webhook.startsWith("https://")) {
                throw new IllegalArgumentException("平台 " + p.name + " 的 webhook 必须以 http(s):// 开头");
            }
        }
    }

    private static Map<?, ?> map(Object o) {
        return o instanceof Map<?, ?> m ? m : Map.of();
    }

    private static String str(Object o, String dflt) {
        return o == null ? dflt : String.valueOf(o);
    }

    private static long longVal(Object o, long dflt) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return dflt;
    }
}

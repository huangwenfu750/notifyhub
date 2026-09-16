package io.notifyhub.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @Test
    void parsesFullConfig() {
        Map<String, Object> retry = Map.of("max_attempts", 5, "backoff_ms", 200);
        Map<String, Object> ding = new LinkedHashMap<>();
        ding.put("name", "ding-1");
        ding.put("type", "dingtalk");
        ding.put("webhook", "https://oapi.dingtalk.com/robot/send?access_token=x");
        ding.put("secret", "SEC123");
        ding.put("topics", java.util.List.of("alert", "ops.*"));
        ding.put("template", "{{title}}\n{{content}}");
        ding.put("at_mobiles", java.util.List.of("13800000000"));
        ding.put("retry", retry);
        ding.put("rate_limit_qps", 10);

        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("name", "hook-1");
        hook.put("type", "webhook");
        hook.put("url", "https://example.com/hook");
        hook.put("sign_header", "X-Sig");

        Map<String, Object> root = Map.of(
                "server", Map.of("port", 12345, "workers", 2),
                "auth", Map.of("tokens", java.util.List.of("t1", "t2")),
                "defaults", Map.of(
                        "retry", Map.of("max_attempts", 3, "backoff_ms", 500),
                        "rate_limit", Map.of("qps", 20),
                        "dedup_window_ms", 5000),
                "platforms", java.util.List.of(ding, hook));

        HubConfig cfg = ConfigLoader.parse(root);
        assertEquals(12345, cfg.port);
        assertEquals(2, cfg.workers);
        assertEquals(java.util.List.of("t1", "t2"), cfg.tokens);
        assertEquals(2, cfg.platforms.size());

        PlatformConf d = cfg.platforms.get(0);
        assertEquals("ding-1", d.name());
        assertEquals("dingtalk", d.type());
        assertEquals("SEC123", d.secret());
        assertEquals(java.util.List.of("alert", "ops.*"), d.topics());
        assertEquals(5, d.maxAttempts());
        assertEquals(200, d.backoffMs());
        assertEquals(10, d.rateLimitQps());
        assertEquals(java.util.List.of("13800000000"), d.atMobiles());

        PlatformConf h = cfg.platforms.get(1);
        assertEquals("webhook", h.type());
        assertEquals("https://example.com/hook", h.webhook()); // url 与 webhook 键等价
        assertEquals("X-Sig", h.extra().get("sign_header"));   // 顶层 sign_header 收进 extra
        assertEquals(20, h.rateLimitQps());                    // 未配置时用全局默认
        assertEquals(5000, cfg.dedupWindowMs);
    }

    @Test
    void missingTopicsDefaultsToWildcard() {
        Map<String, Object> root = Map.of("platforms",
                java.util.List.of(Map.of("name", "p", "type", "webhook", "webhook", "https://a.com/x")));
        HubConfig cfg = ConfigLoader.parse(root);
        assertEquals(java.util.List.of("*"), cfg.platforms.get(0).topics());
    }

    @Test
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.parse(
                Map.of("server", Map.of("port", 70000))));
        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.parse(Map.of("platforms",
                java.util.List.of(
                        Map.of("name", "a", "type", "webhook", "webhook", "https://x"),
                        Map.of("name", "a", "type", "webhook", "webhook", "https://y")))));
        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.parse(Map.of("platforms",
                java.util.List.of(Map.of("name", "p", "type", "webhook"))))); // 缺 webhook
    }

    @Test
    void loadsFromYamlFile() throws Exception {
        String yaml = """
                server:
                  port: 19876
                auth:
                  tokens: ["tk"]
                platforms:
                  - name: dt
                    type: dingtalk
                    webhook: https://oapi.dingtalk.com/robot/send?access_token=t
                    secret: SECx
                    topics: ["alert.*"]
                """;
        java.nio.file.Path file = java.nio.file.Files.createTempFile("notifyhub", ".yaml");
        java.nio.file.Files.writeString(file, yaml);
        HubConfig cfg = ConfigLoader.load(file);
        assertEquals(19876, cfg.port);
        assertEquals(1, cfg.platforms.size());
        assertTrue(cfg.tokens.contains("tk"));
        java.nio.file.Files.deleteIfExists(file);
    }
}

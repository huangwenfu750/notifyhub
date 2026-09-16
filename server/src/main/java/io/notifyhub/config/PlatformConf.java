package io.notifyhub.config;

import java.util.List;
import java.util.Map;

/**
 * 平台（推送渠道）的运行时配置。纯 POJO，零框架依赖——
 * 这是服务端将来重写为 Go/Rust/Zig 时需要等价翻译的核心类型之一。
 */
public final class PlatformConf {

    public final String name;
    /** dingtalk | wecom | feishu | webhook */
    public final String type;
    /** 目标 webhook 地址 */
    public final String webhook;
    /** 加签密钥，可空 */
    public final String secret;
    /** 路由规则（topic 通配符列表） */
    public final List<String> topics;
    /** 渲染模板，可空（为空时用渠道默认格式） */
    public final String template;
    /** 钉钉 @ 手机号 */
    public final List<String> atMobiles;
    /** 扩展配置（如 webhook 的 sign_header） */
    public final Map<String, String> extra;
    /** 最大投递尝试次数（含首次） */
    public final int maxAttempts;
    /** 首次退避基数（毫秒），指数增长 + 抖动 */
    public final long backoffMs;
    /** 平台限流 QPS（0 用全局默认） */
    public final int rateLimitQps;

    public PlatformConf(String name, String type, String webhook, String secret,
                        List<String> topics, String template, List<String> atMobiles,
                        Map<String, String> extra, int maxAttempts, long backoffMs, int rateLimitQps) {
        this.name = name;
        this.type = type;
        this.webhook = webhook;
        this.secret = secret;
        this.topics = topics == null ? List.of() : List.copyOf(topics);
        this.template = template;
        this.atMobiles = atMobiles == null ? List.of() : List.copyOf(atMobiles);
        this.extra = extra == null ? Map.of() : Map.copyOf(extra);
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
        this.rateLimitQps = rateLimitQps;
    }

    // 访问器（record 风格，供核心层调用）
    public String name() { return name; }
    public String type() { return type; }
    public String webhook() { return webhook; }
    public String secret() { return secret; }
    public List<String> topics() { return topics; }
    public String template() { return template; }
    public List<String> atMobiles() { return atMobiles; }
    public Map<String, String> extra() { return extra; }
    public int maxAttempts() { return maxAttempts; }
    public long backoffMs() { return backoffMs; }
    public int rateLimitQps() { return rateLimitQps; }

    public Builder toBuilder() {
        return new Builder()
                .name(name).type(type).webhook(webhook).secret(secret)
                .topics(topics).template(template).atMobiles(atMobiles).extra(extra)
                .maxAttempts(maxAttempts).backoffMs(backoffMs).rateLimitQps(rateLimitQps);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String type;
        private String webhook;
        private String secret;
        private List<String> topics = List.of("*");
        private String template;
        private List<String> atMobiles = List.of();
        private Map<String, String> extra = Map.of();
        private int maxAttempts = 3;
        private long backoffMs = 500;
        private int rateLimitQps;

        public Builder name(String v) { this.name = v; return this; }
        public Builder type(String v) { this.type = v; return this; }
        public Builder webhook(String v) { this.webhook = v; return this; }
        public Builder secret(String v) { this.secret = v; return this; }
        public Builder topics(List<String> v) { this.topics = v; return this; }
        public Builder template(String v) { this.template = v; return this; }
        public Builder atMobiles(List<String> v) { this.atMobiles = v; return this; }
        public Builder extra(Map<String, String> v) { this.extra = v; return this; }
        public Builder maxAttempts(int v) { this.maxAttempts = v; return this; }
        public Builder backoffMs(long v) { this.backoffMs = v; return this; }
        public Builder rateLimitQps(int v) { this.rateLimitQps = v; return this; }

        public PlatformConf build() {
            return new PlatformConf(name, type, webhook, secret, topics, template,
                    atMobiles, extra, maxAttempts, backoffMs, rateLimitQps);
        }
    }
}

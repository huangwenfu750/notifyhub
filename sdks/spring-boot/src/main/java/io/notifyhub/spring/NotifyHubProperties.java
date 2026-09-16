package io.notifyhub.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotifyHub Spring Boot 配置（前缀 {@code notifyhub}）。
 *
 * <pre>{@code
 * notifyhub:
 *   host: localhost
 *   port: 9987
 *   token: ntf_xxx
 * }</pre>
 */
@ConfigurationProperties(prefix = "notifyhub")
public class NotifyHubProperties {

    /** 是否启用自动配置（默认 true）。设为 false 可完全关闭。 */
    private boolean enabled = true;

    /** 服务主机；与 port 组合成 gRPC target。 */
    private String host = "localhost";

    /** 服务端口。 */
    private int port = 9987;

    /** API token，对应服务端 auth.tokens 中的一项。 */
    private String token;

    /** true=明文（默认）；false=启用 TLS。 */
    private boolean plaintext = true;

    /** 启动时注册平台失败是否直接让应用启动失败（默认 false，仅告警）。 */
    private boolean failFast = false;

    /** 启动时通过 Admin RPC 注册/覆盖的推送平台（可选）。 */
    private List<Platform> platforms = new ArrayList<>();

    /** 启动时自动订阅（可选）。 */
    private final Subscriber subscriber = new Subscriber();

    /** gRPC target，形如 host:port。 */
    public String target() {
        return host + ":" + port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isPlaintext() {
        return plaintext;
    }

    public void setPlaintext(boolean plaintext) {
        this.plaintext = plaintext;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms == null ? new ArrayList<>() : platforms;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    /** 启动即订阅，事件以 Spring {@code ApplicationEvent} 形式广播。 */
    public static class Subscriber {

        /** 是否启用自动订阅（默认 false）。 */
        private boolean enabled = false;

        /** 订阅的 topic 列表，支持通配符 alert.* / ops.#。 */
        private List<String> topics = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics == null ? new ArrayList<>() : topics;
        }
    }

    /** 一个推送平台的配置，等价于 Admin RPC 的 UpsertPlatform。 */
    public static class Platform {

        /** 平台名，全局唯一。 */
        private String name;

        /** dingtalk | wecom | feishu | webhook。 */
        private String type;

        /** 目标地址，必须 http(s):// 开头。 */
        private String webhook;

        /** 加签密钥（钉钉/飞书的 SEC...，webhook 渠道的 HMAC 密钥）。 */
        private String secret;

        /** 路由规则，支持通配符；缺省 ["*"]。 */
        private List<String> topics = new ArrayList<>();

        /** 模板，如 "**{{title}}**\n{{content}}"。 */
        private String template;

        /** 钉钉 @ 手机号。 */
        private List<String> atMobiles = new ArrayList<>();

        /** 扩展配置，如 webhook 渠道的 sign_header。 */
        private Map<String, String> extra = new HashMap<>();

        /** 覆盖默认限流 qps，0 表示用服务端默认。 */
        private int rateLimitQps;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getWebhook() {
            return webhook;
        }

        public void setWebhook(String webhook) {
            this.webhook = webhook;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics == null ? new ArrayList<>() : topics;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public List<String> getAtMobiles() {
            return atMobiles;
        }

        public void setAtMobiles(List<String> atMobiles) {
            this.atMobiles = atMobiles == null ? new ArrayList<>() : atMobiles;
        }

        public Map<String, String> getExtra() {
            return extra;
        }

        public void setExtra(Map<String, String> extra) {
            this.extra = extra == null ? new HashMap<>() : extra;
        }

        public int getRateLimitQps() {
            return rateLimitQps;
        }

        public void setRateLimitQps(int rateLimitQps) {
            this.rateLimitQps = rateLimitQps;
        }
    }
}

package io.notifyhub.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 渠道适配器注册表：type 字符串 → sender。 */
public final class SenderRegistry {

    private final Map<String, ChannelSender> byType = new ConcurrentHashMap<>();
    private final HttpPoster poster;

    public SenderRegistry(HttpPoster poster) {
        this.poster = poster;
        registerBuiltins();
    }

    private void registerBuiltins() {
        register(new DingTalkSender(poster));
        register(new WeComSender(poster));
        register(new FeishuSender(poster));
        register(new WebhookSender(poster));
    }

    /** 注册或覆盖适配器（供插件化扩展）。 */
    public void register(ChannelSender sender) {
        byType.put(sender.type(), sender);
    }

    public ChannelSender get(String type) {
        return byType.get(type);
    }

    public boolean knows(String type) {
        return byType.containsKey(type);
    }
}

package io.notifyhub.channel;

import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Message;

/** 渠道适配器 SPI——新增平台（邮件、Slack、Telegram 等）只需实现本接口并注册。实现必须线程安全。 */
public interface ChannelSender {

    /** 平台类型标识，与配置中的 type 对应，如 "dingtalk"。 */
    String type();

    /** 执行一次投递（不含重试与限流，由 DeliveryService 统一负责）。 */
    SendResult send(PlatformConf conf, Message msg, String renderedContent);
}

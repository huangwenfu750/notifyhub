package io.notifyhub.core;

import java.util.Map;

/** 内部消息表示——服务端各层统一使用此类型，transport 层负责 proto 与之互转。 */
public record Message(
        String eventId,
        String topic,
        String title,
        String content,
        Map<String, String> params,
        long timestamp /* unix 毫秒 */) {

    public static final String DEADLETTER_TOPIC = "deadletter";
}

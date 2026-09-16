package io.notifyhub.spring;

import io.notifyhub.v1.Event;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 收到的 NotifyHub 事件，以 Spring {@link ApplicationEvent} 形式广播。
 *
 * <pre>{@code
 * @EventListener
 * public void onNotify(NotifyEvent e) {
 *     log.warn("{} {}", e.getTopic(), e.getTitle());
 * }
 * }</pre>
 */
public class NotifyEvent extends ApplicationEvent {

    private final Event event;

    public NotifyEvent(Object source, Event event) {
        super(source);
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    public String getTopic() {
        return event.getTopic();
    }

    public String getTitle() {
        return event.getTitle();
    }

    public String getContent() {
        return event.getContent();
    }

    public Map<String, String> getParams() {
        return event.getParamsMap();
    }

    public String getEventId() {
        return event.getEventId();
    }

    /** unix 毫秒（不叫 getTimestamp，避免与 ApplicationEvent 的 final 方法冲突） */
    public long getEventTimestamp() {
        return event.getTimestamp();
    }

    @Override
    public String toString() {
        return "NotifyEvent{topic=" + event.getTopic() + ", title=" + event.getTitle()
                + ", eventId=" + event.getEventId() + "}";
    }
}

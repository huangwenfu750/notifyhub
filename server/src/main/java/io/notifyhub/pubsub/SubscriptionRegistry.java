package io.notifyhub.pubsub;

import io.notifyhub.core.Message;
import io.notifyhub.core.Router;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 订阅注册表：每个订阅持有一个有界队列，
 * 慢消费者不阻塞发布方，溢出即丢弃（至多一次语义）。
 */
public final class SubscriptionRegistry {

    private static final int QUEUE_CAPACITY = 1024;

    public record Subscription(long id, Set<String> patterns,
                               BlockingQueue<Message> queue,
                               AtomicBoolean active, ReentrantLock lock) {}

    private final ConcurrentHashMap<Long, Subscription> subs = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong delivered = new AtomicLong();

    /** @throws IllegalArgumentException patterns 为空或含非法模式 */
    public Subscription register(Collection<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("至少订阅一个 topic");
        }
        for (String p : patterns) {
            if (p == null || p.isBlank()) throw new IllegalArgumentException("topic 模式不能为空");
        }
        Subscription s = new Subscription(ids.incrementAndGet(), Set.copyOf(patterns),
                new LinkedBlockingQueue<>(QUEUE_CAPACITY), new AtomicBoolean(true), new ReentrantLock());
        subs.put(s.id(), s);
        return s;
    }

    public void unregister(long id) {
        Subscription s = subs.remove(id);
        if (s != null) s.active().set(false);
    }

    /**
     * 广播给所有匹配的订阅者（不触发平台推送）。
     * @return 实际送达的订阅者数
     */
    public int broadcast(Message msg) {
        int count = 0;
        for (Subscription s : subs.values()) {
            if (!s.active().get()) continue;
            boolean matched = false;
            for (String p : s.patterns()) {
                if (Router.matches(msg.topic(), p)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) continue;
            // 多个 pump 线程可能同时 poll/关闭，加锁避免竞态
            s.lock().lock();
            try {
                if (!s.active().get()) continue;
                if (s.queue().offer(msg)) {
                    count++;
                    delivered.incrementAndGet();
                } else {
                    dropped.incrementAndGet(); // 慢消费者：丢弃
                }
            } finally {
                s.lock().unlock();
            }
        }
        return count;
    }

    public int subscriberCount() {
        return subs.size();
    }

    public long droppedCount() {
        return dropped.get();
    }

    public long deliveredCount() {
        return delivered.get();
    }

    public List<Subscription> snapshot() {
        return List.copyOf(subs.values());
    }
}

package io.notifyhub.core;

import io.notifyhub.channel.ChannelSender;
import io.notifyhub.channel.SenderRegistry;
import io.notifyhub.config.HubConfig;
import io.notifyhub.config.PlatformConf;
import io.notifyhub.pubsub.SubscriptionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 平台投递服务：有界队列 + worker 池，逐平台令牌桶限流，
 * 失败指数退避重试，最终失败写入死信日志并广播到 deadletter 主题。
 */
public final class DeliveryService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private static final long MAX_BACKOFF_MS = 30_000;

    private record Delivery(PlatformConf conf, Message msg) {}

    private final BlockingQueue<Delivery> queue;
    private final Thread[] workerThreads;
    private final SenderRegistry senders;
    private final SubscriptionRegistry subscribers;
    private final HubConfig cfg;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong succeeded = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public DeliveryService(SenderRegistry senders, SubscriptionRegistry subscribers, HubConfig cfg) {
        this.senders = senders;
        this.subscribers = subscribers;
        this.cfg = cfg;
        this.queue = new ArrayBlockingQueue<>(cfg.queueCapacity);
        this.workerThreads = new Thread[cfg.workers];
        for (int i = 0; i < cfg.workers; i++) {
            Thread t = new Thread(this::workLoop, "notifyhub-delivery-" + i);
            t.setDaemon(true);
            this.workerThreads[i] = t;
        }
        for (Thread t : workerThreads) t.start();
    }

    public enum EnqueueResult { ENQUEUED, QUEUE_FULL }

    public EnqueueResult enqueue(PlatformConf conf, Message msg) {
        accepted.incrementAndGet();
        boolean ok = queue.offer(new Delivery(conf, msg));
        if (!ok) {
            failed.incrementAndGet();
            log.warn("投递队列已满({}), 丢弃 topic={} -> platform={}", cfg.queueCapacity, msg.topic(), conf.name());
            return EnqueueResult.QUEUE_FULL;
        }
        return EnqueueResult.ENQUEUED;
    }

    private void workLoop() {
        while (running.get()) {
            Delivery d;
            try {
                d = queue.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (d == null) continue;
            deliverWithRetry(d.conf(), d.msg());
        }
    }

    private void deliverWithRetry(PlatformConf conf, Message msg) {
        ChannelSender sender = senders.get(conf.type());
        if (sender == null) {
            deadLetter(conf, msg, "未知平台类型: " + conf.type());
            return;
        }
        TokenBucket bucket = buckets.computeIfAbsent(conf.name(),
                k -> new TokenBucket(conf.rateLimitQps() > 0 ? conf.rateLimitQps() : cfg.defaultQps));

        String lastError = null;
        int attempts = Math.max(1, conf.maxAttempts());
        for (int attempt = 1; attempt <= attempts && running.get(); attempt++) {
            try {
                bucket.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            String rendered = TemplateRenderer.render(conf.template(), msg);
            io.notifyhub.channel.SendResult r = sender.send(conf, msg, rendered);
            if (r.ok()) {
                succeeded.incrementAndGet();
                log.info("投递成功 platform={} topic={} event={} attempt={}/{}",
                        conf.name(), msg.topic(), msg.eventId(), attempt, attempts);
                return;
            }
            lastError = r.error();
            log.warn("投递失败 platform={} topic={} event={} attempt={}/{}: {}",
                    conf.name(), msg.topic(), msg.eventId(), attempt, attempts, lastError);
            if (attempt < attempts && running.get()) {
                sleepBackoff(conf.backoffMs(), attempt);
            }
        }
        failed.incrementAndGet();
        deadLetter(conf, msg, lastError);
    }

    private void sleepBackoff(long baseMs, int attempt) {
        long backoff = Math.min(MAX_BACKOFF_MS, baseMs * (1L << Math.min(attempt - 1, 10)));
        long jitter = (long) (backoff * 0.5 * Math.random());
        try {
            Thread.sleep(backoff + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void deadLetter(PlatformConf conf, Message msg, String error) {
        log.error("死信 platform={} topic={} event={}: {}", conf.name(), msg.topic(), msg.eventId(), error);
        Message dead = new Message(
                msg.eventId(),
                Message.DEADLETTER_TOPIC,
                "[投递失败] " + conf.name(),
                error == null ? "未知错误" : error,
                Map.of("platform", conf.name(),
                        "topic", nullToEmpty(msg.topic()),
                        "event_id", nullToEmpty(msg.eventId()),
                        "error", error == null ? "" : error),
                System.currentTimeMillis());
        subscribers.broadcast(dead);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public long acceptedCount() { return accepted.get(); }
    public long succeededCount() { return succeeded.get(); }
    public long failedCount() { return failed.get(); }
    public int queueDepth() { return queue.size(); }

    @Override
    public void close() {
        running.set(false);
        for (Thread t : workerThreads) t.interrupt();
    }
}

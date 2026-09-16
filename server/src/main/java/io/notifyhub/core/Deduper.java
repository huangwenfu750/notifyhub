package io.notifyhub.core;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** dedup_key 短窗口去重（防告警风暴）。内存实现，重启即清零。 */
public final class Deduper {

    private static final int PURGE_THRESHOLD = 10_000;

    private final long windowMs;
    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();
    private final AtomicLong purged = new AtomicLong();

    public Deduper(long windowMs) {
        this.windowMs = windowMs;
    }

    /** @return true 表示首次出现（放行）；false 表示窗口内重复（拦截） */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Long prev = seen.putIfAbsent(key, now);
        if (prev == null) {
            if (seen.size() > PURGE_THRESHOLD) purgeExpired(now);
            return true;
        }
        if (now - prev > windowMs) {
            return seen.replace(key, prev, now);
        }
        return false;
    }

    public int size() {
        return seen.size();
    }

    public long purgedCount() {
        return purged.get();
    }

    private void purgeExpired(long now) {
        int removed = 0;
        for (Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > windowMs) {
                it.remove();
                removed++;
            }
        }
        purged.addAndGet(removed);
    }
}

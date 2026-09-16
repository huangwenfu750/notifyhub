package io.notifyhub.core;

/** 简单令牌桶，用于平台级 QPS 限流（阻塞获取）。 */
public final class TokenBucket {

    private final double capacity;
    private final double refillPerMs;
    private double tokens;
    private long lastRefill;

    public TokenBucket(int qps) {
        int effective = Math.max(1, qps);
        this.capacity = effective;
        this.refillPerMs = effective / 1000.0;
        this.tokens = capacity;
        this.lastRefill = System.nanoTime();
    }

    public void acquire() throws InterruptedException {
        while (true) {
            long waitMs;
            synchronized (this) {
                refill();
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return;
                }
                waitMs = (long) Math.ceil((1.0 - tokens) / refillPerMs);
            }
            Thread.sleep(Math.min(Math.max(waitMs, 5), 100));
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedMs = (now - lastRefill) / 1_000_000.0;
        if (elapsedMs <= 0) return;
        tokens = Math.min(capacity, tokens + elapsedMs * refillPerMs);
        lastRefill = now;
    }
}

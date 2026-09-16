"""压测脚本：向 NotifyHub 持续发布并统计吞吐。

用法:
  .venv/Scripts/python.exe scripts/bench.py --target 127.0.0.1:9987 --n 10000 --c 8 [--token ntf_xxx]

仅测服务端 gRPC 吞吐（不挂平台或挂较慢的平台时，瓶颈在出站投递——生产上平台侧限流才是上限）。
"""
import argparse
import sys
import threading
import time

sys.path.insert(0, "sdks/python/src")
from notifyhub import NotifyClient  # noqa: E402


def worker(target, token, n, topic, idx, latencies, errors):
    client = NotifyClient(target, token=token or None)
    for i in range(n):
        t0 = time.perf_counter()
        try:
            client.publish(topic, f"bench-{idx}", f"message {i}")
        except Exception as e:  # noqa: BLE001
            errors.append(e)
        latencies.append((time.perf_counter() - t0) * 1000)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--target", default="127.0.0.1:9987")
    ap.add_argument("--token", default="")
    ap.add_argument("--n", type=int, default=10000, help="总请求数")
    ap.add_argument("--c", type=int, default=8, help="并发线程数")
    args = ap.parse_args()

    n_per = args.n // args.c
    latencies, errors = [], []
    threads = [threading.Thread(target=worker,
                                args=(args.target, args.token, n_per, "bench", i, latencies, errors))
               for i in range(args.c)]

    t0 = time.perf_counter()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.perf_counter() - t0

    total = len(latencies)
    lat_sorted = sorted(latencies)
    p50 = lat_sorted[int(total * 0.5)]
    p99 = lat_sorted[min(total - 1, int(total * 0.99))]
    print(f"请求: {total}  并发: {args.c}  耗时: {elapsed:.2f}s")
    print(f"吞吐: {total / elapsed:,.0f} msg/s")
    print(f"延迟: p50={p50:.1f}ms  p99={p99:.1f}ms")
    if errors:
        print(f"错误: {len(errors)} 个，首个: {errors[0]}")


if __name__ == "__main__":
    main()

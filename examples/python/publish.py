"""发布一条通知 + 订阅主题的最小示例。"""
from notifyhub import NotifyClient

with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    # 订阅主题
    sub = client.subscribe(["alert.*"], lambda e: print("收到:", e.topic, e.title))
    import time; time.sleep(0.3)

    # 发布
    ack = client.publish("alert.db", "磁盘告警", "db-01 使用率 95%", params={"host": "db-01"})
    print("accepted:", ack.accepted, "event:", ack.event_id, "platforms:", ack.matched_platforms)

    time.sleep(1)
    sub.cancel()

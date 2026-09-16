"""Python SDK 跨进程烟雾测试。

前置: 服务端已以 smoke/config.yaml 启动(127.0.0.1:9987, token=ntf_smoke_token)。
运行: .venv/Scripts/python.exe smoke/py_smoke.py
"""

import hashlib
import hmac
import json
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

sys.path.insert(0, "sdks/python/src")
from notifyhub import NotifyClient  # noqa: E402
from notifyhub.notify.v1 import notify_pb2 as pb  # noqa: E402

TARGET = "127.0.0.1:9987"
TOKEN = "ntf_smoke_token"

received = []


class Receiver(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        received.append((self.headers.get("X-Signature"), body))
        self.send_response(200)
        self.end_headers()

    def log_message(self, *a):
        pass


def main():
    httpd = HTTPServer(("127.0.0.1", 19800), Receiver)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()

    with NotifyClient(TARGET, token=TOKEN) as client:
        # 1. ping
        pong = client.ping()
        assert pong["version"], pong
        print("[1] ping ok:", pong)

        # 2. 无 token 客户端应被拒
        try:
            NotifyClient(TARGET).ping()
            # ping 免鉴权，预期成功
            print("[2] ping 免鉴权 ok")
        except Exception as e:  # noqa: BLE001
            raise AssertionError(f"ping 不应要求 token: {e}")

        # 3. 订阅 smoke.* 并发布
        got = []

        def on_event(e):
            got.append(e)

        sub = client.subscribe(["smoke.*"], on_event)
        time.sleep(0.5)
        ack = client.publish("smoke.db", "Python 发布成功", "来自 smoke.py",
                             params={"lang": "python", "n": "3"})
        assert ack.accepted and ack.matched_platforms == ["smoke-hook"], ack
        print("[3] publish ok:", ack)

        deadline = time.time() + 5
        while not got and time.time() < deadline:
            time.sleep(0.05)
        assert got and got[0].topic == "smoke.db" and got[0].title == "Python 发布成功", got
        print("[4] subscribe ok:", got[0])
        sub.cancel()

        # 4. dedup（用唯一 key 避免受上一次运行影响）
        import uuid
        dedup_key = f"k-{uuid.uuid4()}"
        ack1 = client.publish("smoke.dedup", "1", "", dedup_key=dedup_key)
        ack2 = client.publish("smoke.dedup", "2", "", dedup_key=dedup_key)
        assert ack1.accepted and not ack2.accepted and ack2.deduplicated, (ack1, ack2)
        print("[5] dedup ok:", ack2)

        # 5. 批量发布（双向流）：中间一条缺 topic，应回执错误但不中断
        reqs = [
            pb.PublishRequest(topic="smoke.batch", title="批量1", content="c1"),
            pb.PublishRequest(title="缺 topic 的非法请求"),
            pb.PublishRequest(topic="smoke.batch", title="批量3", content="c3"),
        ]
        acks = client.publish_batch(reqs)
        assert len(acks) == 3, f"应收到 3 条回执: {acks}"
        assert acks[0].accepted and acks[0].matched_platforms == ["smoke-hook"], acks[0]
        assert not acks[1].accepted and "INVALID_ARGUMENT" in acks[1].error, acks[1]
        assert acks[2].accepted, "第 3 条应成功（流未被打断）"
        print("[6] publish_batch ok:", [a.accepted for a in acks], acks[1].error)

        # 6. webhook 收到带 HMAC 签名的 POST
        deadline = time.time() + 5
        while not received and time.time() < deadline:
            time.sleep(0.05)
        assert received, "webhook 未收到请求"
        found = None
        for sig, body in received:
            expect = hmac.new(b"smoke-secret", body, hashlib.sha256).hexdigest()
            assert sig == expect, f"签名不匹配 {sig} != {expect}"
            payload = json.loads(body)
            if payload["topic"] == "smoke.db":
                found = payload
        assert found, f"未找到 smoke.db 的投递: {received}"
        assert found["params"].get("lang") == "python", found
        print("[7] webhook + 签名 ok:", found["topic"], found["title"])

        # 7. Admin: 运行时注册平台（webhook 指向另一个路径）
        client.upsert_platform(name="py-hook", type="webhook",
                               webhook="http://127.0.0.1:19800/py",
                               topics=["py.*"])
        ack = client.publish("py.event", "运行时注册", "通过 Admin RPC")
        assert "py-hook" in ack.matched_platforms, ack
        deadline = time.time() + 5
        while time.time() < deadline:
            if any(json.loads(b).get("topic") == "py.event" for _, b in received):
                break
            time.sleep(0.05)
        assert any(json.loads(b).get("topic") == "py.event" for _, b in received), \
            "运行时注册的平台未收到请求"
        client.remove_platform("py-hook")
        print("[8] admin ok:", ack.matched_platforms)

    print("\nPython 烟雾测试全部通过 ✓")


if __name__ == "__main__":
    main()

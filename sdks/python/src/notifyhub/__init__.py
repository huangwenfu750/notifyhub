"""NotifyHub Python 客户端（gRPC 薄封装）。

用法::

    from notifyhub import NotifyClient

    client = NotifyClient("localhost:9987", token="xxx")
    client.publish("alert", "部署完成", "v1.2.0 上线")
    client.close()

批量发布（双向流）::

    from notifyhub import NotifyClient, PublishRequest

    reqs = [PublishRequest(topic="a.b", title="t1"),
            PublishRequest(title="非法：缺 topic")]
    for ack in client.publish_batch(reqs):
        print(ack.accepted, ack.error)
"""

from .client import Event, NotifyClient, NotifyError, PublishAck, Subscription
from .notify.v1 import notify_pb2

Options = notify_pb2.Options
PublishRequest = notify_pb2.PublishRequest
PlatformConfig = notify_pb2.PlatformConfig

__all__ = [
    "NotifyClient",
    "NotifyError",
    "PublishAck",
    "Event",
    "Subscription",
    "PublishRequest",
    "Options",
    "PlatformConfig",
]
__version__ = "0.1.1"

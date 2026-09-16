# NotifyHub Python SDK

> 中文版：[README.md](README.md)

```bash
pip install -e sdks/python   # local install (the way to use it before publishing to PyPI)
```

```python
from notifyhub import NotifyClient, PublishRequest

with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "Deployment finished", "v1.2.0 is live", params={"env": "prod"})
    print(client.ping())
```

Batch publish (a single bidi stream; one failure does not break the batch):

```python
reqs = [
    PublishRequest(topic="alert.db", title="t1", content="c1"),
    PublishRequest(title="an illegal request with no topic"),
]
for ack in client.publish_batch(reqs):
    print(ack.accepted, ack.error)      # True '' / False 'INVALID_ARGUMENT: topic 不能为空'
```

Subscribe to topics:

```python
sub = client.subscribe(["alert.*"], print)
...
sub.cancel()
```

Register a push platform from code:

```python
client.upsert_platform(
    name="ding-alert", type="dingtalk",
    webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
    secret="SECxxx", topics=["alert"],
)
```

Regenerate the stubs (after changing the proto):

```bash
./scripts/gen-protos.sh python
```

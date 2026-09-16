# NotifyHub Python SDK

> English: [README.en.md](README.en.md)

```bash
pip install -e sdks/python   # 本地安装（发布到 PyPI 前的用法）
```

```python
from notifyhub import NotifyClient, PublishRequest

with NotifyClient("localhost:9987", token="ntf_xxx") as client:
    client.publish("alert", "部署完成", "v1.2.0 上线", params={"env": "prod"})
    print(client.ping())
```

批量发布（一条双向流，单条失败不中断）：

```python
reqs = [
    PublishRequest(topic="alert.db", title="t1", content="c1"),
    PublishRequest(title="缺 topic 的非法请求"),
]
for ack in client.publish_batch(reqs):
    print(ack.accepted, ack.error)      # True '' / False 'INVALID_ARGUMENT: topic 不能为空'
```

订阅主题：

```python
sub = client.subscribe(["alert.*"], print)
...
sub.cancel()
```

用代码注册推送平台：

```python
client.upsert_platform(
    name="ding-alert", type="dingtalk",
    webhook="https://oapi.dingtalk.com/robot/send?access_token=xxx",
    secret="SECxxx", topics=["alert"],
)
```

重新生成 stub（改了 proto 之后）：

```bash
./scripts/gen-protos.sh python
```

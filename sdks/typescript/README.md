# NotifyHub TypeScript / JavaScript SDK

> English: [README.en.md](README.en.md)

运行时用 `@grpc/proto-loader` 加载 `proto/notify/v1/notify.proto`，**无需代码生成**。

```bash
cd sdks/typescript && npm install && npm run build
```

## 用法

```js
const { NotifyClient } = require("./dist/client.js");

const client = new NotifyClient("localhost:9987", "ntf_xxx");

// 发布
const ack = await client.publish("alert.db", "磁盘告警", "db-01 使用率 95%", {
  params: { host: "db-01" },
  // dedupKey: "db-01-disk",
});
console.log(ack.accepted, ack.eventId, ack.matchedPlatforms);

// 批量发布（双向流，单条失败不中断）
const acks = await client.publishBatch([
  { topic: "alert.db", title: "t1" },
  { title: "缺 topic 的非法请求" },   // -> accepted=false, error="INVALID_ARGUMENT: ..."
]);
console.log(acks.map((a) => a.accepted), acks[1].error);

// 订阅
const sub = client.subscribe(["alert.*"], (e) => console.log("收到:", e.topic, e.title));
...
sub.close();

await client.upsertPlatform({
  name: "ding-alert", type: "dingtalk",
  webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx",
  secret: "SECxxx", topics: ["alert"],
});

client.close();
```

## 能力

| 方法 | 说明 |
|---|---|
| `publish` | 单条发布 |
| `publishBatch(requests, timeoutMs?)` | 双向流批量发布，返回按序回执 |
| `subscribe(topics, cb)` | 订阅，返回句柄（`close()`） |
| `upsertPlatform` / `removePlatform` / `listPlatforms` | 运行时管理推送平台 |
| `ping` | 免鉴权探活 |

> 字段名是 **camelCase**（`eventId` / `matchedPlatforms` / `dedupKey` / `rateLimitQps`），
> 与 Python SDK 的 snake_case 不同，跨语言对接时注意。

## 本地验证

```bash
node smoke/node_smoke.js    # 需先在仓库根启动服务端
```

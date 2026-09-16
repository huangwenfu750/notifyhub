# NotifyHub TypeScript / JavaScript SDK

> 中文版：[README.md](README.md)

At runtime it loads `proto/notify/v1/notify.proto` via `@grpc/proto-loader` — **no code generation needed**.

```bash
cd sdks/typescript && npm install && npm run build
```

## Usage

```js
const { NotifyClient } = require("./dist/client.js");

const client = new NotifyClient("localhost:9987", "ntf_xxx");

// Publish
const ack = await client.publish("alert.db", "Disk alert", "db-01 at 95%", {
  params: { host: "db-01" },
  // dedupKey: "db-01-disk",
});
console.log(ack.accepted, ack.eventId, ack.matchedPlatforms);

// Batch publish (bidi stream, a single failure does not break the batch)
const acks = await client.publishBatch([
  { topic: "alert.db", title: "t1" },
  { title: "an illegal request with no topic" },   // -> accepted=false, error="INVALID_ARGUMENT: ..."
]);
console.log(acks.map((a) => a.accepted), acks[1].error);

// Subscribe
const sub = client.subscribe(["alert.*"], (e) => console.log("received:", e.topic, e.title));
...
sub.close();

await client.upsertPlatform({
  name: "ding-alert", type: "dingtalk",
  webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx",
  secret: "SECxxx", topics: ["alert"],
});

client.close();
```

## API

| Method | Description |
|---|---|
| `publish` | Single publish |
| `publishBatch(requests, timeoutMs?)` | Bidi-stream batch publish, returns acks in order |
| `subscribe(topics, cb)` | Subscribe, returns a handle (`close()`) |
| `upsertPlatform` / `removePlatform` / `listPlatforms` | Runtime platform management |
| `ping` | Auth-free liveness probe |

> Field names are **camelCase** (`eventId` / `matchedPlatforms` / `dedupKey` / `rateLimitQps`),
> unlike the Python SDK's snake_case — watch out when crossing languages.

## Local Verification

```bash
node smoke/node_smoke.js    # start the server from the repo root first
```

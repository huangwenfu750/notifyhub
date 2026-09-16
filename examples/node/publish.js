/** 发布一条通知 + 订阅主题的最小示例。
 *  先在 sdks/typescript 下 npm run build。 */
const { NotifyClient } = require("../../sdks/typescript/dist/client.js");

async function main() {
  const client = new NotifyClient("localhost:9987", "ntf_xxx");

  // 订阅主题
  const sub = client.subscribe(["alert.*"], (e) => console.log("收到:", e.topic, e.title));
  await new Promise((r) => setTimeout(r, 300));

  // 发布
  const ack = await client.publish("alert.db", "磁盘告警", "db-01 使用率 95%", {
    params: { host: "db-01" },
  });
  console.log("accepted:", ack.accepted, "event:", ack.eventId, "platforms:", ack.matchedPlatforms);

  await new Promise((r) => setTimeout(r, 1000));
  sub.close();
  client.close();
}

main().catch(console.error);

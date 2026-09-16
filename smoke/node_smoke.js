/** Node/TS SDK 跨进程烟雾测试。
 *  前置: 服务端运行于 127.0.0.1:9987 (token=ntf_smoke_token)
 *  运行: node smoke/node_smoke.js
 */
const { NotifyClient } = require("../sdks/typescript/dist/client.js");

const TARGET = "127.0.0.1:9987";
const TOKEN = "ntf_smoke_token";

async function main() {
  const client = new NotifyClient(TARGET, TOKEN);

  // 1. ping
  const pong = await client.ping();
  if (!pong.version) throw new Error("ping 失败");
  console.log("[1] ping ok:", pong);

  // 2. 发布 + 订阅
  const received = [];
  const sub = client.subscribe(["smoke.*"], (e) => received.push(e));
  await new Promise((r) => setTimeout(r, 500));
  const ack = await client.publish("smoke.node", "Node 发布成功", "来自 node_smoke.js", {
    params: { lang: "node" },
  });
  if (!ack.accepted || !ack.matchedPlatforms.includes("smoke-hook")) throw new Error("发布路由失败: " + JSON.stringify(ack));
  console.log("[2] publish ok:", ack.eventId);

  const deadline = Date.now() + 5000;
  while (received.length === 0 && Date.now() < deadline) await new Promise((r) => setTimeout(r, 50));
  if (!received.length || received[0].topic !== "smoke.node") throw new Error("订阅未收到事件: " + JSON.stringify(received));
  console.log("[3] subscribe ok:", received[0].title);
  sub.close();

  // 3. dedup
  const key = "node-" + Date.now();
  const a1 = await client.publish("smoke.dedup", "1", "", { dedupKey: key });
  const a2 = await client.publish("smoke.dedup", "2", "", { dedupKey: key });
  if (!a1.accepted || a2.accepted || !a2.deduplicated) throw new Error("去重异常: " + JSON.stringify([a1, a2]));
  console.log("[4] dedup ok");

  // 4. 批量发布（双向流）：中间一条缺 topic，应回执错误但不中断
  const acks = await client.publishBatch([
    { topic: "smoke.batch", title: "批量1", content: "c1" },
    { title: "缺 topic 的非法请求" },
    { topic: "smoke.batch", title: "批量3", content: "c3" },
  ]);
  if (acks.length !== 3) throw new Error("批量回执数量异常: " + JSON.stringify(acks));
  if (!acks[0].accepted || !acks[0].matchedPlatforms.includes("smoke-hook")) throw new Error("第 1 条应成功: " + JSON.stringify(acks[0]));
  if (acks[1].accepted || !String(acks[1].error).includes("INVALID_ARGUMENT")) throw new Error("第 2 条应失败: " + JSON.stringify(acks[1]));
  if (!acks[2].accepted) throw new Error("第 3 条应成功（流未被打断）: " + JSON.stringify(acks[2]));
  console.log("[5] publishBatch ok:", acks.map((a) => a.accepted), acks[1].error);

  // 5. Admin: 运行时注册平台
  await client.upsertPlatform({
    name: "js-hook",
    type: "webhook",
    webhook: "http://127.0.0.1:19800/js",
    topics: ["js.*"],
  });
  const ack2 = await client.publish("js.event", "运行时注册", "通过 JS SDK");
  if (!ack2.matchedPlatforms.includes("js-hook")) throw new Error("运行时平台未生效: " + JSON.stringify(ack2));
  await client.removePlatform("js-hook");
  console.log("[6] admin ok:", ack2.matchedPlatforms);

  // 6. 鉴权: 无 token 客户端
  const anon = new NotifyClient(TARGET);
  await anon.ping(); // ping 免鉴权
  let rejected = false;
  try {
    await anon.publish("x", "y", "z");
  } catch (e) {
    rejected = true;
  }
  if (!rejected) throw new Error("无 token 应被拒绝");
  console.log("[7] auth ok");

  client.close();
  anon.close();
  console.log("\nNode 烟雾测试全部通过 ✓");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

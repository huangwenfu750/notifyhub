# Spring Boot 示例工程

> English: [README.en.md](README.en.md)

最小可运行的 Spring Boot 应用：演示「`application.yml` 配置 → 注入 `NotifyHubTemplate` → 发布 / 收事件 / 去重」。

这是一个**独立的 Gradle 工程**（有自己的 `settings.gradle.kts`），不参与根工程的构建。

## 1. 前置：把 starter 装到本地 Maven 仓库

```bash
gradle publishNotifyHubToMavenLocal
```

## 2. 准备服务端与回显服务

```bash
# 终端 A：NotifyHub 服务端（smoke 配置：端口 9987，token=ntf_smoke_token）
./server/build/install/server/bin/server --config smoke/config.yaml

# 终端 B：本地回显服务，接收投递过来的 webhook
python echo_server.py
```

## 3. 跑示例

```bash
cd examples/spring-boot
gradle bootRun
```

启动日志：

```
NotifyHub 平台已注册: name=demo-hook type=webhook topics=[demo.*]
Tomcat started on port 8080 (http)
Started NotifyHubExampleApplication in 2.289 seconds
NotifyHub 已订阅: topics=[demo.*]
ping -> 0.1.1
已注册平台 -> [demo-hook, smoke-hook]
```

## 4. 调接口

```bash
# 发一条通知
curl -X POST localhost:8080/api/notify -H "Content-Type: application/json" \
  -d '{"topic":"demo.deploy","title":"部署完成","content":"v1.2.0 上线","params":{"env":"prod"}}'
# -> {"eventId":"...","matchedPlatforms":["demo-hook"],"deduplicated":false,"accepted":true}

# 带 dedup_key 连发两次：第二次被去重
curl -X POST localhost:8080/api/notify -H "Content-Type: application/json" \
  -d '{"topic":"demo.alert","title":"磁盘告警","content":"db-01 使用率 95%","dedupKey":"db-01-disk"}'
# -> 第一次 accepted=true；第二次 accepted=false deduplicated=true error=窗口期内重复的 dedup_key

# 批量（一条双向流发多条；中间故意缺 topic，验证单条错误不中断流）
curl -X POST localhost:8080/api/notify/batch -H "Content-Type: application/json" \
  -d '{"items":[{"topic":"demo.b1","title":"批量1","content":"c1"},{"title":"缺 topic","content":"c2"},{"topic":"demo.b3","title":"批量3","content":"c3"}]}'
# -> 第 2 条 accepted=false error=INVALID_ARGUMENT: topic 不能为空，第 3 条照常成功

curl localhost:8080/api/platforms    # -> ["demo-hook","smoke-hook"]
curl localhost:8080/api/ping         # -> {"uptimeSeconds":43,"version":"0.1.1"}
```

## 5. 健康检查与指标

示例工程引了 `spring-boot-starter-actuator`，starter 会自动挂上健康检查与 Micrometer 埋点：

```bash
curl localhost:8080/actuator/health
# -> ... "notifyHub":{"status":"UP","details":{"version":"0.1.1","uptimeSeconds":47}} ...

curl localhost:8080/actuator/metrics/notifyhub.publish.total
# -> COUNT=3.0, tags: topic=[demo.alert, demo.deploy], outcome=[accepted, deduplicated]

curl localhost:8080/actuator/metrics/notifyhub.publish.duration
# -> COUNT=3.0, TOTAL_TIME=0.029s, MAX=0.0157s

# 批量走独立指标（不计入 publish.total）
curl localhost:8080/actuator/metrics/notifyhub.publish.batch.duration   # -> COUNT=1.0, TOTAL_TIME≈0.01s
curl localhost:8080/actuator/metrics/notifyhub.publish.batch.size       # -> COUNT=1.0, TOTAL=2.0（每批条数）
```

上面这组数字对应的正是「deploy 1 次 accepted + alert 1 次 accepted + alert 重复 1 次被去重」。

应用日志里会同步出现 `收到事件 -> topic=demo.deploy ...`（starter 把事件广播成了 Spring `ApplicationEvent`），
回显服务窗口打印投递过去的 JSON。

## 6. 换成真实环境

改 `src/main/resources/application.yml`：

```yaml
notifyhub:
  host: notifyhub.internal
  port: 9987
  token: ${NOTIFYHUB_TOKEN}      # 建议走环境变量
  platforms:
    - name: ding-alert
      type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      secret: SECxxx
      topics: [alert, ops.*]
```

生产环境更推荐把平台写进**服务端 YAML**（持久化），应用侧只保留 host/port/token。

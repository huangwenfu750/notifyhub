# Spring Boot Sample Project

> 中文版：[README.md](README.md)

A minimal runnable Spring Boot application: demonstrates "`application.yml` config → inject
`NotifyHubTemplate` → publish / receive events / deduplicate".

This is a **standalone Gradle project** (with its own `settings.gradle.kts`) and is not part of the
root build.

## 1. Prerequisite: install the starter into the local Maven repository

```bash
gradle publishNotifyHubToMavenLocal
```

## 2. Start the server and the echo service

```bash
# Terminal A: NotifyHub server (smoke config: port 9987, token=ntf_smoke_token)
./server/build/install/server/bin/server --config smoke/config.yaml

# Terminal B: local echo service that receives the delivered webhook
python echo_server.py
```

## 3. Run the sample

```bash
cd examples/spring-boot
gradle bootRun
```

Startup log:

```
NotifyHub 平台已注册: name=demo-hook type=webhook topics=[demo.*]
Tomcat started on port 8080 (http)
Started NotifyHubExampleApplication in 2.289 seconds
NotifyHub 已订阅: topics=[demo.*]
ping -> 0.1.0
已注册平台 -> [demo-hook, smoke-hook]
```

## 4. Call the Endpoints

```bash
# Publish a notification
curl -X POST localhost:8080/api/notify -H "Content-Type: application/json" \
  -d '{"topic":"demo.deploy","title":"Deployment finished","content":"v1.2.0 is live","params":{"env":"prod"}}'
# -> {"eventId":"...","matchedPlatforms":["demo-hook"],"deduplicated":false,"accepted":true}

# Send twice with the same dedup_key: the second one is deduplicated
curl -X POST localhost:8080/api/notify -H "Content-Type: application/json" \
  -d '{"topic":"demo.alert","title":"Disk alert","content":"db-01 at 95%","dedupKey":"db-01-disk"}'
# -> first: accepted=true; second: accepted=false deduplicated=true error=窗口期内重复的 dedup_key

# Batch (multiple messages over one bidi stream; one item deliberately omits the topic to show that a single error does not break the stream)
curl -X POST localhost:8080/api/notify/batch -H "Content-Type: application/json" \
  -d '{"items":[{"topic":"demo.b1","title":"batch1","content":"c1"},{"title":"no topic","content":"c2"},{"topic":"demo.b3","title":"batch3","content":"c3"}]}'
# -> item 2: accepted=false error=INVALID_ARGUMENT: topic 不能为空; item 3 still succeeds

curl localhost:8080/api/platforms    # -> ["demo-hook","smoke-hook"]
curl localhost:8080/api/ping         # -> {"uptimeSeconds":43,"version":"0.1.0"}
```

## 5. Health Check and Metrics

The sample depends on `spring-boot-starter-actuator`, so the starter automatically wires up the
health check and Micrometer instrumentation:

```bash
curl localhost:8080/actuator/health
# -> ... "notifyHub":{"status":"UP","details":{"version":"0.1.0","uptimeSeconds":47}} ...

curl localhost:8080/actuator/metrics/notifyhub.publish.total
# -> COUNT=3.0, tags: topic=[demo.alert, demo.deploy], outcome=[accepted, deduplicated]

curl localhost:8080/actuator/metrics/notifyhub.publish.duration
# -> COUNT=3.0, TOTAL_TIME=0.029s, MAX=0.0157s

# Batches use separate metrics (not counted in publish.total)
curl localhost:8080/actuator/metrics/notifyhub.publish.batch.duration   # -> COUNT=1.0, TOTAL_TIME≈0.01s
curl localhost:8080/actuator/metrics/notifyhub.publish.batch.size       # -> COUNT=1.0, TOTAL=2.0 (items per batch)
```

Those numbers correspond exactly to "1 accepted deploy + 1 accepted alert + 1 deduplicated alert".

The application log also shows `收到事件 -> topic=demo.deploy ...` (the starter broadcasts events as
Spring `ApplicationEvent`s), and the echo service window prints the delivered JSON.

## 6. Switching to a Real Environment

Edit `src/main/resources/application.yml`:

```yaml
notifyhub:
  host: notifyhub.internal
  port: 9987
  token: ${NOTIFYHUB_TOKEN}      # environment variable recommended
  platforms:
    - name: ding-alert
      type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send?access_token=xxx
      secret: SECxxx
      topics: [alert, ops.*]
```

For production it is better to keep platforms in the **server YAML** (persisted) and leave only
host/port/token on the application side.

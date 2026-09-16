# 示例

> English: [README.en.md](README.en.md)

每个语言的完整可运行示例：

| 语言 | 位置 | 运行方式 |
|---|---|---|
| Java | `../sdks/java/src/test/java/io/notifyhub/sdk/SmokeMain.java` | `gradle :sdk-java:smoke -Ptoken=ntf_xxx` |
| Python | `python/publish.py` | 先 `pip install -e ../sdks/python`，再 `python publish.py` |
| Node/TS | `node/publish.js` | 先 `cd ../sdks/typescript && npm run build`，再 `node publish.js` |
| Go | `go/publish.go` | 先 `./scripts/gen-protos.sh go`，再 `cd go && go mod tidy && go run .` |
| Spring Boot | `spring-boot/` | 独立 Gradle 工程；先 `gradle publishNotifyHubToMavenLocal`，再 `cd spring-boot && gradle bootRun` |

以下示例假设服务端运行于 `localhost:9987`、token 为 `ntf_xxx`。

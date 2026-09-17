# Examples

> 中文版：[README.md](README.md)

Complete runnable examples per language:

| Language | Location | How to run |
|---|---|---|
| Java | `../sdks/java/src/test/java/io/notifyhub/sdk/SmokeMain.java` | `gradle :sdk-java:smoke -Ptoken=ntf_xxx` |
| Python | `python/publish.py` | `pip install -e ../sdks/python` first, then `python publish.py` |
| Node/TS | `node/publish.js` | `cd ../sdks/typescript && npm run build` first, then `node publish.js` |
| Go | `go/publish.go` | `cd go && go mod tidy && go run .` |
| Spring Boot | `spring-boot/` | Standalone Gradle project; `gradle publishNotifyHubToMavenLocal` first, then `cd spring-boot && gradle bootRun` |

The examples assume the server is running at `localhost:9987` with the token `ntf_xxx`.

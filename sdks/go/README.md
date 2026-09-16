# NotifyHub Go SDK

> English: [README.en.md](README.en.md)

> `gen/` 下的 gRPC stub 不入库，首次使用需生成（需要 Go 工具链）：
>
> ```bash
> ./scripts/gen-protos.sh go                    # 生成 gen/notify/v1/*.go
> cd sdks/go && go mod tidy && go build ./...   # 校验编译
> cd ../../smoke/go && go run .                 # 对真实服务端跑 8 步烟雾测试
> ```
>
> 插件走 `go install`；若 `proxy.golang.org` 不可达，脚本会自动改用 `goproxy.cn`。

## 用法

```go
package main

import (
	"context"
	"fmt"
	"time"

	notifyhub "github.com/notifyhub/notifyhub-sdk-go"
	v1 "github.com/notifyhub/notifyhub-sdk-go/gen/notify/v1"
)

func main() {
	client, err := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
	if err != nil {
		panic(err)
	}
	defer client.Close()

	ctx := context.Background()

	// 发布
	ack, err := client.Publish(ctx, "alert", "部署完成", "v1.2.0 上线")
	if err != nil {
		panic(err)
	}
	fmt.Println("accepted:", ack.Accepted, "event:", ack.EventId)

	// 批量发布（双向流，单条失败不中断）
	acks, err := client.PublishBatch(ctx, []*v1.PublishRequest{
		{Topic: "alert.db", Title: "t1"},
		{Title: "缺 topic 的非法请求"},   // -> Accepted=false, Error="INVALID_ARGUMENT: ..."
	})
	if err != nil {
		panic(err)
	}
	for _, a := range acks {
		fmt.Println("accepted:", a.Accepted, "error:", a.Error)
	}

	// 订阅主题
	sub, err := client.Subscribe(ctx, []string{"alert.*"})
	if err != nil {
		panic(err)
	}
	defer sub.Close()
	go func() {
		for ev := range sub.Events() {
			fmt.Println("收到:", ev.Topic, ev.Title)
		}
	}()

	// 代码配置推送平台
	_, _ = client.UpsertPlatform(ctx, &v1.PlatformConfig{
		Name:    "ding-alert",
		Type:    "dingtalk",
		Webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx",
		Secret:  "SECxxx",
		Topics:  []string{"alert"},
	})

	time.Sleep(time.Second)
}
```

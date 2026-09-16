# NotifyHub Go SDK

> 中文版：[README.md](README.md)

> The gRPC stubs under `gen/` are not committed; generate them before first use (Go toolchain required):
>
> ```bash
> ./scripts/gen-protos.sh go                    # generates gen/notify/v1/*.go
> cd sdks/go && go mod tidy && go build ./...   # verify it compiles
> cd ../../smoke/go && go run .                 # 8-step smoke test against a real server
> ```
>
> Plugins are installed via `go install`; if `proxy.golang.org` is unreachable, the script
> automatically switches to `goproxy.cn`.

## Usage

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

	// Publish
	ack, err := client.Publish(ctx, "alert", "Deployment finished", "v1.2.0 is live")
	if err != nil {
		panic(err)
	}
	fmt.Println("accepted:", ack.Accepted, "event:", ack.EventId)

	// Batch publish (bidi stream, a single failure does not break the batch)
	acks, err := client.PublishBatch(ctx, []*v1.PublishRequest{
		{Topic: "alert.db", Title: "t1"},
		{Title: "an illegal request with no topic"},   // -> Accepted=false, Error="INVALID_ARGUMENT: ..."
	})
	if err != nil {
		panic(err)
	}
	for _, a := range acks {
		fmt.Println("accepted:", a.Accepted, "error:", a.Error)
	}

	// Subscribe to topics
	sub, err := client.Subscribe(ctx, []string{"alert.*"})
	if err != nil {
		panic(err)
	}
	defer sub.Close()
	go func() {
		for ev := range sub.Events() {
			fmt.Println("received:", ev.Topic, ev.Title)
		}
	}()

	// Configure a push platform from code
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

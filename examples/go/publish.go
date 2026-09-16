// 发布一条通知 + 批量发布 + 订阅主题的最小示例。
//
// 前置：
//   ./scripts/gen-protos.sh go            # 生成 sdks/go/gen
//
// 运行：
//   cd examples/go && go mod tidy && go run .
package main

import (
	"context"
	"fmt"
	"log"
	"time"

	notifyhub "github.com/huangwenfu750/notifyhub/sdks/go"
	v1 "github.com/huangwenfu750/notifyhub/sdks/go/gen/notify/v1"
)

func main() {
	client, err := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
	if err != nil {
		log.Fatal(err)
	}
	defer client.Close()

	ctx := context.Background()

	// 订阅主题
	sub, err := client.Subscribe(ctx, []string{"alert.*"}, notifyhub.WithToken("ntf_xxx"))
	if err != nil {
		log.Fatal(err)
	}
	go func() {
		for ev := range sub.Events() {
			fmt.Println("收到:", ev.Topic, ev.Title)
		}
	}()
	time.Sleep(300 * time.Millisecond) // 等订阅流建立
	defer sub.Close()

	// 发布一条
	ack, err := client.Publish(ctx, "alert.db", "磁盘告警", "db-01 使用率 95%")
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("accepted: %v event: %s platforms: %v\n",
		ack.Accepted, ack.EventId, ack.MatchedPlatforms)

	// 批量发布（一条双向流；单条失败不中断，只体现在对应回执上）
	acks, err := client.PublishBatch(ctx, []*v1.PublishRequest{
		{Topic: "alert.db", Title: "批量1", Content: "c1"},
		{Title: "缺 topic 的非法请求"},
		{Topic: "alert.db", Title: "批量3", Content: "c3"},
	})
	if err != nil {
		log.Fatal(err)
	}
	for i, a := range acks {
		fmt.Printf("batch[%d] accepted=%v error=%q\n", i, a.Accepted, a.Error)
	}

	time.Sleep(time.Second)
}

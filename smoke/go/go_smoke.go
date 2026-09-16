// Go SDK 跨进程烟雾测试。
//
// 前置: 服务端运行于 127.0.0.1:9987 (token=ntf_smoke_token)
// 运行: cd smoke/go && go run .
package main

import (
	"context"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	notifyhub "github.com/notifyhub/notifyhub-sdk-go"
	v1 "github.com/notifyhub/notifyhub-sdk-go/gen/notify/v1"
)

const (
	target = "127.0.0.1:9987"
	token  = "ntf_smoke_token"
)

var (
	mu        sync.Mutex
	delivered []string // 收到的投递请求体
)

// startReceiver 起一个本地 HTTP 服务接收平台投递。
func startReceiver(addr string) *http.Server {
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		mu.Lock()
		delivered = append(delivered, string(b))
		mu.Unlock()
		w.WriteHeader(http.StatusOK)
	})
	srv := &http.Server{Addr: addr, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	go func() { _ = srv.ListenAndServe() }()
	return srv
}

func waitDelivered(substr string, d time.Duration) bool {
	deadline := time.Now().Add(d)
	for time.Now().Before(deadline) {
		mu.Lock()
		hit := false
		for _, b := range delivered {
			if strings.Contains(b, substr) {
				hit = true
				break
			}
		}
		mu.Unlock()
		if hit {
			return true
		}
		time.Sleep(50 * time.Millisecond)
	}
	return false
}

func must(cond bool, msg string) {
	if !cond {
		log.Fatalf("断言失败: %s", msg)
	}
}

func main() {
	srv := startReceiver("127.0.0.1:19800")
	defer srv.Close()
	ctx := context.Background()

	client, err := notifyhub.New(target, notifyhub.WithToken(token))
	must(err == nil, fmt.Sprintf("连接失败: %v", err))
	defer client.Close()

	// 1. ping（免鉴权）
	pong, err := client.Ping(ctx)
	must(err == nil, fmt.Sprintf("ping 失败: %v", err))
	must(pong.GetVersion() != "", "ping 返回空 version")
	fmt.Printf("[1] ping ok: version=%s uptime=%ds\n", pong.GetVersion(), pong.GetUptimeSeconds())

	// 2. 订阅 + 发布
	sub, err := client.Subscribe(ctx, []string{"smoke.*"}, notifyhub.WithToken(token))
	must(err == nil, fmt.Sprintf("订阅失败: %v", err))
	time.Sleep(400 * time.Millisecond)

	ack, err := client.PublishMsg(ctx, &v1.PublishRequest{
		Topic:   "smoke.go",
		Title:   "Go 发布成功",
		Content: "来自 go_smoke.go",
		Params:  map[string]string{"lang": "go"},
	})
	must(err == nil, fmt.Sprintf("发布失败: %v", err))
	must(ack.GetAccepted(), "发布未被接受")
	must(len(ack.GetMatchedPlatforms()) == 1 && ack.GetMatchedPlatforms()[0] == "smoke-hook",
		fmt.Sprintf("路由异常: %v", ack.GetMatchedPlatforms()))
	fmt.Printf("[2] publish ok: eventId=%s matched=%v\n", ack.GetEventId(), ack.GetMatchedPlatforms())

	select {
	case ev := <-sub.Events():
		must(ev.GetTopic() == "smoke.go", fmt.Sprintf("事件 topic 不符: %s", ev.GetTopic()))
		fmt.Printf("[3] subscribe ok: topic=%s title=%s\n", ev.GetTopic(), ev.GetTitle())
	case <-time.After(5 * time.Second):
		log.Fatal("断言失败: 订阅超时未收到事件")
	}
	sub.Close()

	// 4. 平台真的收到了投递
	must(waitDelivered("smoke.go", 5*time.Second), "webhook 未收到投递")
	fmt.Println("[4] webhook ok: 平台收到投递")

	// 5. dedup
	key := fmt.Sprintf("go-%d", time.Now().UnixNano())
	dedup := &v1.Options{DedupKey: key}
	a1, err := client.PublishMsg(ctx, &v1.PublishRequest{Topic: "smoke.dedup", Title: "1", Options: dedup})
	must(err == nil, fmt.Sprintf("dedup 首次失败: %v", err))
	a2, err := client.PublishMsg(ctx, &v1.PublishRequest{Topic: "smoke.dedup", Title: "2", Options: dedup})
	must(err == nil, fmt.Sprintf("dedup 第二次失败: %v", err))
	must(a1.GetAccepted() && !a2.GetAccepted() && a2.GetDeduplicated(), "去重语义异常")
	fmt.Printf("[5] dedup ok: second accepted=%v deduplicated=%v\n", a2.GetAccepted(), a2.GetDeduplicated())

	// 6. 批量发布（中间一条缺 topic，应回执错误但不中断流）
	batch := []*v1.PublishRequest{
		{Topic: "smoke.batch", Title: "批量1", Content: "c1"},
		{Title: "缺 topic 的非法请求"},
		{Topic: "smoke.batch", Title: "批量3", Content: "c3"},
	}
	acks, err := client.PublishBatch(ctx, batch)
	must(err == nil, fmt.Sprintf("批量发布失败: %v", err))
	must(len(acks) == 3, fmt.Sprintf("批量回执数量应为 3，实际 %d", len(acks)))
	must(acks[0].GetAccepted(), "第 1 条应成功")
	must(!acks[1].GetAccepted() && strings.Contains(acks[1].GetError(), "INVALID_ARGUMENT"),
		fmt.Sprintf("第 2 条应失败: %q", acks[1].GetError()))
	must(acks[2].GetAccepted(), "第 3 条应成功（流未被打断）")
	must(waitDelivered("smoke.batch", 5*time.Second), "批量投递未到达平台")
	fmt.Printf("[6] publishBatch ok: %v err=%s\n",
		[]bool{acks[0].GetAccepted(), acks[1].GetAccepted(), acks[2].GetAccepted()}, acks[1].GetError())

	// 7. Admin: 运行时注册平台
	_, err = client.UpsertPlatform(ctx, &v1.PlatformConfig{
		Name:    "go-hook",
		Type:    "webhook",
		Webhook: "http://127.0.0.1:19800/go",
		Topics:  []string{"go.*"},
	})
	must(err == nil, fmt.Sprintf("注册平台失败: %v", err))
	ack2, err := client.Publish(ctx, "go.event", "运行时注册", "通过 Go SDK")
	must(err == nil, fmt.Sprintf("发布到运行时平台失败: %v", err))
	must(len(ack2.GetMatchedPlatforms()) == 1 && ack2.GetMatchedPlatforms()[0] == "go-hook",
		fmt.Sprintf("运行时平台未生效: %v", ack2.GetMatchedPlatforms()))
	must(waitDelivered("go.event", 5*time.Second), "运行时注册的平台未收到投递")
	must(client.RemovePlatform(ctx, "go-hook") == nil, "移除平台失败")
	fmt.Printf("[7] admin ok: matched=%v\n", ack2.GetMatchedPlatforms())

	// 8. 鉴权: 无 token 客户端的发布应被拒（ping 仍免鉴权）
	anon, err := notifyhub.New(target)
	must(err == nil, fmt.Sprintf("创建匿名客户端失败: %v", err))
	defer anon.Close()
	_, err = anon.Publish(ctx, "x", "y", "z")
	must(err != nil, "无 token 的发布应被拒绝")
	fmt.Printf("[8] auth ok: 无 token 被拒 (%v)\n", err)

	fmt.Println("\nGo 烟雾测试全部通过 ✓")
	os.Exit(0)
}

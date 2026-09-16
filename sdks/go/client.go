// Package notifyhub 是 NotifyHub 通知推送服务的 Go 客户端（gRPC 薄封装）。
//
// 使用前先在仓库根目录执行 ./scripts/gen-protos.sh go 生成 gen/ 下的 stub。
//
//	client, err := notifyhub.New("localhost:9987", notifyhub.WithToken("ntf_xxx"))
//	if err != nil { panic(err) }
//	defer client.Close()
//	ack, err := client.Publish(ctx, "alert", "部署完成", "v1.2.0 上线")
package notifyhub

import (
	"context"
	"fmt"
	"io"
	"time"

	v1 "github.com/huangwenfu750/notifyhub/sdks/go/gen/notify/v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/metadata"
)

// Client 与 NotifyHub 服务端的连接。
type Client struct {
	conn  *grpc.ClientConn
	stub  v1.NotifyClient
	token string
}

// Option 客户端选项。
type Option func(*clientOptions)

type clientOptions struct {
	token string
}

// WithToken 设置 API token（服务端 auth.tokens 中的一项）。
func WithToken(token string) Option {
	return func(o *clientOptions) { o.token = token }
}

// New 建立与 target（如 "localhost:9987"）的连接。
func New(target string, opts ...Option) (*Client, error) {
	o := &clientOptions{}
	for _, f := range opts {
		f(o)
	}
	conn, err := grpc.NewClient(target, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, fmt.Errorf("notifyhub: 连接失败: %w", err)
	}
	return &Client{conn: conn, stub: v1.NewNotifyClient(conn), token: o.token}, nil
}

// withToken 把 token 注入每次调用的 metadata。
func (c *Client) withToken(ctx context.Context) context.Context {
	if c.token == "" {
		return ctx
	}
	return metadata.AppendToOutgoingContext(ctx, "x-api-token", c.token)
}

// TimeoutCtx 返回带超时的 ctx（默认 10s）。
func TimeoutCtx(parent context.Context, d ...time.Duration) (context.Context, context.CancelFunc) {
	if len(d) > 0 {
		return context.WithTimeout(parent, d[0])
	}
	return context.WithTimeout(parent, 10*time.Second)
}

// ---------- 发布 ----------

// Publish 发布一条通知（快捷方法）。
func (c *Client) Publish(ctx context.Context, topic, title, content string) (*v1.PublishAck, error) {
	return c.PublishMsg(ctx, &v1.PublishRequest{
		Topic:   topic,
		Title:   title,
		Content: content,
	})
}

// PublishMsg 发布完整消息（含 params、显式平台、dedup_key 等）。
func (c *Client) PublishMsg(ctx context.Context, req *v1.PublishRequest) (*v1.PublishAck, error) {
	ctx, cancel := TimeoutCtx(ctx)
	defer cancel()
	return c.stub.Publish(c.withToken(ctx), req)
}

// PublishBatch 通过一条双向流批量发布，返回按序的回执。
//
// 单条失败（topic 为空、平台不存在）不会中断流，对应回执的 Accepted=false 且带 Error。
func (c *Client) PublishBatch(ctx context.Context, reqs []*v1.PublishRequest) ([]*v1.PublishAck, error) {
	ctx, cancel := TimeoutCtx(ctx)
	defer cancel()

	stream, err := c.stub.PublishStream(c.withToken(ctx))
	if err != nil {
		return nil, fmt.Errorf("notifyhub: 建立批量流失败: %w", err)
	}
	for _, r := range reqs {
		if err := stream.Send(r); err != nil {
			// 发送阶段的错误：尽力拿回已产生的回执
			acks, _ := drainAcks(stream) //nolint:errcheck
			return acks, fmt.Errorf("notifyhub: 批量发送失败: %w", err)
		}
	}
	if err := stream.CloseSend(); err != nil {
		return nil, fmt.Errorf("notifyhub: 批量流关闭失败: %w", err)
	}
	acks, err := drainAcks(stream)
	if err != nil {
		return acks, fmt.Errorf("notifyhub: 批量接收回执失败: %w", err)
	}
	return acks, nil
}

// drainAcks 读到 EOF 为止。
func drainAcks(stream v1.Notify_PublishStreamClient) ([]*v1.PublishAck, error) {
	var acks []*v1.PublishAck
	for {
		ack, err := stream.Recv()
		if err == io.EOF {
			return acks, nil
		}
		if err != nil {
			return acks, err
		}
		acks = append(acks, ack)
	}
}

// ---------- 订阅 ----------

// Subscription 订阅句柄。
type Subscription struct {
	events <-chan *v1.Event
	cancel context.CancelFunc
	done   chan struct{}
}

// Events 返回事件通道（流关闭后关闭）。
func (s *Subscription) Events() <-chan *v1.Event { return s.events }

// Close 主动退订。
func (s *Subscription) Close() { s.cancel() }

// Done 流结束信号。
func (s *Subscription) Done() <-chan struct{} { return s.done }

// Subscribe 订阅主题（支持通配符 "alert.*"、"logs.#"）。必须显式 Close。
func (c *Client) Subscribe(ctx context.Context, topics []string, opts ...Option) (*Subscription, error) {
	o := &clientOptions{token: c.token}
	for _, f := range opts {
		f(o)
	}
	cctx, cancel := context.WithCancel(c.withToken(ctx))
	stream, err := c.stub.Subscribe(cctx, &v1.SubscribeRequest{Topics: topics})
	if err != nil {
		cancel()
		return nil, fmt.Errorf("notifyhub: 订阅失败: %w", err)
	}
	ch := make(chan *v1.Event, 256)
	done := make(chan struct{})
	go func() {
		defer close(ch)
		defer close(done)
		for {
			ev, err := stream.Recv()
			if err != nil {
				return // 取消或服务端关闭
			}
			ch <- ev
		}
	}()
	return &Subscription{events: ch, cancel: cancel, done: done}, nil
}

// ---------- Admin（代码配置推送平台） ----------

// UpsertPlatform 注册/更新推送平台（运行时生效，重启后需重新注册或写入配置文件）。
func (c *Client) UpsertPlatform(ctx context.Context, cfg *v1.PlatformConfig) (*v1.PlatformConfig, error) {
	ctx, cancel := TimeoutCtx(ctx)
	defer cancel()
	return c.stub.UpsertPlatform(c.withToken(ctx), cfg)
}

// ListPlatforms 列出当前全部平台。
func (c *Client) ListPlatforms(ctx context.Context) (*v1.PlatformList, error) {
	ctx, cancel := TimeoutCtx(ctx)
	defer cancel()
	return c.stub.ListPlatforms(c.withToken(ctx), &v1.Empty{})
}

// RemovePlatform 移除平台。
func (c *Client) RemovePlatform(ctx context.Context, name string) error {
	ctx, cancel := TimeoutCtx(ctx)
	defer cancel()
	_, err := c.stub.RemovePlatform(c.withToken(ctx), &v1.PlatformRef{Name: name})
	return err
}

// Ping 健康检查（免鉴权）。
func (c *Client) Ping(ctx context.Context) (*v1.Pong, error) {
	ctx, cancel := TimeoutCtx(ctx, 5*time.Second)
	defer cancel()
	return c.stub.Ping(c.withToken(ctx), &v1.Empty{})
}

// Close 关闭连接。
func (c *Client) Close() error { return c.conn.Close() }

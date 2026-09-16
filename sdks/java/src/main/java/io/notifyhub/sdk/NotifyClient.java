package io.notifyhub.sdk;

import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.notifyhub.v1.Empty;
import io.notifyhub.v1.Event;
import io.notifyhub.v1.NotifyGrpc;
import io.notifyhub.v1.PlatformConfig;
import io.notifyhub.v1.PlatformList;
import io.notifyhub.v1.PlatformRef;
import io.notifyhub.v1.Pong;
import io.notifyhub.v1.PublishAck;
import io.notifyhub.v1.PublishRequest;
import io.notifyhub.v1.SubscribeRequest;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * NotifyHub Java 客户端（gRPC 薄封装）。
 *
 * <pre>{@code
 * try (NotifyClient client = NotifyClient.newBuilder("localhost", 9987).token("xxx").build()) {
 *     client.publish("alert", "部署完成", "v1.2.0 上线");
 * }
 * }</pre>
 */
public final class NotifyClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final NotifyGrpc.NotifyBlockingStub blocking;
    private final NotifyGrpc.NotifyStub async;

    private NotifyClient(ManagedChannel channel, NotifyGrpc.NotifyBlockingStub blocking, NotifyGrpc.NotifyStub async) {
        this.channel = channel;
        this.blocking = blocking;
        this.async = async;
    }

    // ---------- 快捷发布 ----------

    public PublishAck publish(String topic, String title, String content) {
        return publish(PublishRequest.newBuilder().setTopic(topic).setTitle(title).setContent(content).build());
    }

    public PublishAck publish(PublishRequest request) {
        return blocking.publish(request);
    }

    public CompletableFuture<PublishAck> publishAsync(PublishRequest request) {
        CompletableFuture<PublishAck> future = new CompletableFuture<>();
        async.publish(request, new StreamObserver<>() {
            @Override public void onNext(PublishAck value) { future.complete(value); }
            @Override public void onError(Throwable t) { future.completeExceptionally(t); }
            @Override public void onCompleted() { }
        });
        return future;
    }

    /**
     * 批量发布：与服务端建立一条双向流，逐条发送、逐条回执。
     *
     * <p>相比循环调用 {@link #publish(PublishRequest)}，省掉每条请求的 RPC 开销；
     * 单条错误（topic 为空、平台不存在）不会中断流，而是回执 {@code accepted=false, error=...}。</p>
     *
     * <pre>{@code
     * var session = client.publishStream(ack -> log.info("{}", ack.getEventId()));
     * session.send(req1);
     * session.send(req2);
     * session.complete();
     * session.await(10, TimeUnit.SECONDS);
     * }</pre>
     */
    public PublishStreamSession publishStream(Consumer<PublishAck> onAck) {
        return publishStream(onAck, null);
    }

    public PublishStreamSession publishStream(Consumer<PublishAck> onAck, Consumer<Throwable> onError) {
        return new PublishStreamSession(async, onAck, onError);
    }

    /**
     * 订阅主题。回调在 gRPC 线程触发，请勿阻塞。
     */
    public SubscriptionHandle subscribe(Collection<String> topics, Consumer<Event> onEvent) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        java.util.concurrent.atomic.AtomicReference<ClientCallStreamObserver<?>> callRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        io.grpc.stub.ClientResponseObserver<SubscribeRequest, Event> response =
                new io.grpc.stub.ClientResponseObserver<>() {
                    @Override
                    public void beforeStart(ClientCallStreamObserver<SubscribeRequest> requestStream) {
                        callRef.set(requestStream);
                    }

                    @Override public void onNext(Event value) { onEvent.accept(value); }

                    @Override
                    public void onError(Throwable t) {
                        if (t instanceof StatusRuntimeException sre
                                && sre.getStatus().getCode() == Status.Code.CANCELLED) {
                            done.complete(null);
                        } else {
                            done.completeExceptionally(t);
                        }
                    }

                    @Override public void onCompleted() { done.complete(null); }
                };
        async.subscribe(SubscribeRequest.newBuilder().addAllTopics(topics).build(), response);
        return new SubscriptionHandle(done, callRef);
    }

    // ---------- Admin（代码配置推送平台） ----------

    public PlatformConfig upsertPlatform(PlatformConfig config) {
        return blocking.upsertPlatform(config);
    }

    public PlatformList listPlatforms() {
        return blocking.listPlatforms(Empty.newBuilder().build());
    }

    public void removePlatform(String name) {
        blocking.removePlatform(PlatformRef.newBuilder().setName(name).build());
    }

    public Pong ping() {
        return blocking.ping(Empty.newBuilder().build());
    }

    @Override
    public void close() {
        channel.shutdown();
    }

    public boolean awaitClose(long timeout, TimeUnit unit) throws InterruptedException {
        return channel.awaitTermination(timeout, unit);
    }

    // ---------- Builder ----------

    public static Builder newBuilder(String host, int port) {
        return new Builder(host + ":" + port);
    }

    public static Builder newBuilder(String target) {
        return new Builder(target);
    }

    public static final class Builder {
        private final String target;
        private String token;
        private boolean plaintext = true;

        private Builder(String target) {
            this.target = target;
        }

        /** API token（服务端配置了 auth.tokens 时必填）。 */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /** @param usePlaintext true=不使用 TLS（默认），false=TLS */
        public Builder usePlaintext(boolean usePlaintext) {
            this.plaintext = usePlaintext;
            return this;
        }

        public NotifyClient build() {
            ManagedChannelBuilder<?> cb = ManagedChannelBuilder.forTarget(target);
            if (plaintext) cb.usePlaintext();
            if (token != null && !token.isBlank()) {
                cb.intercept(new TokenInterceptor(token));
            }
            ManagedChannel channel = cb.build();
            return new NotifyClient(channel,
                    NotifyGrpc.newBlockingStub(channel),
                    NotifyGrpc.newStub(channel));
        }
    }

    /** 把 token 注入每个调用的 metadata（公开供高级用法直接挂到自建 channel 上）。 */
    public static final class TokenInterceptor implements ClientInterceptor {
        static final Metadata.Key<String> KEY = Metadata.Key.of("x-api-token", Metadata.ASCII_STRING_MARSHALLER);

        private final String token;

        public TokenInterceptor(String token) {
            this.token = token;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, io.grpc.CallOptions callOptions,
                io.grpc.Channel next) {
            ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
            return new ForwardingClientCall.SimpleForwardingClientCall<>(call) {
                @Override
                public void start(io.grpc.ClientCall.Listener<RespT> listener, Metadata headers) {
                    headers.put(KEY, token);
                    super.start(listener, headers);
                }
            };
        }
    }

    /** 批量发布会话：send() 逐条发送，complete() 半关闭，await() 等待全部回执。 */
    public static final class PublishStreamSession {

        private final java.util.concurrent.atomic.AtomicReference<ClientCallStreamObserver<PublishRequest>> sender =
                new java.util.concurrent.atomic.AtomicReference<>();
        private final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

        PublishStreamSession(io.notifyhub.v1.NotifyGrpc.NotifyStub async,
                             Consumer<PublishAck> onAck, Consumer<Throwable> onError) {
            ClientResponseObserver<PublishRequest, PublishAck> observer =
                    new ClientResponseObserver<>() {
                        @Override
                        public void beforeStart(ClientCallStreamObserver<PublishRequest> requestStream) {
                            sender.set(requestStream);
                        }

                        @Override public void onNext(PublishAck value) { onAck.accept(value); }

                        @Override
                        public void onError(Throwable t) {
                            if (onError != null) onError.accept(t);
                            done.countDown();
                        }

                        @Override public void onCompleted() { done.countDown(); }
                    };
            async.publishStream(observer);
        }

        public void send(PublishRequest request) {
            ClientCallStreamObserver<PublishRequest> s = sender.get();
            if (s == null) throw new IllegalStateException("流尚未就绪");
            s.onNext(request);
        }

        /** 半关闭：告诉服务端"发完了"，之后服务端会结束流。 */
        public void complete() {
            ClientCallStreamObserver<PublishRequest> s = sender.get();
            if (s != null) s.onCompleted();
        }

        /** 等待流结束；超时返回 false。 */
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return done.await(timeout, unit);
        }
    }

    /** 订阅句柄：await() 等待流结束，cancel() 主动退订。 */
    public static final class SubscriptionHandle {
        private final CompletableFuture<Void> done;
        private final java.util.concurrent.atomic.AtomicReference<ClientCallStreamObserver<?>> callRef;

        SubscriptionHandle(CompletableFuture<Void> done,
                           java.util.concurrent.atomic.AtomicReference<ClientCallStreamObserver<?>> callRef) {
            this.done = done;
            this.callRef = callRef;
        }

        /** 阻塞直到流结束（服务端关闭或 cancel）。 */
        public void await() throws java.util.concurrent.ExecutionException, InterruptedException {
            done.get();
        }

        public void cancel(String message) {
            ClientCallStreamObserver<?> call = callRef.get();
            if (call != null) {
                call.cancel(message, new Cancelled());
            }
        }

        static final class Cancelled extends RuntimeException {
            Cancelled() { super("client cancelled"); }
        }
    }

    /** 便捷别名。 */
    public List<PlatformConfig> platforms() {
        return listPlatforms().getPlatformsList();
    }
}

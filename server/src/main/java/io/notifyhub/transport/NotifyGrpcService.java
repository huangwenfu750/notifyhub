package io.notifyhub.transport;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.notifyhub.channel.SenderRegistry;
import io.notifyhub.config.HubConfig;
import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Deduper;
import io.notifyhub.core.DeliveryService;
import io.notifyhub.core.Message;
import io.notifyhub.core.Router;
import io.notifyhub.pubsub.SubscriptionRegistry;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** gRPC 服务实现：Publish / Subscribe / PublishStream / Admin。 */
public final class NotifyGrpcService extends NotifyGrpc.NotifyImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotifyGrpcService.class);

    private final AdminStore store;
    private final SenderRegistry senders;
    private final SubscriptionRegistry subscriptions;
    private final DeliveryService delivery;
    private final Deduper deduper;
    private final HubConfig cfg;
    private final long startedAt = System.currentTimeMillis();

    public NotifyGrpcService(AdminStore store, SenderRegistry senders, SubscriptionRegistry subscriptions,
                             DeliveryService delivery, Deduper deduper, HubConfig cfg) {
        this.store = store;
        this.senders = senders;
        this.subscriptions = subscriptions;
        this.delivery = delivery;
        this.deduper = deduper;
        this.cfg = cfg;
    }

    // ---------- Publish ----------

    @Override
    public void publish(PublishRequest request, StreamObserver<PublishAck> responseObserver) {
        try {
            responseObserver.onNext(doPublish(request));
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    /** 单条发布语义：校验→去重→广播订阅者→入队平台投递。 */
    private PublishAck doPublish(PublishRequest req) {
        String topic = req.getTopic();
        if (topic == null || topic.isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("topic 不能为空").asRuntimeException();
        }
        List<PlatformConf> targets;
        if (!req.getPlatformsList().isEmpty()) {
            targets = new ArrayList<>();
            for (String name : req.getPlatformsList()) {
                PlatformConf p = store.get(name);
                if (p == null) {
                    throw Status.NOT_FOUND.withDescription("平台不存在: " + name).asRuntimeException();
                }
                targets.add(p);
            }
        } else {
            targets = Router.route(store.all(), topic);
        }

        String eventId = UUID.randomUUID().toString();
        Message msg = ProtoMappers.toMessage(req, eventId);

        String dedupKey = req.getOptions().getDedupKey();
        if (dedupKey != null && !dedupKey.isBlank() && !deduper.tryAcquire(dedupKey)) {
            return PublishAck.newBuilder()
                    .setEventId(eventId).setAccepted(false).setDeduplicated(true)
                    .setError("窗口期内重复的 dedup_key").build();
        }

        if (!req.getOptions().getSkipSubscribers()) {
            subscriptions.broadcast(msg);
        }
        if (!req.getOptions().getSkipPlatforms()) {
            for (PlatformConf p : targets) {
                if (delivery.enqueue(p, msg) == DeliveryService.EnqueueResult.QUEUE_FULL) {
                    throw Status.UNAVAILABLE.withDescription("投递队列已满，请稍后重试").asRuntimeException();
                }
            }
        }
        log.debug("发布 topic={} event={} platforms={}", topic, eventId, targets.size());
        PublishAck.Builder ack = PublishAck.newBuilder()
                .setEventId(eventId).setAccepted(true);
        // matched_platforms 表示实际入队投递的目标
        if (!req.getOptions().getSkipPlatforms()) {
            targets.forEach(p -> ack.addMatchedPlatforms(p.name()));
        }
        return ack.build();
    }

    // ---------- PublishStream（双向流：按条回执，不中断流） ----------

    @Override
    public StreamObserver<PublishRequest> publishStream(StreamObserver<PublishAck> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(PublishRequest req) {
                try {
                    responseObserver.onNext(doPublish(req));
                } catch (StatusRuntimeException e) {
                    // 按条回执错误，流继续保持
                    responseObserver.onNext(PublishAck.newBuilder()
                            .setAccepted(false)
                            .setError(e.getStatus().getCode() + ": " + e.getStatus().getDescription())
                            .build());
                } catch (Exception e) {
                    responseObserver.onNext(PublishAck.newBuilder()
                            .setAccepted(false).setError("INTERNAL: " + e.getMessage()).build());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.debug("publishStream 客户端断开: {}", String.valueOf(t.getMessage()));
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    // ---------- Subscribe ----------

    @Override
    public void subscribe(SubscribeRequest request, StreamObserver<Event> responseObserver) {
        SubscriptionRegistry.Subscription sub;
        try {
            sub = subscriptions.register(request.getTopicsList());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
            return;
        }
        AtomicBoolean closed = new AtomicBoolean(false);
        if (responseObserver instanceof io.grpc.stub.ServerCallStreamObserver<Event> sco) {
            sco.setOnCancelHandler(() -> {
                closed.set(true);
                subscriptions.unregister(sub.id());
            });
        }
        log.info("新订阅 id={} patterns={}", sub.id(), sub.patterns());
        Thread pump = new Thread(() -> pumpEvents(sub, responseObserver, closed), "notifyhub-sub-" + sub.id());
        pump.setDaemon(true);
        pump.start();
    }

    private void pumpEvents(SubscriptionRegistry.Subscription sub, StreamObserver<Event> resp,
                            AtomicBoolean closed) {
        try {
            while (!closed.get()) {
                Message m = sub.queue().poll(200, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (m == null) continue;
                if (closed.get()) break;
                try {
                    resp.onNext(ProtoMappers.toEvent(m));
                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode() == Status.Code.CANCELLED) break;
                    throw e;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("订阅泵退出 id={}: {}", sub.id(), String.valueOf(e.getMessage()));
        } finally {
            closed.set(true);
            subscriptions.unregister(sub.id());
            log.info("订阅结束 id={}", sub.id());
        }
    }

    // ---------- Admin ----------

    @Override
    public void upsertPlatform(PlatformConfig request, StreamObserver<PlatformConfig> responseObserver) {
        try {
            PlatformConf conf = ProtoMappers.toConf(request, cfg.defaultMaxAttempts,
                    cfg.defaultBackoffMs, cfg.defaultQps);
            validate(conf);
            store.put(conf);
            log.info("平台已注册 name={} type={} topics={}", conf.name(), conf.type(), conf.topics());
            responseObserver.onNext(ProtoMappers.toProto(store.get(conf.name())));
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    private void validate(PlatformConf conf) {
        if (conf.name() == null || conf.name().isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        if (conf.type() == null || conf.type().isBlank()) {
            throw new IllegalArgumentException("type 不能为空");
        }
        if (!senders.knows(conf.type())) {
            throw new IllegalArgumentException("未知平台类型: " + conf.type()
                    + "（支持: dingtalk, wecom, feishu, webhook）");
        }
        if (conf.webhook() == null || conf.webhook().isBlank()) {
            throw new IllegalArgumentException("webhook 不能为空");
        }
        if (!conf.webhook().startsWith("http://") && !conf.webhook().startsWith("https://")) {
            throw new IllegalArgumentException("webhook 必须以 http(s):// 开头");
        }
    }

    @Override
    public void listPlatforms(Empty request, StreamObserver<PlatformList> responseObserver) {
        PlatformList.Builder b = PlatformList.newBuilder();
        store.all().forEach(p -> b.addPlatforms(ProtoMappers.toProto(p)));
        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
    }

    @Override
    public void removePlatform(PlatformRef request, StreamObserver<Empty> responseObserver) {
        String name = request.getName();
        if (name == null || name.isBlank() || store.remove(name) == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("平台不存在: " + name).asException());
            return;
        }
        log.info("平台已移除 name={}", name);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    // ---------- Ping ----------

    @Override
    public void ping(Empty request, StreamObserver<Pong> responseObserver) {
        responseObserver.onNext(Pong.newBuilder()
                .setVersion(io.notifyhub.Server.VERSION)
                .setUptimeSeconds((System.currentTimeMillis() - startedAt) / 1000)
                .build());
        responseObserver.onCompleted();
    }
}

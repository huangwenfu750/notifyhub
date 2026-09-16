package io.notifyhub;

import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.notifyhub.channel.SenderRegistry;
import io.notifyhub.config.HubConfig;
import io.notifyhub.config.PlatformConf;
import io.notifyhub.core.Deduper;
import io.notifyhub.core.DeliveryService;
import io.notifyhub.pubsub.SubscriptionRegistry;
import io.notifyhub.transport.AdminStore;
import io.notifyhub.transport.AuthInterceptor;
import io.notifyhub.transport.NotifyGrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;

/** 服务组装与生命周期管理。 */
public final class Server implements AutoCloseable {

    public static final String VERSION = "0.1.0";

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final HubConfig cfg;
    private final io.grpc.Server grpc;
    private final DeliveryService delivery;

    private Server(HubConfig cfg, io.grpc.Server grpc, DeliveryService delivery) {
        this.cfg = cfg;
        this.grpc = grpc;
        this.delivery = delivery;
    }

    public static Server create(HubConfig cfg) {
        io.notifyhub.channel.HttpPoster poster = new io.notifyhub.channel.HttpPoster();
        SenderRegistry senders = new SenderRegistry(poster);

        for (PlatformConf p : cfg.platforms) {
            if (!senders.knows(p.type())) {
                throw new IllegalArgumentException(
                        "平台 " + p.name() + " 类型未知: " + p.type() + "（支持: dingtalk, wecom, feishu, webhook）");
            }
        }

        AdminStore store = new AdminStore(cfg.platforms);
        SubscriptionRegistry subs = new SubscriptionRegistry();
        DeliveryService delivery = new DeliveryService(senders, subs, cfg);
        Deduper deduper = new Deduper(cfg.dedupWindowMs);
        NotifyGrpcService service = new NotifyGrpcService(store, senders, subs, delivery, deduper, cfg);

        NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress(cfg.host, cfg.port))
                .addService(service)
                .maxInboundMessageSize(4 * 1024 * 1024);
        // 空白名单时不挂拦截器，让 Ping 之外的方法也放行
        if (!cfg.tokens.isEmpty()) {
            builder.intercept(new AuthInterceptor(new HashSet<>(cfg.tokens)));
        }
        return new Server(cfg, builder.build(), delivery);
    }

    public void start() throws IOException {
        grpc.start();
        log.info("NotifyHub {} 已启动，监听 {}:{} (tokens={}, platforms={}, workers={})",
                VERSION, cfg.host, actualPort(), cfg.tokens.size(), cfg.platforms.size(), cfg.workers);
    }

    public int actualPort() {
        return grpc.getPort();
    }

    public void awaitTermination() throws InterruptedException {
        grpc.awaitTermination();
    }

    @Override
    public void close() {
        delivery.close();
        grpc.shutdownNow();
        log.info("NotifyHub 已停止");
    }
}

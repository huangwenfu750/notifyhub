package io.notifyhub;

import io.notifyhub.config.ConfigLoader;
import io.notifyhub.config.HubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/** 入口：notifyhub-server [--config <path>]，也可用环境变量 NOTIFYHUB_CONFIG 指定配置路径。 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String cfgPath = System.getenv().getOrDefault("NOTIFYHUB_CONFIG", "config.yaml");
        for (int i = 0; i < args.length; i++) {
            if (("--config".equals(args[i]) || "-c".equals(args[i])) && i + 1 < args.length) {
                cfgPath = args[++i];
            } else if ("--version".equals(args[i])) {
                System.out.println("NotifyHub " + Server.VERSION);
                return;
            }
        }

        HubConfig cfg = ConfigLoader.load(Path.of(cfgPath));
        Server server = Server.create(cfg);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "notifyhub-shutdown"));
        server.start();

        CountDownLatch done = new CountDownLatch(1);
        done.await(); // 主线程挂起，等待 shutdown hook
    }

    private Main() {}
}

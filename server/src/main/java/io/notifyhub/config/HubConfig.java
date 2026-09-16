package io.notifyhub.config;

import java.util.ArrayList;
import java.util.List;

/** 服务端运行配置（由 ConfigLoader 从 YAML 装配）。 */
public final class HubConfig {

    public String host = "0.0.0.0";
    public int port = 9987;
    /** API token 白名单；为空表示不鉴权 */
    public List<String> tokens = new ArrayList<>();
    /** 配置文件声明的平台 */
    public List<PlatformConf> platforms = new ArrayList<>();
    /** 投递 worker 线程数 */
    public int workers = Math.max(4, Runtime.getRuntime().availableProcessors());
    /** 投递队列容量 */
    public int queueCapacity = 10_000;
    /** dedup_key 去重窗口（毫秒） */
    public long dedupWindowMs = 60_000;
    /** 全局默认重试 */
    public int defaultMaxAttempts = 3;
    public long defaultBackoffMs = 500;
    /** 全局默认平台限流 QPS */
    public int defaultQps = 15;
}

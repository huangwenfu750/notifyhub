package io.notifyhub.transport;

import io.notifyhub.config.PlatformConf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** 平台配置存储：启动时来自 YAML，运行时经 Admin RPC 增删改（内存态，重启需重新注册）。 */
public final class AdminStore {

    private final ConcurrentHashMap<String, PlatformConf> map = new ConcurrentHashMap<>();

    public AdminStore(Collection<PlatformConf> initial) {
        for (PlatformConf p : initial) {
            map.put(p.name(), p);
        }
    }

    public PlatformConf get(String name) {
        return map.get(name);
    }

    /** @return 被覆盖的旧配置（null 表示新增） */
    public PlatformConf put(PlatformConf conf) {
        return map.put(conf.name(), conf);
    }

    public PlatformConf remove(String name) {
        return map.remove(name);
    }

    /** 当前快照，按名称排序保证输出稳定。 */
    public List<PlatformConf> all() {
        List<PlatformConf> out = new ArrayList<>(map.values());
        out.sort((a, b) -> a.name().compareTo(b.name()));
        return out;
    }
}

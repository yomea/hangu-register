package org.hangu.disconver.manager;

import io.netty.util.concurrent.DefaultPromise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hangu.common.entity.RpcResult;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:57
 */
public class RpcRequestManager {

    private static final Map<Long, DefaultPromise<RpcResult>> FUTURE_MAP = new ConcurrentHashMap<>(8192);

    public static void putFuture(Long id, DefaultPromise<RpcResult> future) {
        FUTURE_MAP.put(id, future);
    }

    public static DefaultPromise<RpcResult> getFuture(Long id) {
        return FUTURE_MAP.get(id);
    }
}

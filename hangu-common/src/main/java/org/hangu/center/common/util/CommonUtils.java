package org.hangu.center.common.util;

import cn.hutool.core.util.IdUtil;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:16
 */
public final class CommonUtils {

    private CommonUtils() {
        throw new RuntimeException("不允许实例化！");
    }

    public static Long snowFlakeNextId() {

        return IdUtil.getSnowflake(1, 1).nextId();
    }

    public static String createServiceKey(String groupName, String interfaceName, String version) {

        return groupName + "/" + version + "/" + interfaceName;
    }

    public static String createServiceKey(ServerInfo serverInfo) {

        return serverInfo.getGroupName() + "/" + serverInfo.getVersion() + "/" + serverInfo.getInterfaceName();
    }

    public static Response createResponseInfo(
        Long id,
        int code,
        Class<?> clzz,
        Object value) {

        RpcResult rpcResult = new RpcResult();
        rpcResult.setCode(code);
        rpcResult.setReturnType(clzz);
        rpcResult.setResult(value);
        // 返回响应
        Response response = new Response();
        response.setId(id);
        response.setRpcResult(rpcResult);

        return response;
    }

    public static ClassLoader getClassLoader(Class<?> cls) {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = cls.getClassLoader();
        }
        return cl;
    }

}

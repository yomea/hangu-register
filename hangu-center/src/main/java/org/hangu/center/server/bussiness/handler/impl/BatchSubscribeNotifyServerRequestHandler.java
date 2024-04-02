package org.hangu.center.server.bussiness.handler.impl;

import io.netty.channel.Channel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.RegistryNotifyInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.util.CommonUtils;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;

/**
 * 订阅通知
 *
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
public class BatchSubscribeNotifyServerRequestHandler implements RequestHandler<Set<ServerInfo>> {

    private ServiceRegisterManager serviceRegisterManager;

    public BatchSubscribeNotifyServerRequestHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.BATCH_SUBSCRIBE_SERVICE;
    }

    @Override
    public Response handler(Request<Set<ServerInfo>> request, NettyServer nettyServer, Channel channel) {
        // 拉取服务列表
        Set<ServerInfo> serverInfoList = request.getBody();
        List<RegistryInfo> registryInfos = serverInfoList.stream().flatMap(serverInfo ->
            Optional.ofNullable(serviceRegisterManager.subscribe(channel, serverInfo))
                .orElse(Collections.emptyList()).stream()).collect(Collectors.toList());

        // 不需要响应
        if (request.isOneWay()) {
            return null;
        }

        /*Map<String, List<RegistryInfo>> keyMap = registryInfos.stream()
            .collect(Collectors.groupingBy(CommonUtils::createServiceKey));

        List<RegistryNotifyInfo> notifyInfos = serverInfoList.stream().map(serverInfo -> {
            RegistryNotifyInfo notifyInfo = new RegistryNotifyInfo();
            notifyInfo.setServerInfo(serverInfo);
            notifyInfo.setRegistryInfos(
                keyMap.getOrDefault(CommonUtils.createServiceKey(serverInfo), Collections.emptyList()));
            return notifyInfo;
        }).collect(Collectors.toList());*/

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(true);
        rpcResult.setReturnType(Boolean.class);
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

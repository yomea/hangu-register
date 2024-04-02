package org.hangu.center.server.bussiness.handler.impl;

import io.netty.channel.Channel;
import java.util.List;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.RegistryNotifyInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;

/**
 * 订阅通知
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
public class SingleSubscribeNotifyServerRequestHandler implements RequestHandler<ServerInfo> {

    private ServiceRegisterManager serviceRegisterManager;
    public SingleSubscribeNotifyServerRequestHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.SINGLE_SUBSCRIBE_SERVICE;
    }

    @Override
    public Response handler(Request<ServerInfo> request, NettyServer nettyServer, Channel channel) {
        // 拉取服务列表
        ServerInfo serverInfo = request.getBody();
        List<RegistryInfo> registryInfos = serviceRegisterManager.subscribe(channel, serverInfo);

        RegistryNotifyInfo registryNotifyInfo = new RegistryNotifyInfo();
        registryNotifyInfo.setServerInfo(serverInfo);
        registryNotifyInfo.setRegistryInfos(registryInfos);

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(registryNotifyInfo);
        rpcResult.setReturnType(RegistryNotifyInfo.class);
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

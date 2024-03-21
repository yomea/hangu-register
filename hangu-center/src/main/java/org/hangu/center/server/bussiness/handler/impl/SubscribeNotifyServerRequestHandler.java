package org.hangu.center.server.bussiness.handler.impl;

import java.util.List;
import java.util.Objects;
import org.hangu.center.common.entity.CenterNodeInfo;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.RegistryInfoDirectory;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订阅通知
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
@Service
public class SubscribeNotifyServerRequestHandler implements RequestHandler<ServerInfo> {

    @Autowired
    private ServiceRegisterManager serviceRegisterManager;

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.NOTIFY_REGISTER_SERVICE;
    }

    @Override
    public Response handler(Request<ServerInfo> request, NettyServer nettyServer) {
        // 拉取服务列表
        ServerInfo serverInfo = request.getBody();
        List<RegistryInfo> registryInfos = serviceRegisterManager.subscribe(nettyServer, serverInfo);

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(registryInfos);
        rpcResult.setReturnType(List.class);
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

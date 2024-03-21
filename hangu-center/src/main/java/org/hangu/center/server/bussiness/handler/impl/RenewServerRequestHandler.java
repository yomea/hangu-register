package org.hangu.center.server.bussiness.handler.impl;

import org.hangu.center.common.entity.RegistryInfoDirectory;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 增量拉取服务
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
@Service
public class RenewServerRequestHandler implements RequestHandler<RegistryInfoDirectory> {

    @Autowired
    private ServiceRegisterManager serviceRegisterManager;

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.RENEW_SERVICE;
    }

    @Override
    public Response handler(Request<RegistryInfoDirectory> request, NettyServer nettyServer) {

        RegistryInfoDirectory directory = request.getBody();
        serviceRegisterManager.renew(directory.getRegistryInfoList());
        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        // 返回当前服务的状态
        ServerStatusEnum status = nettyServer.getStatus();
        rpcResult.setResult(status.getStatus());
        rpcResult.setReturnType(Integer.class);
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

package org.hangu.center.server.bussiness.handler.impl;

import java.util.List;
import java.util.Objects;
import org.hangu.center.common.entity.CenterNodeInfo;
import org.hangu.center.common.entity.RegistryInfo;
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
public class RenewAndDeltaPullServerRequestHandler implements RequestHandler<RegistryInfoDirectory> {

    @Autowired
    private ServiceRegisterManager serviceRegisterManager;

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.RENEW_AND_DELTA_PULL_SERVICE;
    }

    @Override
    public Response handler(Request<RegistryInfoDirectory> request, NettyServer nettyServer) {
        // 拉取服务列表
        RegistryInfoDirectory registryInfo = request.getBody();
        serviceRegisterManager.renew(registryInfo.getRegistryInfoList());
        Long afterRegisterTime = registryInfo.getRegisterTime();
        afterRegisterTime = Objects.isNull(afterRegisterTime) ? 0L : afterRegisterTime;
        List<RegistryInfo> infos = serviceRegisterManager.lookupAfterTime(afterRegisterTime);

        ServerStatusEnum status = nettyServer.getStatus();
        CenterNodeInfo centerNodeInfo = new CenterNodeInfo();
        centerNodeInfo.setStatus(status.getStatus());
        centerNodeInfo.setInfoList(infos);

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(centerNodeInfo);
        rpcResult.setReturnType(centerNodeInfo.getClass());
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

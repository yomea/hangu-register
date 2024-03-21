package org.hangu.center.server.bussiness.handler.impl;

import java.util.List;
import java.util.Objects;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;

/**
 * 增量拉取服务
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
public class PullDeltaServerRequestHandler extends AbstractRequestHandler<Long> {

    private ServiceRegisterManager serviceRegisterManager;

    public PullDeltaServerRequestHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.DELTA_PULL_SERVICE;
    }

    @Override
    public Response doHandler(Request<Long> request, NettyServer nettyServer) {
        // 拉取服务列表
        Long afterRegisterTime = request.getBody();
        afterRegisterTime = Objects.isNull(afterRegisterTime) ? 0L : afterRegisterTime;
        List<RegistryInfo> infos = serviceRegisterManager.lookupAfterTime(afterRegisterTime);

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(infos);
        rpcResult.setReturnType(infos.getClass());
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

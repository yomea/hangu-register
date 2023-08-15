package org.hangu.center.bussiness.handler.impl;

import java.util.List;
import java.util.Objects;
import org.hangu.center.bussiness.handler.RequestHandler;
import org.hangu.center.manager.ServiceRegisterManager;
import org.hangu.common.entity.LookupServer;
import org.hangu.common.entity.RegistryInfo;
import org.hangu.common.entity.Request;
import org.hangu.common.entity.Response;
import org.hangu.common.entity.RpcResult;
import org.hangu.common.enums.CommandTypeMarkEnum;
import org.hangu.common.enums.ErrorCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 增量拉取服务
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
@Service
public class PullDeltaServerRequestHandler implements RequestHandler<Long> {

    @Autowired
    private ServiceRegisterManager serviceRegisterManager;

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.DELTA_PULL_SERVICE;
    }

    @Override
    public Response handler(Request<Long> request) {
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

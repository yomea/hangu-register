package org.hangu.center.bussiness.handler.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
@Service
public class RemoveServerRequestHandler implements RequestHandler<List<RegistryInfo>> {

    @Autowired
    private ServiceRegisterManager serviceRegisterManager;

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.BATCH_REMOVE_SERVICE;
    }

    @Override
    public Response handler(Request<List<RegistryInfo>> request) {

        List<RegistryInfo> registryInfos = Optional.ofNullable(request.getBody()).orElse(Collections.emptyList());
        registryInfos.stream().forEach(serviceRegisterManager::register);

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());

        RpcResult rpcResult = new RpcResult();
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        rpcResult.setReturnType(Boolean.class);
        rpcResult.setResult(true);
        response.setRpcResult(rpcResult);

        return response;
    }
}

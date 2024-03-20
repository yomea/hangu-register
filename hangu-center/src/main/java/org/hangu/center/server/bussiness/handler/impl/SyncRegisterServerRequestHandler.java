package org.hangu.center.server.bussiness.handler.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
@Service
public class SyncRegisterServerRequestHandler extends AbstractRequestHandler<List<RegistryInfo>> {

    @Autowired
    private ServiceRegisterManager serviceRegisterManager;

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.BATCH_SYNC_REGISTER_SERVICE;
    }

    @Override
    public Response doHandler(Request<List<RegistryInfo>> request) {

        List<RegistryInfo> registryInfos = Optional.ofNullable(request.getBody()).orElse(Collections.emptyList());
        registryInfos.stream().forEach(serviceRegisterManager::syncRegistry);

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

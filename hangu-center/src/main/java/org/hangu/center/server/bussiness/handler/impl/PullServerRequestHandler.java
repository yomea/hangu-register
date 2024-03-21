package org.hangu.center.server.bussiness.handler.impl;

import java.util.List;
import java.util.Objects;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;
import org.springframework.util.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
public class PullServerRequestHandler extends AbstractRequestHandler<LookupServer> {

    private ServiceRegisterManager serviceRegisterManager;

    public PullServerRequestHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.PULL_SERVICE;
    }

    @Override
    public Response doHandler(Request<LookupServer> request, NettyServer nettyServer) {
        // 拉取服务列表
        LookupServer lookupServer = request.getBody();
        List<RegistryInfo> infos;
        if (Objects.isNull(lookupServer)) {
            infos = serviceRegisterManager.lookup();
        } else {
            String groupName = lookupServer.getGroupName();
            String interfaceName = lookupServer.getInterfaceName();
            String version = lookupServer.getVersion();
            if (!StringUtils.hasText(groupName) && !StringUtils.hasText(interfaceName) && !StringUtils.hasText(
                version)) {
                infos = serviceRegisterManager.lookup();
            } else {
                infos = serviceRegisterManager.lookup(lookupServer);
            }
        }

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

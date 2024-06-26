package org.hangu.center.server.bussiness.handler.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.CenterNodeInfo;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.NettyClient;
import org.hangu.center.server.manager.ServiceRegisterManager;

/**
 * @author wuzhenhong
 * @date 2024/3/11 17:14
 */
@Slf4j
public class RenewAndDeltaPullResponseHandler implements ResponseHandler {

    private ServiceRegisterManager serviceRegisterManager;
    public RenewAndDeltaPullResponseHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.RENEW_AND_DELTA_PULL_SERVICE;
    }

    @Override
    public void handler(Response response, NettyClient nettyClient) {
        RpcResult rpcResult = response.getRpcResult();
        if(rpcResult.getCode() != ErrorCodeEnum.SUCCESS.getCode()) {
            Exception exception = (Exception) rpcResult.getResult();
            log.error("center 节点续约并增量拉取服务失败！", exception);
        } else {
            CenterNodeInfo centerNodeInfo = (CenterNodeInfo) rpcResult.getResult();
            nettyClient.setStatus(ServerStatusEnum.getEnumByStatus(centerNodeInfo.getStatus()));
            List<RegistryInfo> infoList = Optional.ofNullable(centerNodeInfo.getInfoList()).orElse(Collections.emptyList());
            infoList.stream().forEach(serviceRegisterManager::register);
            Long maxPullRegisterTime = infoList.stream().map(RegistryInfo::getRegisterTime).max(Long::compareTo).orElse(0L);
            nettyClient.updateMaxPullRegistryTime(maxPullRegisterTime);
        }
    }
}

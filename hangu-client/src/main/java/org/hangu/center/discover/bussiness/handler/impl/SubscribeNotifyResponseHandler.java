package org.hangu.center.discover.bussiness.handler.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.client.NettyClient;

/**
 * @author wuzhenhong
 * @date 2024/3/11 17:14
 */
@Slf4j
public class SubscribeNotifyResponseHandler implements ResponseHandler {

    private DiscoverClient discoverClient;

    public SubscribeNotifyResponseHandler(DiscoverClient discoverClient) {
        this.discoverClient = discoverClient;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.SINGLE_SUBSCRIBE_SERVICE;
    }

    @Override
    public void handler(Response response, NettyClient nettyClient) {
        RpcResult rpcResult = response.getRpcResult();
        if(rpcResult.getCode() != ErrorCodeEnum.SUCCESS.getCode()) {
            Exception exception = (Exception) rpcResult.getResult();
            log.error("订阅通知异常！", exception);
        } else {
            List<RegistryInfo> hostInfoList = (List<RegistryInfo>) rpcResult.getResult();
            List<RegistryInfo> infoList = Optional.ofNullable(hostInfoList).orElse(Collections.emptyList());
            discoverClient.notify(infoList);
        }
    }
}

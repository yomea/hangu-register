package org.hangu.center.discover.bussiness.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.NettyClient;

/**
 * @author wuzhenhong
 * @date 2024/3/11 16:56
 */
@Slf4j
public class RenewResponseHandler implements ResponseHandler {

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.RENEW_SERVICE;
    }

    @Override
    public void handler(Response response, NettyClient nettyClient) {

        RpcResult rpcResult = response.getRpcResult();
        if(rpcResult.getCode() != ErrorCodeEnum.SUCCESS.getCode()) {
            Exception exception = (Exception) rpcResult.getResult();
            log.error("向注册中心续约失败！", exception);
        } else {
            Integer status = (Integer) rpcResult.getResult();
            nettyClient.setStatus(ServerStatusEnum.getEnumByStatus(status));
        }

    }
}

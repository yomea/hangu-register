package org.hangu.center.discover.bussiness.handler.impl;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.client.NettyClient;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2024/3/11 16:56
 */
@Slf4j
public class ChannelActiveResponseHandler implements ResponseHandler {

    private DiscoverClient discoverClient;

    public ChannelActiveResponseHandler(DiscoverClient discoverClient) {
        this.discoverClient = discoverClient;
    }


    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.CHANNEL_ACTIVE_SERVICE;
    }

    @Override
    public void handler(Response response, NettyClient nettyClient) {
        // 检查该客户端是否订阅过接口，如果订阅过，再重连成功之后，向注册中心注册新的通道
        Set<ServerInfo> serverInfoSet = nettyClient.getSubscribeServerInfoList();
        if(CollectionUtils.isEmpty(serverInfoSet)) {
            return;
        }
        this.discoverClient.reSendSubscribe(nettyClient, serverInfoSet);
    }
}

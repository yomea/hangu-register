package org.hangu.center.discover.bussiness.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.client.NettyClient;

/**
 * @author wuzhenhong
 * @date 2024/3/11 16:56
 */
@Slf4j
public class ChannelUnregisteredResponseHandler implements ResponseHandler {

    private DiscoverClient discoverClient;

    public ChannelUnregisteredResponseHandler(DiscoverClient discoverClient) {
        this.discoverClient = discoverClient;
    }


    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.UN_REGISTERED_SERVICE;
    }

    @Override
    public void handler(Response response, NettyClient nettyClient) {
        // 检查该客户端是否订阅过接口，如果订阅过，并且该链接失效了，重试向其他注册节点订阅
        Set<ServerInfo> serverInfoSet = nettyClient.getSubscribeServerInfoList();
        if(CollectionUtil.isNotEmpty(serverInfoSet)) {
            // 清理该客户端订阅的内容
            Set<ServerInfo> copyServerInfoSet = new HashSet<>(serverInfoSet);
            nettyClient.clearSubscribeServerInfo();
            this.discoverClient.reSendSubscribe(copyServerInfoSet);
        }
    }
}

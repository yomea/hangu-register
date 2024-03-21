package org.hangu.center.discover.manager;

import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.hangu.center.discover.client.NettyClient;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/14 14:08
 */
public class CenterConnectManager {

    private final AtomicInteger idx = new AtomicInteger();
    private List<NettyClient> centerChannelList = new ArrayList<>();

    public CenterConnectManager() {

    }

    public synchronized void cacheChannel(NettyClient nettyClient) {
        centerChannelList.add(nettyClient);
    }

    public Optional<Channel> pollActiveChannel() {
        List<Channel> channelList = centerChannelList.stream()
            .filter(NettyClient::isActive).map(NettyClient::getChannel).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(channelList)) {
            return Optional.empty();
        }
        return Optional.ofNullable(channelList.get(Math.abs(idx.getAndIncrement() % channelList.size())));
    }

    public List<NettyClient> getActiveCenterChannelList() {
        return centerChannelList.stream()
            .filter(NettyClient::isActive)
            .filter(e -> e.isUnKnow() || e.isComplete()).collect(Collectors.toList());
    }

    public Optional<NettyClient> pollActiveAndCompleteChannel(List<NettyClient> exclusionList) {
        List<NettyClient> channelList = centerChannelList.stream()
            .filter(NettyClient::isActive)
            .filter(e -> {
                boolean exists = false;
                for(NettyClient exclusion : exclusionList) {
                    if(e == exclusion) {
                        exists = true;
                        break;
                    }
                }
                return !exists;
            })
            .filter(e -> e.isUnKnow() || e.isComplete()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(channelList)) {
            return Optional.empty();
        }
        return Optional.ofNullable(channelList.get(Math.abs(idx.getAndIncrement() % channelList.size())));
    }
}

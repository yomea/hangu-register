package org.hangu.center.discover.manager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.hangu.center.discover.client.NettyClient;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/14 14:08
 */
public class ConnectManager {

    private Map<ChannelId, NettyClient> centerChannelMap = new ConcurrentHashMap<>();

    public ConnectManager() {

    }

    public void cacheChannel(NettyClient clientConnect) {
        centerChannelMap.put(clientConnect.getChannel().id(), clientConnect);
    }

    public void removeChannel(Channel channel) {
        centerChannelMap.remove(channel.id());
    }

    public Optional<Channel> randActiveChannel(Set<ChannelId> exclusionList) {
        List<Channel> channelList = centerChannelMap.values().stream()
            .filter(NettyClient::isActive).filter(ClientConnect -> !exclusionList.contains(ClientConnect.getChannel().id()))
            .map(NettyClient::getChannel).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(channelList)) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(0, channelList.size());
        return Optional.ofNullable(channelList.get(index));
    }
}

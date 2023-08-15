package org.hangu.discover.manager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/14 14:08
 */
public class ConnectManager {

    private Map<ChannelId, Channel> centerChannelMap = new ConcurrentHashMap<>();

    public ConnectManager() {

    }

    public void cacheChannel(Channel channel) {
        centerChannelMap.put(channel.id(), channel);
    }

    public void removeChannel(Channel channel) {
        centerChannelMap.remove(channel.id());
    }

    public Optional<Channel> randActiveChannel(Set<ChannelId> exclusionList) {
        List<Channel> channelList = centerChannelMap.values().stream()
            .filter(Channel::isActive).filter(channel -> !exclusionList.contains(channel.id()))
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(channelList)) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(0, channelList.size());
        return Optional.ofNullable(channelList.get(index));
    }
}

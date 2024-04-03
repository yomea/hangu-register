package org.hangu.center.discover.manager;

import cn.hutool.core.collection.CollectionUtil;
import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.client.NettyClient;
import org.hangu.center.discover.properties.ClientProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/14 14:08
 */
@Slf4j
public class CenterConnectManager {

    private final AtomicInteger idx = new AtomicInteger();
    private List<NettyClient> centerChannelList = new ArrayList<>();
    private Set<HostInfo> hostInfoSet = new HashSet<>();
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService executorService;
    private ClientProperties clientProperties;
    private boolean center;
    private DiscoverClient discoverClient;

    public CenterConnectManager(
        DiscoverClient discoverClient,
        ClientProperties clientProperties,
        ExecutorService executorService,
        ScheduledExecutorService scheduledExecutorService,
        boolean center) {
        this.discoverClient = discoverClient;
        this.clientProperties = clientProperties;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.center = center;
    }

    public synchronized boolean openNewChannel(HostInfo hostInfo) throws InterruptedException {
        // 启动netty客户端
        NettyClient nettyClient = new NettyClient(this.discoverClient, this, clientProperties.getTransport(),
            hostInfo, this.center);
        boolean success = this.cacheChannel(nettyClient, true);
        if (success) {
            nettyClient.open(this.executorService);
            nettyClient.syncConnect();
        }
        return success;
    }

    private synchronized boolean cacheChannel(NettyClient nettyClient, boolean share) {
        boolean success = false;
        if (share) {
            if (!hostInfoSet.contains(nettyClient.getHostInfo())) {
                hostInfoSet.add(nettyClient.getHostInfo());
                success = centerChannelList.add(nettyClient);
            } else {
                // nothing to do
            }
        } else {
            hostInfoSet.add(nettyClient.getHostInfo());
            success = centerChannelList.add(nettyClient);
        }
        return success;
    }

    public Optional<Channel> pollActiveChannel() {
        List<Channel> channelList = centerChannelList.stream()
            .filter(NettyClient::isActive)
            .filter(e -> !e.isRelease())
            .map(NettyClient::getChannel).collect(Collectors.toList());

        if (CollectionUtil.isEmpty(channelList)) {
            return Optional.empty();
        }
        return Optional.ofNullable(channelList.get(Math.abs(idx.getAndIncrement() % channelList.size())));
    }

    public List<NettyClient> getActiveCenterChannelList() {
        return centerChannelList.stream()
            .filter(NettyClient::isActive)
            .filter(e -> !e.isRelease())
            .filter(e -> e.isUnKnow() || e.isComplete()).collect(Collectors.toList());
    }

    public Optional<NettyClient> pollActiveAndCompleteChannel(List<NettyClient> exclusionList) {
        List<NettyClient> channelList = centerChannelList.stream()
            .filter(NettyClient::isActive)
            .filter(e -> {
                boolean exists = false;
                for (NettyClient exclusion : exclusionList) {
                    if (e == exclusion) {
                        exists = true;
                        break;
                    }
                }
                return !exists;
            })
            .filter(e -> e.isUnKnow() || e.isComplete())
            .filter(e -> !e.isRelease())
            .collect(Collectors.toList());

        if (CollectionUtil.isEmpty(channelList)) {
            return Optional.empty();
        }
        return Optional.ofNullable(channelList.get(Math.abs(idx.getAndIncrement() % channelList.size())));
    }

    public void refreshCenterConnect(List<HostInfo> hostInfoList) {

        if (CollectionUtil.isEmpty(hostInfoList)) {
            return;
        }

        List<NettyClient> waitCloseNettClientList = this.centerChannelList.stream().filter(nettyClient -> {
            if (!hostInfoList.contains(nettyClient.getHostInfo())) {
                nettyClient.markRelease(true);
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        hostInfoList.stream().filter(hostInfo -> !this.hostInfoSet.contains(hostInfo))
            .forEach(e -> {
                try {
                    this.openNewChannel(e);
                } catch (InterruptedException ex) {
                    log.error("注册中心通知：链接ip{}失败！", e, ex);
                }
            });

        if (CollectionUtil.isNotEmpty(waitCloseNettClientList)) {
            this.scheduledExecutorService.schedule(() -> {
                waitCloseNettClientList.stream().forEach(NettyClient::close);
            }, 30, TimeUnit.SECONDS);
        }
    }
}

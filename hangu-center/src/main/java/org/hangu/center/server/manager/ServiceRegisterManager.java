package org.hangu.center.server.manager;

import cn.hutool.core.net.NetUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.common.exception.NoServerAvailableException;
import org.hangu.center.common.properties.TransportProperties;
import org.hangu.center.common.util.CommonUtils;
import org.hangu.center.discover.lookup.LookupService;
import org.hangu.center.server.client.CloudDiscoverClient;
import org.hangu.center.server.properties.CenterProperties;
import org.hangu.center.server.server.CenterServer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 服务注册列表
 *
 * @author wuzhenhong
 * @date 2023/7/31 15:07
 */
@Slf4j
public class ServiceRegisterManager implements InitializingBean, LookupService {

    private static final int DEFAULT_SIZE = 1024;

    private static final int TIME_DELAY = 2 * 60 * 1000;

    private Object LOCK = new Object();

    // 订阅列表
    private final Map<String, Map<ChannelId, Channel>> subscribeTable = new HashMap<>(
        DEFAULT_SIZE);
    private final Map<ChannelId, Set<String>> channelSubKeysTable = new HashMap<>(
        DEFAULT_SIZE);
    private final Map<String, Object> subscribeLock = new ConcurrentHashMap<>(
        DEFAULT_SIZE);

    /**
     * 接口与所在机器的host映射
     * key -> groupName + "/" + version + "/" + interfaceName
     * value -> 地址集合
     */
    private final Map<String, Map<HostInfo, RegistryInfo>> serviceKeyMapHostInfos = new ConcurrentHashMap<>(
        DEFAULT_SIZE);


    private CenterServer centerServer;

    private CloudDiscoverClient discoverClient;

    private TransportProperties transportProperties;

    private CenterProperties centerProperties;

    private ScheduledExecutorService scheduledExecutorService;

    private HostInfo hostInfo;

    private long heartExpireTimes;

    private ExecutorService workExecutorService;

    public ServiceRegisterManager(ExecutorService workExecutorService, CenterServer centerServer, CloudDiscoverClient discoverClient,
        CenterProperties centerProperties) {
        this.centerServer = centerServer;
        this.discoverClient = discoverClient;
        this.centerProperties = centerProperties;
        this.transportProperties = centerProperties.getTransport();
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(HanguCons.CPUS);
        if (Objects.isNull(this.transportProperties)) {
            this.transportProperties = new TransportProperties();
        }
        this.heartExpireTimes = transportProperties.getHeartbeatTimeRate()
            * transportProperties.getHeartbeatTimeOutCount() * 1000L;
        this.workExecutorService = workExecutorService;
    }

    @Override
    public List<RegistryInfo> lookup(LookupServer serverInfo) {
        String key = CommonUtils.createServiceKey(serverInfo);
        List<RegistryInfo> registryInfos = this.serviceKeyMapHostInfos.getOrDefault(key, Collections.emptyMap())
            .values().stream().collect(Collectors.toList());
        Long afterRegisterTime = serverInfo.getAfterRegisterTime();
        // 如果有指定注册时间，那么增量拉取注册的服务
        if (Objects.nonNull(afterRegisterTime) && afterRegisterTime > 0) {
            registryInfos = registryInfos.stream().filter(info -> info.getRegisterTime() >= afterRegisterTime)
                .collect(Collectors.toList());
        }
        return this.filterExpireRegistryInfos(registryInfos);
    }

    @Override
    public List<RegistryInfo> lookup() {
        List<RegistryInfo> registryInfoSet = this.serviceKeyMapHostInfos.values().stream()
            .flatMap(e -> e.values().stream())
            .collect(Collectors.toList());
        return this.filterExpireRegistryInfos(registryInfoSet);
    }

    @Override
    public List<RegistryInfo> lookupAfterTime(long registerTime) {
        List<RegistryInfo> registryInfos = this.lookup();
        return registryInfos.stream().filter(info -> info.getRegisterTime() >= registerTime)
            .collect(Collectors.toList());
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        this.bindLocalHost();
        // 从其他节点同步注册信息
        // 将自己注册到其他的节点上，用于监控在线的节点
        this.syncAndRegisterSelf();
        // 启动清理过期数据的任务
        this.startClearTask();
        // 启动从其他节点上增量同步注册信息的任务
        this.startDeltaSynOtherNodes();
    }

    private void bindLocalHost() {
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHost(NetUtil.getLocalhost().getHostAddress());
        hostInfo.setPort(this.centerProperties.getPort());
        this.hostInfo = hostInfo;
    }

    private void syncAndRegisterSelf() {

        String peerNodeHosts = this.centerProperties.getPeerNodeHosts();
        // 只有配置了有其他的节点，才会调用
        if (StringUtils.hasText(peerNodeHosts)) {
            // 从其他节点同步注册信息
            this.syncOtherNodesInfo();
            // 将自己注册到其他的节点上，用于监控在线的节点
            this.registerSelfToOtherNodes();
        } else {
            centerServer.setStatus(ServerStatusEnum.COMPLETE);
        }
    }

    private void startDeltaSynOtherNodes() {
        // 没20s从其他节点进行增量同步（同步的目的是为了避免因为网络问题，其他节点注册的信息没有推送过来，有时候网络原因，出现假死状态）
        // 话说有必要么，网络不通就会关闭连接，恢复连接的时候自动再增量同步下，应该会更好点吧
        this.scheduledExecutorService.schedule(this::deltaSynOtherNodes, 20, TimeUnit.SECONDS);
    }

    private void deltaSynOtherNodes() {
        // 时延2分钟，我们部署的时候尽量保证多个机器的时钟是差不多的
        long registerTime = System.currentTimeMillis() - TIME_DELAY;
        try {
            discoverClient.lookupAfterTime(registerTime);
        } catch (Exception e) {
            log.error("增量同步其他节点服务失败", e);
        }
    }

    private void syncOtherNodesInfo() {
        try {
            List<RegistryInfo> infos = Optional.ofNullable(discoverClient.lookup())
                .orElse(Collections.emptyList());
            infos.stream().forEach(this::register);
            centerServer.setStatus(ServerStatusEnum.COMPLETE);
        } catch (NoServerAvailableException e) {
            // 没有可用链接的时候，有可能该节点是第一个启动的，允许提供服务，也有可能其他的服务全tm都挂了，也要提供服务
            // 也有可能就这台机器连不同其他的节点（除非不是同一网段，一般可能性小）
            centerServer.setStatus(ServerStatusEnum.COMPLETE);
            log.error("从其他节点同步注册信息失败！原因：{}，2s后重试", e.getMessage(), e);
            this.scheduledExecutorService.schedule(this::syncOtherNodesInfo, 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("从其他节点同步注册信息失败！2s后重试", e);
            this.scheduledExecutorService.schedule(this::syncOtherNodesInfo, 2, TimeUnit.SECONDS);
        }
    }

    private void startClearTask() {
        this.scheduledExecutorService.schedule(this::doClearExpireData, 24, TimeUnit.HOURS);
    }

    private void doClearExpireData() {
        Map<String, RegistryInfo> removeRegistryInfoMap = new HashMap<>();
        synchronized (LOCK) {
            serviceKeyMapHostInfos.entrySet().stream().forEach(entry -> {
                Map<HostInfo, RegistryInfo> registryInfoMap = entry.getValue();
                List<HostInfo> removeHostInfos = new ArrayList<>();
                registryInfoMap.forEach((hostInfo, registryInfo) -> {
                    if (System.currentTimeMillis() > registryInfo.getExpireTime()) {
                        removeHostInfos.add(hostInfo);
                        removeRegistryInfoMap.put(CommonUtils.createServiceKey(registryInfo)
                            , registryInfo);
                    }
                });

                removeHostInfos.stream().forEach(registryInfoMap::remove);
            });
        }
        this.subscribeNotify(removeRegistryInfoMap.values().stream().collect(Collectors.toList()));
    }

    private void specifyClearExpireData(List<? extends ServerInfo> serverInfoList) {
        serverInfoList.stream().forEach(this::doSpecifyClearExpireData);
        this.subscribeNotify(serverInfoList);
    }

    private void doSpecifyClearExpireData(ServerInfo serverInfo) {
        String key = CommonUtils.createServiceKey(serverInfo);
        Map<HostInfo, RegistryInfo> registryInfoMap = serviceKeyMapHostInfos.get(key);
        if (CollectionUtils.isEmpty(registryInfoMap)) {
            return;
        }
        synchronized (LOCK) {
            List<HostInfo> removeHostInfos = new ArrayList<>();
            registryInfoMap.forEach((hostInfo, registryInfo) -> {
                if (System.currentTimeMillis() > registryInfo.getExpireTime()) {
                    removeHostInfos.add(hostInfo);
                }
            });
            removeHostInfos.stream().forEach(registryInfoMap::remove);
        }
    }

    public void register(RegistryInfo registryInfo) {
        this.doRegister(registryInfo, true);
    }

    public void syncRegistry(RegistryInfo registryInfo) {
        this.doRegister(registryInfo, false);
    }

    private void doRegister(RegistryInfo registryInfo, boolean sync) {
        String key = CommonUtils.createServiceKey(registryInfo);
        synchronized (LOCK) {
            Map<HostInfo, RegistryInfo> hostInfoRegistryInfoMap = serviceKeyMapHostInfos.get(key);
            if (Objects.isNull(hostInfoRegistryInfoMap)) {
                hostInfoRegistryInfoMap = new HashMap<>();
                serviceKeyMapHostInfos.put(key, hostInfoRegistryInfoMap);
            }
            // 如果是同步过来的，registerTime 和 expireTime 是会有值的
            Long registerTime = registryInfo.getRegisterTime();
            if(Objects.isNull(registerTime) || registerTime <= 0L) {
                registryInfo.setRegisterTime(System.currentTimeMillis());
            }
            Long expireTime = registryInfo.getExpireTime();
            if(Objects.isNull(expireTime) || expireTime <= 0L) {
                registryInfo.setExpireTime(System.currentTimeMillis() + this.heartExpireTimes);
            }
            hostInfoRegistryInfoMap.put(registryInfo.getHostInfo(), registryInfo);
            this.discoverClient.updateMaxRegistryTime(registryInfo.getRegisterTime());
        }
        // 向其他节点同步注册信息
        if(sync) {
            this.workExecutorService.submit(() -> {
                this.discoverClient.syncRegistry(registryInfo);
            });
        }

        this.subscribeNotify(Collections.singletonList(registryInfo));
    }

    public void unRegister(RegistryInfo registryInfo) {
        String key = CommonUtils.createServiceKey(registryInfo);
        Map<HostInfo, RegistryInfo> map = serviceKeyMapHostInfos.get(key);
        if(!CollectionUtils.isEmpty(map)) {
            map.remove(registryInfo.getHostInfo());
            this.subscribeNotify(Collections.singletonList(registryInfo));
        }
    }

    private void registerSelfToOtherNodes() {

        String peerNodeHosts = this.centerProperties.getPeerNodeHosts();
        if (StringUtils.hasText(peerNodeHosts)) {
            RegistryInfo registryInfo = new RegistryInfo();
            registryInfo.setGroupName(HanguCons.GROUP_NAME);
            registryInfo.setInterfaceName(HanguCons.INTERFACE_NAME);
            registryInfo.setVersion(HanguCons.VERSION);
            registryInfo.setCenter(true);
            registryInfo.setHostInfo(this.hostInfo);
            this.doRegisterSelfToOtherNodes(registryInfo);
        }
    }

    private void doRegisterSelfToOtherNodes(RegistryInfo registryInfo) {
        try {
            discoverClient.register(registryInfo);
        } catch (Exception e) {
            log.error("向其他节点注册自己（{}）失败！", registryInfo.getHostInfo(), e);
            // 过会再次尝试向其他节点注册自己
            this.scheduledExecutorService.schedule(() -> this.doRegisterSelfToOtherNodes(registryInfo),
                2, TimeUnit.SECONDS);
        }
    }

    /**
     * 过滤过期的数据
     *
     * @return
     */
    private List<RegistryInfo> filterExpireRegistryInfos(List<RegistryInfo> registryInfoSet) {
        Long currentTime = System.currentTimeMillis();
        List<RegistryInfo> effectiveList = registryInfoSet.stream()
            .filter(e -> currentTime <= e.getExpireTime())
            .collect(Collectors.toList());
        List<RegistryInfo> expireList = registryInfoSet.stream()
            .filter(e -> currentTime > e.getExpireTime())
            .collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(expireList)) {
            workExecutorService.submit(() -> {
                // 处理过期数据
                this.specifyClearExpireData(expireList);
            });
        }
        return effectiveList;
    }

    public void renew(List<RegistryInfo> registryInfoList) {
        Optional.ofNullable(registryInfoList).orElse(Collections.emptyList()).stream().forEach(registryInfo -> {
            String key = CommonUtils.createServiceKey(registryInfo);
            RegistryInfo exists = serviceKeyMapHostInfos.getOrDefault(key, Collections.emptyMap())
                .get(registryInfo.getHostInfo());
            if (Objects.nonNull(exists)) {
                exists.setExpireTime(System.currentTimeMillis() + this.heartExpireTimes);
            } else {
                this.register(registryInfo);
            }
        });
    }

    private void subscribeNotify(List<? extends ServerInfo> serverInfoList) {
        this.subscribeNotify(serverInfoList, null);
    }

    private void subscribeNotify(List<? extends ServerInfo> serverInfoList, List<Channel> nettyServers) {
        if(CollectionUtils.isEmpty(serverInfoList)) {
            return;
        }
        this.workExecutorService.submit(() -> {
            serverInfoList.stream().forEach(e -> this.doSubscribeNotify(e, nettyServers));
        });
    }

    private void doSubscribeNotify(ServerInfo serverInfo, List<Channel> nettyServers) {
        String key = CommonUtils.createServiceKey(serverInfo);
        List<Channel> nettyServerList = Objects.isNull(nettyServers)
            ? subscribeTable.getOrDefault(key, Collections.emptyMap()).values().stream().collect(Collectors.toList())
            : nettyServers;
        if (CollectionUtils.isEmpty(nettyServerList)) {
            return;
        }
        nettyServerList = nettyServerList.stream().filter(Channel::isActive).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(nettyServerList)) {
            return;
        }
        LookupServer lookupServer = new LookupServer();
        lookupServer.setGroupName(serverInfo.getGroupName());
        lookupServer.setVersion(serverInfo.getVersion());
        lookupServer.setInterfaceName(serverInfo.getInterfaceName());
        lookupServer.setAfterRegisterTime(0L);
        List<RegistryInfo> registryInfoList = this.lookup(lookupServer);
        Response response = this.buildNotifyResponse(registryInfoList);
        nettyServerList.stream().forEach(nettyServer -> {
            try {
                nettyServer.writeAndFlush(response);
            } catch (Exception e) {
                log.error("通知服务变更：groupName：{}，interfaceName：{}， version：{} 失败！",
                    serverInfo.getGroupName(), serverInfo.getInterfaceName(), serverInfo.getVersion());
            }
        });
    }

    private Response buildNotifyResponse(List<RegistryInfo> registryInfos) {
        Response response = new Response();
        response.setId(0L);
        response.setCommandType(CommandTypeMarkEnum.SINGLE_SUBSCRIBE_SERVICE.getType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        rpcResult.setResult(registryInfos);
        rpcResult.setReturnType(List.class);
        response.setRpcResult(rpcResult);
        return response;
    }

    public List<RegistryInfo> subscribe(Channel channel, ServerInfo serverInfo) {

        String key = CommonUtils.createServiceKey(serverInfo);
        while (Objects.nonNull(this.subscribeLock.putIfAbsent(key, LOCK))) {
            Thread.yield();
        }
        try {
            Map<ChannelId, Channel> nettyServers = this.subscribeTable.get(key);
            if(Objects.isNull(nettyServers)) {
                nettyServers = new HashMap<>();
                this.subscribeTable.put(key, nettyServers);
            }
            nettyServers.put(channel.id(), channel);

            Set<String> keySet = this.channelSubKeysTable.get(channel.id());
            if(Objects.isNull(keySet)) {
                keySet = new HashSet<>();
                this.channelSubKeysTable.put(channel.id(), keySet);
            }
            keySet.add(key);
        } finally {
            this.subscribeLock.remove(key);
        }

        LookupServer lookupServer = new LookupServer();
        lookupServer.setGroupName(serverInfo.getGroupName());
        lookupServer.setVersion(serverInfo.getVersion());
        lookupServer.setInterfaceName(serverInfo.getInterfaceName());
        lookupServer.setAfterRegisterTime(0L);
        return this.lookup(lookupServer);
    }

    public void unSubscribe(Channel channel, ServerInfo serverInfo) {
        String key = CommonUtils.createServiceKey(serverInfo);
        this.unSubscribe(channel, key, true);
    }

    public void unSubscribe(Channel channel, String key, boolean containChannelSubTab) {

        while (Objects.nonNull(this.subscribeLock.putIfAbsent(key, LOCK))) {
            Thread.yield();
        }
        try {
            Map<ChannelId, Channel> nettyServers = this.subscribeTable.get(key);
            if(Objects.nonNull(nettyServers)) {
                nettyServers.remove(channel.id());
            }
            if(containChannelSubTab) {
                Set<String> keySet = this.channelSubKeysTable.get(channel.id());
                if(Objects.nonNull(keySet)) {
                    keySet.remove(key);
                }
            }
        } finally {
            this.subscribeLock.remove(key);
        }
    }

    public void unRegistered(Channel channel) {
        Set<String> keySet = this.channelSubKeysTable.remove(channel.id());
        if(CollectionUtils.isEmpty(keySet)) {
            return;
        }
        keySet.stream().forEach(key -> this.unSubscribe(channel, key, false));
    }
}

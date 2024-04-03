package org.hangu.center.server.manager;

import cn.hutool.core.collection.CollectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.hangu.center.common.api.Close;
import org.hangu.center.common.api.Init;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.RegistryNotifyInfo;
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
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 服务注册列表
 *
 * @author wuzhenhong
 * @date 2023/7/31 15:07
 */
@Slf4j
public class ServiceRegisterManager implements Init, Close, LookupService {

    private static final int DEFAULT_SIZE = 1024;

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
        return registryInfos.stream().filter(info -> info.getRegisterTime() > registerTime)
            .collect(Collectors.toList());
    }


    @Override
    public void init() throws Exception {
        // 从其他节点同步注册信息
        // 将自己注册到其他的节点上，用于监控在线的节点
        this.syncAndRegisterSelf();
        // 启动清理过期数据的任务
        this.startClearTask();
    }

    private void syncAndRegisterSelf() {

        String peerNodeHosts = this.centerProperties.getPeerNodeHosts();
        List<String> ipAddressList = Arrays.stream(
                StringUtils.tokenizeToStringArray(peerNodeHosts, AbstractApplicationContext.CONFIG_LOCATION_DELIMITERS)).filter(StringUtils::hasText)
            .filter(ipAddress -> {
                String[] ipPort = ipAddress.split(":");
                if (ipPort.length == 2) {
                    HostInfo hostInfo = this.centerServer.getCenterHostInfo();
                    return (!hostInfo.getHost().equals(ipPort[0]) && !"localhost".equals(ipPort[0]))
                        || hostInfo.getPort()
                        != Integer.parseInt(ipPort[1]);
                }
                return true;
            }).collect(Collectors.toList());
        // 只有配置了有其他的节点，才会调用
        if (!CollectionUtils.isEmpty(ipAddressList)) {
            // 从其他节点同步注册信息
            this.syncOtherNodesInfo();
            // 将自己注册到其他的节点上，用于监控在线的节点
            this.registerSelfToOtherNodes();
        } else {
            centerServer.setStatus(ServerStatusEnum.COMPLETE);
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
        this.scheduledExecutorService.schedule(this::doClearExpireData, 10, TimeUnit.MINUTES);
        this.scheduledExecutorService.schedule(this::doClearInvalidSubChannel, 10, TimeUnit.MINUTES);
    }

    private void doClearInvalidSubChannel() {
        this.subscribeTable.entrySet().stream().forEach(entry -> {
            Map<ChannelId, Channel> map = entry.getValue();
            List<Channel> removeList = map.values().stream().filter(channel -> !channel.isActive())
                .collect(Collectors.toList());
            removeList.stream().forEach(channel -> this.unRegistered(channel));
        });
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
        // 对失效了的数据发布通知
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
        this.doRegister(registryInfo, true, true);
    }

    public void syncRegistry(RegistryInfo registryInfo) {
        this.doRegister(registryInfo, false, true);
    }

    private void doRegister(RegistryInfo registryInfo, boolean sync, boolean notify) {
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
        }
        // 向其他节点同步注册信息
        if(sync) {
            this.workExecutorService.submit(() -> {
                this.discoverClient.syncRegistry(registryInfo);
            });
        }

        if(notify) {
            this.subscribeNotify(Collections.singletonList(registryInfo));
        }
    }

    public void unRegister(RegistryInfo registryInfo) {
        this.doUnRegister(registryInfo, true);
    }

    public void syncUnRegistry(RegistryInfo registryInfo) {
        this.doUnRegister(registryInfo, false);
    }

    private void doUnRegister(RegistryInfo registryInfo, boolean sync) {
        String key = CommonUtils.createServiceKey(registryInfo);
        Map<HostInfo, RegistryInfo> map = serviceKeyMapHostInfos.get(key);
        if(!CollectionUtils.isEmpty(map)) {
            map.remove(registryInfo.getHostInfo());
            // 向其他节点同步注册信息
            if(sync) {
                this.workExecutorService.submit(() -> {
                    this.discoverClient.syncUnRegister(registryInfo);
                });
            }
            this.subscribeNotify(Collections.singletonList(registryInfo));
        }
    }

    private void registerSelfToOtherNodes() {

        String peerNodeHosts = this.centerProperties.getPeerNodeHosts();
        if (StringUtils.hasText(peerNodeHosts)) {
            RegistryInfo registryInfo = this.buildSelfRegistryInfo();
            this.doRegisterSelfToOtherNodes(registryInfo);
        }
    }

    private RegistryInfo buildSelfRegistryInfo() {
        RegistryInfo registryInfo = new RegistryInfo();
        registryInfo.setGroupName(HanguCons.GROUP_NAME);
        registryInfo.setInterfaceName(HanguCons.INTERFACE_NAME);
        registryInfo.setVersion(HanguCons.VERSION);
        registryInfo.setCenter(true);
        registryInfo.setHostInfo(this.centerServer.getCenterHostInfo());
        return registryInfo;
    }

    private void doRegisterSelfToOtherNodes(RegistryInfo registryInfo) {
        try {
            this.discoverClient.register(registryInfo);
            // 订阅集群节点的变化，随时调整集群成员的变更
            this.discoverClient.subscribe(registryInfo, this.discoverClient.getCenterNodeChangeNotify());
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

    public void renew(List<RegistryInfo> registryInfoList, boolean sync) {
        List<RegistryInfo> needSyncRenew = new ArrayList<>();
        Optional.ofNullable(registryInfoList).orElse(Collections.emptyList()).stream().forEach(registryInfo -> {
            String key = CommonUtils.createServiceKey(registryInfo);
            RegistryInfo exists = serviceKeyMapHostInfos.getOrDefault(key, Collections.emptyMap())
                .get(registryInfo.getHostInfo());
            if (Objects.nonNull(exists)) {
                exists.setExpireTime(System.currentTimeMillis() + this.heartExpireTimes);
                needSyncRenew.add(registryInfo);
            } else {
                // 可能因为网络问题或者其他的一些问题导致服务器上没有该api服务，那么
                // 就重新注册下
                 this.register(registryInfo);
//                this.doRegister(registryInfo, false, false);
            }
        });

        if(sync && CollectionUtil.isNotEmpty(needSyncRenew)) {
            // 续约，需要同步到其他节点，但不需要进行订阅通知
            this.workExecutorService.submit(() -> {
                this.discoverClient.syncRenew(needSyncRenew);
            });
        }
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

        RegistryNotifyInfo registryNotifyInfo = new RegistryNotifyInfo();
        registryNotifyInfo.setServerInfo(serverInfo);
        registryNotifyInfo.setRegistryInfos(registryInfoList);
        Response response = this.buildNotifyResponse(registryNotifyInfo);
        nettyServerList.stream().forEach(nettyServer -> {
            try {
                nettyServer.writeAndFlush(response);
            } catch (Exception e) {
                log.error("通知服务变更：groupName：{}，interfaceName：{}， version：{} 失败！",
                    serverInfo.getGroupName(), serverInfo.getInterfaceName(), serverInfo.getVersion());
            }
        });
    }

    private Response buildNotifyResponse(RegistryNotifyInfo registryNotifyInfo) {
        Response response = new Response();
        response.setId(0L);
        response.setCommandType(CommandTypeMarkEnum.SINGLE_SUBSCRIBE_SERVICE.getType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setCode(ErrorCodeEnum.SUCCESS.getCode());
        rpcResult.setResult(registryNotifyInfo);
        rpcResult.setReturnType(RegistryNotifyInfo.class);
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

    @Override
    public void close() throws Exception {
        this.discoverClient.unRegister(this.buildSelfRegistryInfo());
    }
}

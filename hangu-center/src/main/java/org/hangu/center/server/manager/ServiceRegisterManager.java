package org.hangu.center.server.manager;

import cn.hutool.core.net.NetUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.InstanceInfo;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.exception.RpcStarterException;
import org.hangu.center.common.properties.TransportProperties;
import org.hangu.center.common.util.CommonUtils;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.lookup.LookupService;
import org.hangu.center.server.properties.CenterProperties;
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

    /**
     * 接口与所在机器的host映射
     * key -> groupName + "/" + version + "/" + interfaceName
     * value -> 地址集合
     */
    private final Map<String, Set<RegistryInfo>> serviceKeyMapHostInfos = new ConcurrentHashMap<>(DEFAULT_SIZE);

    /**
     * 机器 -》 实例相关信息，比如是否仍有心跳之类的
     */
    private final Map<HostInfo, InstanceInfo> infoInstanceInfoMap = new ConcurrentHashMap<>(DEFAULT_SIZE);

    private DiscoverClient discoverClient;

    private TransportProperties transportProperties;

    private CenterProperties centerProperties;

    private ScheduledExecutorService scheduledExecutorService;

    private HostInfo hostInfo;

    public ServiceRegisterManager(DiscoverClient discoverClient, CenterProperties centerProperties) {
        this.discoverClient = discoverClient;
        this.centerProperties = centerProperties;
        this.transportProperties = centerProperties.getTransport();
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(HanguCons.CPUS);
        if(Objects.isNull(this.transportProperties)) {
            this.transportProperties = new TransportProperties();
        }
    }

    @Override
    public List<RegistryInfo> lookup(LookupServer serverInfo) {
        String key = CommonUtils.createServiceKey(serverInfo);
        Set<RegistryInfo> registryInfos = this.serviceKeyMapHostInfos.getOrDefault(key, Collections.emptySet());
        Long afterRegisterTime = serverInfo.getAfterRegisterTime();
        if (Objects.nonNull(afterRegisterTime) && afterRegisterTime > 0) {
            registryInfos = registryInfos.stream().filter(info -> info.getRegisterTime() >= afterRegisterTime)
                .collect(Collectors.toSet());
        }

        return registryInfos.stream().collect(Collectors.toList());
    }

    @Override
    public List<RegistryInfo> lookup() {
        return this.serviceKeyMapHostInfos.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<RegistryInfo> lookupAfterTime(long registerTime) {
        List<RegistryInfo> registryInfos = this.lookup();
        return registryInfos.stream().filter(info -> info.getRegisterTime() >= registerTime)
            .collect(Collectors.toList());
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        this.bindLocalhost();
        // 从其他节点同步注册信息
        // 将自己注册到其他的节点上，用于监控在线的节点
        this.syncAndRegisterSelf();
        // 启动清理过期数据的任务
        this.startClearTask();
        // 启动从其他节点上增量同步注册信息的任务
        this.startDeltaSynOtherNodes();
    }

    private void syncAndRegisterSelf() {

        String peerNodeHosts = this.centerProperties.getPeerNodeHosts();
        // 只有配置了有其他的节点，才会调用
        if(StringUtils.hasText(peerNodeHosts)) {
            // 从其他节点同步注册信息
            this.syncOtherNodesInfo();
            // 将自己注册到其他的节点上，用于监控在线的节点
            this.registerSelfToOtherNodes();
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
        } catch (RpcStarterException e) {
            log.error("从其他节点同步注册信息失败！原因：{}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("从其他节点同步注册信息失败！", e);
        }
    }

    private void startClearTask() {
        this.scheduledExecutorService.schedule(this::doClearExpireData, 24, TimeUnit.HOURS);
    }

    private void doClearExpireData() {
        synchronized (LOCK) {
            List<HostInfo> removeHostInfos = new ArrayList<>();
            serviceKeyMapHostInfos.entrySet().stream().forEach(entry -> {
                Set<RegistryInfo> registryInfoSet = entry.getValue();
                Iterator<RegistryInfo> iterator = registryInfoSet.iterator();
                while (iterator.hasNext()) {
                    RegistryInfo registryInfo = iterator.next();
                    InstanceInfo instanceInfo = infoInstanceInfoMap.get(registryInfo.getHostInfo());
                    if (Objects.isNull(instanceInfo)) {
                        iterator.remove();
                    } else {
                        if (System.currentTimeMillis() > instanceInfo.getExpireTime()) {
                            iterator.remove();
                            removeHostInfos.add(registryInfo.getHostInfo());
                        }
                    }
                }

            });
            removeHostInfos.stream().forEach(infoInstanceInfoMap::remove);
            List<String> removeKeys = serviceKeyMapHostInfos.entrySet().stream()
                .filter(entry -> CollectionUtils.isEmpty(entry.getValue()))
                .map(Entry::getKey).collect(Collectors.toList());
            removeKeys.stream().forEach(serviceKeyMapHostInfos::remove);
        }
    }

    public void register(RegistryInfo registryInfo) {
        String key = CommonUtils.createServiceKey(registryInfo);
        synchronized (LOCK) {
            Set<RegistryInfo> registryInfoSet = serviceKeyMapHostInfos.get(key);
            if (Objects.isNull(registryInfoSet)) {
                registryInfoSet = new HashSet<>();
                serviceKeyMapHostInfos.put(key, registryInfoSet);
            }
            registryInfo.setRegisterTime(System.currentTimeMillis());
            registryInfoSet.add(registryInfo);

            HostInfo hostInfo = registryInfo.getHostInfo();
            InstanceInfo instanceInfo = infoInstanceInfoMap.get(hostInfo);
            if (Objects.isNull(instanceInfo)) {
                instanceInfo = new InstanceInfo();
                instanceInfo.setHostInfo(hostInfo);
                instanceInfo.setExpireTime(System.currentTimeMillis() + (transportProperties.getHeartbeatTimeRate()
                    * transportProperties.getHeartbeatTimeOutCount() * 1000L));

                infoInstanceInfoMap.put(hostInfo, instanceInfo);
            }
        }
    }

    public void unRegister(RegistryInfo registryInfo) {
        String key = CommonUtils.createServiceKey(registryInfo);
        serviceKeyMapHostInfos.remove(key);
    }

    public void unRegister(HostInfo hostInfo) {
        infoInstanceInfoMap.remove(hostInfo);
    }

    private void registerSelfToOtherNodes() {

        String peerNodeHosts = this.centerProperties.getPeerNodeHosts();
        if(StringUtils.hasText(peerNodeHosts)) {
            RegistryInfo registryInfo = new RegistryInfo();
            registryInfo.setGroupName(HanguCons.GROUP_NAME);
            registryInfo.setInterfaceName(HanguCons.INTERFACE_NAME);
            registryInfo.setVersion(HanguCons.VERSION);
            registryInfo.setCenter(true);
            registryInfo.setHostInfo(this.hostInfo);
            this.doRegisterSelfToOtherNodes(registryInfo);
        }
    }

    private void bindLocalhost() {
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHost(NetUtil.getLocalhost().getHostAddress());
        hostInfo.setPort(centerProperties.getPort());
        this.hostInfo = hostInfo;
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
}

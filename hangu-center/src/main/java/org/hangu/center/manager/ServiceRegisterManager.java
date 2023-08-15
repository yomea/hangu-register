package org.hangu.center.manager;

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
import org.hangu.common.constant.HanguCons;
import org.hangu.common.entity.HostInfo;
import org.hangu.common.entity.InstanceInfo;
import org.hangu.common.entity.LookupServer;
import org.hangu.common.entity.RegistryInfo;
import org.hangu.common.entity.ServerInfo;
import org.hangu.common.properties.TransportProperties;
import org.hangu.common.util.CommonUtils;
import org.hangu.discover.client.DiscoverClient;
import org.hangu.discover.lookup.LookupService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

/**
 * 服务注册列表
 *
 * @author wuzhenhong
 * @date 2023/7/31 15:07
 */
public class ServiceRegisterManager implements InitializingBean, LookupService {

    private static final int DEFAULT_SIZE = 1024;

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

    private ScheduledExecutorService scheduledExecutorService;

    public ServiceRegisterManager(DiscoverClient discoverClient, TransportProperties transportProperties) {
        this.discoverClient = discoverClient;
        this.transportProperties = transportProperties;
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        // 从其他节点同步注册信息
        this.syncOtherNodesInfo();
        // 将自己注册到其他的节点上，用于监控在线的节点
        this.registerSelfToOtherNodes();
        // 启动清理过期数据的任务
        this.startClearTask();
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

    private void syncOtherNodesInfo() {
        List<RegistryInfo> infos = null;
        try {
            infos = Optional.ofNullable(discoverClient.lookup())
                .orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        infos.stream().forEach(this::register);
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
        RegistryInfo registryInfo = new RegistryInfo();
        registryInfo.setGroupName(HanguCons.GROUP_NAME);
        registryInfo.setInterfaceName(HanguCons.INTERFACE_NAME);
        registryInfo.setVersion(HanguCons.VERSION);
        registryInfo.setCenter(true);
        discoverClient.register(registryInfo);
    }
}

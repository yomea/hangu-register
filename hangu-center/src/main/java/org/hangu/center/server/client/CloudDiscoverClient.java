package org.hangu.center.server.client;

import cn.hutool.core.collection.CollectionUtil;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.listener.RegistryNotifyListener;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.properties.ClientProperties;

/**
 * @author wuzhenhong
 * @date 2024/3/20 16:34
 */
@Slf4j
public class CloudDiscoverClient extends DiscoverClient {

    private HostInfo cloudHostInfo;

    public CloudDiscoverClient(ClientProperties clientProperties, HostInfo cloudHostInfo) {
        super(clientProperties);
        this.cloudHostInfo = cloudHostInfo;
    }

    @Override
    public boolean isCenter() {
        return true;
    }

    @Override
    public boolean connectPeerNode(HostInfo hostInfo) {
        // 如果是本机就不需要链接
        if(this.cloudHostInfo.equals(hostInfo)) {
            log.warn("ip为{}的服务为本机，不需要链接！", hostInfo);
            return false;
        }
        return super.connectPeerNode(hostInfo);
    }

    @Override
    public RegistryNotifyListener getCenterNodeChangeNotify() {
        return registryNotifyInfos -> {
            if(CollectionUtil.isEmpty(registryNotifyInfos)) {
                return;
            }
            List<RegistryInfo> registryInfoList = registryNotifyInfos.stream().flatMap(e -> Optional.ofNullable(e.getRegistryInfos())
                .orElse(Collections.emptyList()).stream()).collect(Collectors.toList());
            List<HostInfo> hostInfoList = registryInfoList.stream()
                .filter(e -> !e.getHostInfo().equals(this.cloudHostInfo))
                .map(RegistryInfo::getHostInfo)
                .distinct().collect(Collectors.toList());
            this.connectManager.refreshCenterConnect(hostInfoList);
        };
    }
}

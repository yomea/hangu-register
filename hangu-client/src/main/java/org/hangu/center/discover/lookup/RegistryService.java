package org.hangu.center.discover.lookup;

import java.util.List;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.listener.RegistryNotifyListener;

/**
 * 注册服务
 * @author wuzhenhong
 * @date 2023/8/14 16:47
 */
public interface RegistryService {

    /**
     * 注册服务
     * @param registryInfo
     */
    void register(RegistryInfo registryInfo);

    void register(RegistryInfo registryInfo, Integer retryCount);

    /**
     * 取消注册
     * @param registryInfo
     */
    void unRegister(RegistryInfo registryInfo);

    void syncRegistry(RegistryInfo registryInfo);

    void syncUnRegister(RegistryInfo registryInfo);

    void subscribe(ServerInfo serverInfo, RegistryNotifyListener notifyListener);

    void syncRenew(List<RegistryInfo> registryInfos);
}

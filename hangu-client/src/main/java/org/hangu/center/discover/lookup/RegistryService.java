package org.hangu.center.discover.lookup;

import org.hangu.center.common.entity.RegistryInfo;

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

    /**
     * 取消注册
     * @param serverInfo
     */
    void unRegister(RegistryInfo serverInfo);

    void syncRegistry(RegistryInfo registryInfo);
}

package org.hangu.discover.lookup;

import org.hangu.common.entity.RegistryInfo;
import org.hangu.common.entity.ServerInfo;

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
}

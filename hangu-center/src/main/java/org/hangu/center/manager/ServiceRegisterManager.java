package org.hangu.center.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hangu.common.entity.HostInfo;
import org.hangu.common.entity.InstanceInfo;
import org.springframework.beans.factory.InitializingBean;

/**
 * 服务注册列表
 *
 * @author wuzhenhong
 * @date 2023/7/31 15:07
 */
public class ServiceRegisterManager implements InitializingBean {

    private static final int DEFAULT_SIZE = 1024;

    /**
     * 接口与所在机器的host映射
     * key -> groupName + "/" + version + "/" + interfaceName
     * value -> 地址集合
     */
    private final Map<String, List<HostInfo>> serviceKeyMapHostInfos = new ConcurrentHashMap<>(DEFAULT_SIZE);

    /**
     * 对应机器心跳过期时间
     */
    private final Map<HostInfo, InstanceInfo> instanceMapExpire = new ConcurrentHashMap<>(DEFAULT_SIZE);

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO: 2023/8/11 向其他节点同步注册信息
    }
}

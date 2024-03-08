package org.hangu.center.discover.lookup;

import java.util.List;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;

/**
 * 查找服务
 * @author wuzhenhong
 * @date 2023/8/14 10:56
 */
public interface LookupService {

    /**
     * 查找指定可用服务
     * @param lookupServer
     * @return
     */
    List<RegistryInfo> lookup(LookupServer lookupServer) throws Exception;

    /**
     * 拉取所有的服务
     * @return
     */
    List<RegistryInfo> lookup() throws Exception;

    /**
     * 指定某时间之后的注册的所有服务
     * @param registerTime
     * @return
     */
    List<RegistryInfo> lookupAfterTime(long registerTime) throws Exception;
}

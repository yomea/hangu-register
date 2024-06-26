package org.hangu.center.common.listener;

import java.util.List;
import org.hangu.center.common.entity.RegistryNotifyInfo;

/**
 * @author wuzhenhong
 * @date 2024/3/21 16:37
 */
@FunctionalInterface
public interface RegistryNotifyListener {

    void notify(List<RegistryNotifyInfo> registryNotifyInfos);
}

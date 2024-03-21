package org.hangu.center.discover.client;

import java.util.List;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.listener.RegistryNotifyListener;
import org.hangu.center.discover.entity.ClientOtherInfo;
import org.hangu.center.discover.lookup.LookupService;
import org.hangu.center.discover.lookup.RegistryService;

/**
 * @author wuzhenhong
 * @date 2024/3/20 17:01
 */
public interface Client extends LookupService, RegistryService, RegistryNotifyListener {

    ClientOtherInfo getClientOtherInfo();

    void subscribe(ServerInfo serverInfo, RegistryNotifyListener notifyListener);
}

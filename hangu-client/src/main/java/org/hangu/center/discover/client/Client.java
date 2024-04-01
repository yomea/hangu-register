package org.hangu.center.discover.client;

import org.hangu.center.common.api.Close;
import org.hangu.center.common.api.Init;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.listener.RegistryNotifyListener;
import org.hangu.center.discover.lookup.LookupService;
import org.hangu.center.discover.lookup.RegistryService;

/**
 * @author wuzhenhong
 * @date 2024/3/20 17:01
 */
public interface Client extends LookupService, RegistryService, RegistryNotifyListener, Init, Close {

    boolean isCenter();

    boolean connectPeerNode(HostInfo hostInfo);

    RegistryNotifyListener getCenterNodeChangeNotify();
}

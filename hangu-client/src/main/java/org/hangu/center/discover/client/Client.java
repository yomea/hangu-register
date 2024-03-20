package org.hangu.center.discover.client;

import org.hangu.center.discover.entity.ClientOtherInfo;
import org.hangu.center.discover.lookup.LookupService;
import org.hangu.center.discover.lookup.RegistryService;

/**
 * @author wuzhenhong
 * @date 2024/3/20 17:01
 */
public interface Client extends LookupService, RegistryService {

    ClientOtherInfo getClientOtherInfo();
}

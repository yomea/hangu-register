package org.hangu.center.server.client;

import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.properties.ClientProperties;

/**
 * @author wuzhenhong
 * @date 2024/3/20 16:34
 */
public class CloudDiscoverClient extends DiscoverClient {

    public CloudDiscoverClient(ClientProperties clientProperties) {
        super(clientProperties);
    }

    @Override
    public boolean isCenter() {
        return true;
    }
}

package org.hangu.center.server.client;

import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.entity.ClientOtherInfo;
import org.hangu.center.discover.properties.ClientProperties;

/**
 * @author wuzhenhong
 * @date 2024/3/20 16:34
 */
public class CloudDiscoverClient extends DiscoverClient {

    private ClientOtherInfo clientOtherInfo;

    public CloudDiscoverClient(ClientProperties clientProperties) {
        super(clientProperties);
        this.clientOtherInfo = new ClientOtherInfo(true, 0L);
    }

    public void updateMaxRegistryTime(long registryTime) {
        this.clientOtherInfo.setMaxRegistryTime(Math.max(registryTime,
            this.clientOtherInfo.getMaxRegistryTime()));
    }

    @Override
    public ClientOtherInfo getClientOtherInfo() {
        return clientOtherInfo;
    }
}

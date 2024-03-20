package org.hangu.center.server.client;

import java.util.List;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
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

    @Override
    public List<RegistryInfo> lookup(LookupServer lookupServer) throws Exception {
        return super.lookup(lookupServer);
    }

    @Override
    public List<RegistryInfo> lookup() throws Exception {
        return super.lookup();
    }

    @Override
    public List<RegistryInfo> lookupAfterTime(long registerTime) throws Exception {
        return super.lookupAfterTime(registerTime);
    }

    @Override
    public void register(RegistryInfo registryInfo) {
        super.register(registryInfo);
    }

    @Override
    public void unRegister(RegistryInfo registryInfo) {
        super.unRegister(registryInfo);
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

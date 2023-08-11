package org.hangu.center.configuration;

import org.hangu.center.manager.ServiceRegisterManager;
import org.hangu.common.properties.HanguProperties;
import org.hangu.center.server.CenterServer;
import org.hangu.discover.client.DiscoverClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HanguProperties.class)
public class HanguCenterAutoConfiguration {

    @Autowired
    private DiscoverClient discoverClient;

    @Autowired
    private HanguProperties hanguProperties;

    @Bean
    public CenterServer centerServer() {
        return new CenterServer(hanguProperties);
    }

    @Bean
    public ServiceRegisterManager serviceRegisterManager() {
        return new ServiceRegisterManager();
    }
}

package org.hangu.center.configuration;

import org.hangu.center.manager.ServiceRegisterManager;
import org.hangu.center.properties.CenterProperties;
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
@EnableConfigurationProperties(CenterProperties.class)
public class HanguCenterAutoConfiguration {

    @Autowired
    private DiscoverClient discoverClient;

    @Autowired
    private CenterProperties centerProperties;

    @Bean
    public CenterServer centerServer() {
        return new CenterServer(centerProperties);
    }

    @Bean
    public ServiceRegisterManager serviceRegisterManager() {
        return new ServiceRegisterManager(discoverClient, centerProperties.getTransport());
    }
}

package org.hangu.center.discover.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.config.ClientResponseHandlerConfig;
import org.hangu.center.discover.config.impl.ClientResponseHandlerConfigDefaultImpl;
import org.hangu.center.discover.properties.ClientProperties;
import org.hangu.center.discover.starter.CenterClientStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClientProperties.class)
public class HanguClientAutoConfiguration {

    @Autowired
    private ClientProperties clientProperties;

    @Bean
    public ClientResponseHandlerConfig responseHandlerConfig() {
        return new ClientResponseHandlerConfigDefaultImpl();
    }

    @Bean
    public DiscoverClient discoverClient(
        @Autowired Optional<List<ClientResponseHandlerConfig>> optionalResponseHandlerConfigs) {
        return CenterClientStarter.start(clientProperties,
            optionalResponseHandlerConfigs.orElse(Collections.emptyList()));
    }
}


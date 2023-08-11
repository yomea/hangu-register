package org.hangu.discover.configuration;

import org.hangu.common.properties.HanguProperties;
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
public class HanguClientAutoConfiguration {

    @Autowired
    private HanguProperties hanguProperties;

    @Bean
    public DiscoverClient discoverClient() {
        return new DiscoverClient(hanguProperties);
    }
}

package org.hangu.center.server.configuration;

import java.util.List;
import java.util.Optional;
import org.hangu.center.discover.properties.ClientProperties;
import org.hangu.center.server.config.RequestHandlerConfig;
import org.hangu.center.server.config.ResponseHandlerConfig;
import org.hangu.center.server.manager.CenterServerManager;
import org.hangu.center.server.properties.CenterProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CenterProperties.class})
public class HanguCenterAutoConfiguration {

    @Autowired
    private CenterProperties centerProperties;

    @Bean
    @ConfigurationProperties(prefix = "hangu.center")
    public ClientProperties clientProperties() {
        return new ClientProperties();
    }

    @Bean(destroyMethod = "close")
    public CenterServerManager centerServerManager(ClientProperties clientProperties,
        Optional<List<ResponseHandlerConfig>> optionalResponseHandlers,
        Optional<List<RequestHandlerConfig>> optionalRequestHandlers) throws Exception {
        CenterServerManager centerServerManager = new CenterServerManager();
        centerServerManager.start(this.centerProperties, clientProperties, optionalResponseHandlers,
            optionalRequestHandlers);
        return centerServerManager;
    }
}

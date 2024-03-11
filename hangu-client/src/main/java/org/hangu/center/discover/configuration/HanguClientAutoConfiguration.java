package org.hangu.center.discover.configuration;

import com.sun.org.apache.regexp.internal.RE;
import java.util.List;
import java.util.Optional;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.ResponseHandlerFactory;
import org.hangu.center.discover.bussiness.handler.impl.RenewResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.properties.ClientProperties;
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
    public DiscoverClient discoverClient() {
        return new DiscoverClient(clientProperties);
    }

    @Bean
    public RenewResponseHandler renewResponseHandler() {
        return new RenewResponseHandler();
    }

    @Bean
    public ResponseHandlerFactory responseHandlerFactory(@Autowired Optional<List<ResponseHandler>> optionalRequestHandlers) {

        return new ResponseHandlerFactory(optionalRequestHandlers);
    }
}

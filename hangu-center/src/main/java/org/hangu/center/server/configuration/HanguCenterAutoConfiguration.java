package org.hangu.center.server.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.properties.ThreadProperties;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.ResponseHandlerFactory;
import org.hangu.center.discover.bussiness.handler.impl.RenewResponseHandler;
import org.hangu.center.discover.properties.ClientProperties;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.bussiness.handler.RequestHandlerFactory;
import org.hangu.center.server.client.CloudDiscoverClient;
import org.hangu.center.server.config.RequestHandlerConfig;
import org.hangu.center.server.config.ResponseHandlerConfig;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.properties.CenterProperties;
import org.hangu.center.server.server.CenterServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CenterProperties.class, ClientProperties.class})
public class HanguCenterAutoConfiguration {

    @Autowired
    private CenterProperties centerProperties;

    @Autowired
    private ClientProperties clientProperties;

    @Bean
    public ExecutorService workExecutorService() {
        int coreNum = HanguCons.CPUS << 3;
        int maxNum = coreNum;
        ThreadProperties thread = centerProperties.getThread();
        if (Objects.nonNull(thread)) {
            coreNum = thread.getCoreNum() > 0 ? thread.getCoreNum() : coreNum;
            maxNum = thread.getMaxNum() > 0 ? thread.getMaxNum() : maxNum;
        }
        return new ThreadPoolExecutor(coreNum, maxNum, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
    }

    @Bean
    public CenterServer centerServer(ExecutorService workExecutorService) {
        return new CenterServer(centerProperties, workExecutorService);
    }

    @Bean
    public CloudDiscoverClient cloudDiscoverClient() {
        return new CloudDiscoverClient(clientProperties);
    }

    @Bean
    public ServiceRegisterManager serviceRegisterManager(ExecutorService workExecutorService, CenterServer centerServer, CloudDiscoverClient cloudDiscoverClient,
        @Autowired Optional<List<ResponseHandlerConfig>> optionalResponseHandlers,
        @Autowired Optional<List<RequestHandlerConfig>> optionalRequestHandlers) {
        ServiceRegisterManager serviceRegisterManager = new ServiceRegisterManager(workExecutorService, centerServer, cloudDiscoverClient, centerProperties);
        // 先注册
        List<ResponseHandler> responseHandlerList = optionalResponseHandlers.orElse(Collections.emptyList())
                .stream().flatMap(config -> config.config(serviceRegisterManager, cloudDiscoverClient).stream())
                .collect(Collectors.toList());

        ResponseHandlerFactory.registryHandlers(responseHandlerList);

        List<RequestHandler> requestHandlers = optionalRequestHandlers.orElse(Collections.emptyList())
            .stream().flatMap(config -> config.config(serviceRegisterManager).stream())
            .collect(Collectors.toList());
        RequestHandlerFactory.registryHandlers(requestHandlers);
        return serviceRegisterManager;
    }


}

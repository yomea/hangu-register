package org.hangu.center.discover.starter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.ResponseHandlerFactory;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.config.ClientResponseHandlerConfig;
import org.hangu.center.discover.config.impl.ClientResponseHandlerConfigDefaultImpl;
import org.hangu.center.discover.properties.ClientProperties;

/**
 * @author wuzhenhong
 * @date 2024/3/22 13:36
 */
public class CenterClientStarter {

    public static DiscoverClient start(ClientProperties clientProperties, List<ClientResponseHandlerConfig> configs)
        throws Exception {
        DiscoverClient discoverClient = new DiscoverClient(clientProperties);
        List<ResponseHandler> responseHandlers = Optional.ofNullable(configs).orElse(Collections.emptyList()).stream()
            .flatMap(config ->
                Optional.ofNullable(config.config(discoverClient)).orElse(Collections.emptyList()).stream())
            .collect(Collectors.toList());
        ResponseHandlerFactory.registryHandlers(responseHandlers);
        // 默认的响应处理
        ResponseHandlerFactory.registryHandlers(new ClientResponseHandlerConfigDefaultImpl().config(discoverClient));
        discoverClient.init();
        return discoverClient;
    }

    public static DiscoverClient start(ClientProperties clientProperties)
        throws Exception {
        DiscoverClient discoverClient = new DiscoverClient(clientProperties);
        ClientResponseHandlerConfig config = new ClientResponseHandlerConfigDefaultImpl();
        List<ResponseHandler> responseHandlers = config.config(discoverClient);
        ResponseHandlerFactory.registryHandlers(responseHandlers);
        discoverClient.init();
        return discoverClient;
    }
}

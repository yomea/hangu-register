package org.hangu.center.server.config.impl;

import java.util.ArrayList;
import java.util.List;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.impl.RenewResponseHandler;
import org.hangu.center.discover.bussiness.handler.impl.SubscribeNotifyResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.server.bussiness.handler.impl.RenewAndDeltaPullResponseHandler;
import org.hangu.center.server.config.ResponseHandlerConfig;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.springframework.stereotype.Component;

/**
 * @author wuzhenhong
 * @date 2024/3/21 20:33
 */
@Component
public class ResponseHandlerConfigDefaultImpl implements ResponseHandlerConfig {

    @Override
    public List<ResponseHandler> config(ServiceRegisterManager serviceRegisterManager, DiscoverClient discoverClient) {

        List<ResponseHandler> responseHandlers = new ArrayList<>();
        responseHandlers.add(new RenewResponseHandler());
        responseHandlers.add(new SubscribeNotifyResponseHandler(discoverClient));
        responseHandlers.add(new RenewAndDeltaPullResponseHandler(serviceRegisterManager));

        return responseHandlers;
    }
}

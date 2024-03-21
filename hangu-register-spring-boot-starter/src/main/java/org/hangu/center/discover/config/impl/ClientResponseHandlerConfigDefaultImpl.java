package org.hangu.center.discover.config.impl;

import java.util.ArrayList;
import java.util.List;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.impl.RenewResponseHandler;
import org.hangu.center.discover.bussiness.handler.impl.SubscribeNotifyResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.config.ClientResponseHandlerConfig;

/**
 * @author wuzhenhong
 * @date 2024/3/21 20:33
 */
public class ClientResponseHandlerConfigDefaultImpl implements ClientResponseHandlerConfig {

    @Override
    public List<ResponseHandler> config(DiscoverClient discoverClient) {

        List<ResponseHandler> responseHandlers = new ArrayList<>();
        responseHandlers.add(new RenewResponseHandler());
        responseHandlers.add(new SubscribeNotifyResponseHandler(discoverClient));

        return responseHandlers;
    }
}

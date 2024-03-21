package org.hangu.center.server.config.impl;

import java.util.ArrayList;
import java.util.List;
import org.hangu.center.discover.bussiness.handler.impl.RenewResponseHandler;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.bussiness.handler.impl.PullDeltaServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.PullServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.RegisterServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.RemoveServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.RenewAndDeltaPullResponseHandler;
import org.hangu.center.server.bussiness.handler.impl.RenewAndDeltaPullServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.RenewServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.SubscribeNotifyServerRequestHandler;
import org.hangu.center.server.bussiness.handler.impl.SyncRegisterServerRequestHandler;
import org.hangu.center.server.config.RequestHandlerConfig;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author wuzhenhong
 * @date 2024/3/21 20:40
 */
@Component
public class RequestHandlerConfigDefaultImpl implements RequestHandlerConfig {

    @Override
    public List<RequestHandler> config(ServiceRegisterManager serviceRegisterManager) {
        List<RequestHandler> requestHandlers = new ArrayList<>();
        requestHandlers.add(new PullDeltaServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new PullServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new RegisterServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new RemoveServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new RenewAndDeltaPullServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new RenewServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new SubscribeNotifyServerRequestHandler(serviceRegisterManager));
        requestHandlers.add(new SyncRegisterServerRequestHandler(serviceRegisterManager));

        return requestHandlers;
    }
}

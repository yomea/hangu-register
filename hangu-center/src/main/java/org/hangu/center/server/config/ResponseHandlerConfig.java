package org.hangu.center.server.config;

import java.util.List;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.server.manager.ServiceRegisterManager;

/**
 * @author wuzhenhong
 * @date 2024/3/21 20:48
 */
public interface ResponseHandlerConfig {

    List<ResponseHandler> config(ServiceRegisterManager serviceRegisterManager, DiscoverClient discoverClient);
}

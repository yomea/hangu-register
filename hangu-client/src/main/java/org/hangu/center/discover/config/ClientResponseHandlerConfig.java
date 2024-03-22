package org.hangu.center.discover.config;

import java.util.List;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.client.DiscoverClient;

/**
 * @author wuzhenhong
 * @date 2024/3/21 20:31
 */
@FunctionalInterface
public interface ClientResponseHandlerConfig {

    List<ResponseHandler> config(DiscoverClient discoverClient);

}

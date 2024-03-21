package org.hangu.center.server.config;

import java.util.List;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;

/**
 * @author wuzhenhong
 * @date 2024/3/21 20:39
 */
@FunctionalInterface
public interface RequestHandlerConfig {
    List<RequestHandler> config(ServiceRegisterManager serviceRegisterManager);
}

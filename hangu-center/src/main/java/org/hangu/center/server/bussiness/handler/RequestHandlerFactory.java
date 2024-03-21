package org.hangu.center.server.bussiness.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:25
 */
public class RequestHandlerFactory {

    public static final Map<Byte, RequestHandler> TYPE_HANDLER_MAP = new HashMap<>();

    public static void registryHandlers(List<RequestHandler> requestHandlers) {

        Optional.ofNullable(requestHandlers).orElse(Collections.emptyList())
            .stream().forEach(requestHandler -> {
                TYPE_HANDLER_MAP.put(requestHandler.support().getType(), requestHandler);
            });
    }

    public static final RequestHandler getRequestHandlerByType(byte type) {

        return TYPE_HANDLER_MAP.get(type);
    }

}

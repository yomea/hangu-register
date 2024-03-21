package org.hangu.center.discover.bussiness.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:25
 */
public class ResponseHandlerFactory {

    public static final Map<Byte, ResponseHandler> TYPE_HANDLER_MAP = new HashMap<>();

    public static void registryHandlers(List<ResponseHandler> requestHandlers) {
        Optional.ofNullable(requestHandlers).orElse(Collections.emptyList())
            .stream().forEach(requestHandler -> {
                TYPE_HANDLER_MAP.put(requestHandler.support().getType(), requestHandler);
            });
    }

    public static final ResponseHandler getResponseHandlerByType(byte type) {

        return TYPE_HANDLER_MAP.get(type);
    }

}

package org.hangu.center.server.bussiness.handler.impl;

import io.netty.channel.Channel;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;

/**
 * 订阅通知
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
public class UnRegisteredServerRequestHandler implements RequestHandler<Object> {

    private ServiceRegisterManager serviceRegisterManager;

    public UnRegisteredServerRequestHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.UN_REGISTERED_SERVICE;
    }

    @Override
    public Response handler(Request<Object> request, NettyServer nettyServer, Channel channel) {
        // 拉取服务列表
        serviceRegisterManager.unRegistered(channel);
        return null;
    }
}

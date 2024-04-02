package org.hangu.center.server.bussiness.handler.impl;

import io.netty.channel.Channel;
import org.hangu.center.common.entity.RegistryInfoDirectory;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.manager.ServiceRegisterManager;
import org.hangu.center.server.server.NettyServer;

/**
 * 同步续约
 * @author wuzhenhong
 * @date 2023/8/14 16:24
 */
public class SyncRenewServerRequestHandler implements RequestHandler<RegistryInfoDirectory> {

    private ServiceRegisterManager serviceRegisterManager;

    public SyncRenewServerRequestHandler(ServiceRegisterManager serviceRegisterManager) {
        this.serviceRegisterManager = serviceRegisterManager;
    }

    @Override
    public CommandTypeMarkEnum support() {
        return CommandTypeMarkEnum.SYNC_RENEW_SERVICE;
    }

    @Override
    public Response handler(Request<RegistryInfoDirectory> request, NettyServer nettyServer, Channel channel) {

        RegistryInfoDirectory directory = request.getBody();
        serviceRegisterManager.renew(directory.getRegistryInfoList(), false);
        return null;
    }
}

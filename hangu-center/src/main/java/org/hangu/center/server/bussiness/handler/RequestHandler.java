package org.hangu.center.server.bussiness.handler;

import io.netty.channel.Channel;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.server.server.NettyServer;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:22
 */
public interface RequestHandler<T> {

    CommandTypeMarkEnum support();

    Response handler(Request<T> request, NettyServer nettyServer, Channel channel);
}

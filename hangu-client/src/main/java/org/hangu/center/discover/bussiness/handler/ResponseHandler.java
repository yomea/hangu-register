package org.hangu.center.discover.bussiness.handler;

import org.hangu.center.common.entity.Response;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.discover.client.DiscoverClient;
import org.hangu.center.discover.client.NettyClient;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:22
 */
public interface ResponseHandler {

    CommandTypeMarkEnum support();

    void handler(Response response, NettyClient nettyClient);
}

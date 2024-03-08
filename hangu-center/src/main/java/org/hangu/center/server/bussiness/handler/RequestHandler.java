package org.hangu.center.server.bussiness.handler;

import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.enums.CommandTypeMarkEnum;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:22
 */
public interface RequestHandler<T> {

    CommandTypeMarkEnum support();

    Response handler(Request<T> request);
}

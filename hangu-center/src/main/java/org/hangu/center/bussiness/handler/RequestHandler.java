package org.hangu.center.bussiness.handler;

import org.hangu.common.entity.Request;
import org.hangu.common.entity.Response;
import org.hangu.common.enums.CommandTypeMarkEnum;

/**
 * @author wuzhenhong
 * @date 2023/8/14 16:22
 */
public interface RequestHandler<T> {

    CommandTypeMarkEnum support();

    Response handler(Request<T> request);
}

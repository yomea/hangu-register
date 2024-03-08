package org.hangu.center.common.entity;

import java.io.Serializable;
import lombok.Data;
import org.hangu.center.common.enums.ErrorCodeEnum;

/**
 * Created by wuzhenhong on 2023/8/1 23:07
 */
@Data
public class RpcResult implements Serializable {

    /**
     * @see ErrorCodeEnum
     */
    private int code;

    private Class<?> returnType;

    private Object result;
}

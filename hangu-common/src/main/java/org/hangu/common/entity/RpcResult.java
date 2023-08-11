package org.hangu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/1 23:07
 */
@Data
public class RpcResult implements Serializable {

    /**
     * @see org.hangu.common.enums.ErrorCodeEnum
     */
    private int code;

    private Class<?> returnType;

    private Object result;
}

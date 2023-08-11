package org.hangu.common.exception;

import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/2 00:22
 */
@Data
public class RpcInvokerException extends RuntimeException {

    private int code;

    public RpcInvokerException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public RpcInvokerException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }
}

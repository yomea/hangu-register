package org.hangu.common.exception;

/**
 * @author wuzhenhong
 * @date 2023/8/9 11:18
 */
public class RpcInvokerTimeoutException extends RpcInvokerException{

    public RpcInvokerTimeoutException(int code, String msg) {
        super(code, msg);
    }

    public RpcInvokerTimeoutException(int code, String msg, Throwable e) {
        super(code, msg, e);
    }
}

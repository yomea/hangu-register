package org.hangu.center.common.exception;

import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/2 00:22
 */
@Data
public class ServerNodeUnCompleteException extends RuntimeException {

    private int code;

    /**
     * @see org.hangu.center.common.enums.ServerStatusEnum
     */
    private Integer status;

    public ServerNodeUnCompleteException(int code, int status, String msg) {
        super(msg);
        this.code = code;
        this.status = status;
    }

    public ServerNodeUnCompleteException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.status = status;
    }
}

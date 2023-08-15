package org.hangu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:29
 */
@Data
public class Request<T> implements Serializable {

    /**
     * 请求ID，8字节
     */
    private Long id;

    /**
     * @see org.hangu.common.enums.MsgTypeMarkEnum
     */
    private boolean oneWay;

    /**
     * @see org.hangu.common.enums.CommandTypeMarkEnum
     */
    private byte commandType;

    /**
     * body
     */
    private T body;
}

package org.hangu.center.common.entity;

import java.io.Serializable;
import lombok.Data;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.MsgTypeMarkEnum;

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
     * @see MsgTypeMarkEnum
     */
    private boolean oneWay;

    /**
     * @see CommandTypeMarkEnum
     */
    private byte commandType;

    /**
     * body
     */
    private T body;
}

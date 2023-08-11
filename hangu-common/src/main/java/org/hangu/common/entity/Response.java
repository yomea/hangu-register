package org.hangu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 请求响应
 *
 * @author wuzhenhong
 * @date 2023/8/1 11:00
 */
@Data
public class Response implements Serializable {

    private Long id;

    private RpcResult rpcResult;
}

package org.hangu.center.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:31
 */
@AllArgsConstructor
@Getter
public enum ErrorCodeEnum {

    SUCCESS(200, "请求成功！"),
    FAILURE(500, "调用错误！"),
    
    TIME_OUT(502, "调用超时！"),
    FORBID(403, "禁止访问！"),
    NOT_FOUND(404, "服务不存在!");


    private Integer code;

    private String desc;
}

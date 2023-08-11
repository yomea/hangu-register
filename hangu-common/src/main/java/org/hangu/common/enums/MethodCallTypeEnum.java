package org.hangu.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuzhenhong
 * @date 2023/8/3 16:39
 */
@AllArgsConstructor
@Getter
public enum MethodCallTypeEnum {

    SYNC(0, "同步调用"),
    ASYNC_PARAMETER(1, "异步参数回调"),
    ASYNC_SPECIFY(2, "异步指定回调类回调");

    private Integer type;

    private String desc;
}

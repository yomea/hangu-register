package org.hangu.center.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuzhenhong
 * @date 2024/3/11 8:55
 */
@Getter
@AllArgsConstructor
public enum ServerStatusEnum {
    UN_KNOW(-2, "未知"),
    STOP(-1, "停止"),
    READY(1, "准备"),
    COMPLETE(2, "完成");

    private Integer status;

    private String desc;

    public static ServerStatusEnum getEnumByStatus(Integer status) {
        for(ServerStatusEnum statusEnum : values()) {
            if(statusEnum.status.equals(status)) {
                return statusEnum;
            }
        }
        return ServerStatusEnum.UN_KNOW;
    }
}

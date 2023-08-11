package org.hangu.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型掩码
 *
 * @author wuzhenhong
 * @date 2023/7/31 16:47
 */
@AllArgsConstructor
@Getter
public enum MsgTypeMarkEnum {

    REQUEST_FLAG(1, (byte) 0x80, "请求位标记，高位置为1：表示请求，0：表示响应"),
    HEART_FLAG(2, (byte) 0x40, "心跳标记位，1：表示是心跳"),
    WAY_FLAG(3, (byte) 0x20, "是否需要回应标记位，1：表示需要响应，0：表示不回应"),

    ;

    private Integer type;

    private byte mark;

    private String desc;

    public static byte getMarkByType(Integer type) {
        for (MsgTypeMarkEnum markEnum : values()) {
            if (markEnum.getType().equals(type)) {
                return markEnum.getMark();
            }
        }
        throw new RuntimeException("不支持的消息类型！");
    }
}

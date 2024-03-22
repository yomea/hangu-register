package org.hangu.center.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 命令标记
 * @author wuzhenhong
 * @date 2023/8/14 13:21
 */
@AllArgsConstructor
@Getter
public enum CommandTypeMarkEnum {

    BATCH_REGISTER_SERVICE((byte) 1, "批量注册服务"),
    PULL_SERVICE((byte) 2, "拉取服务列表"),

    DELTA_PULL_SERVICE((byte) 3, "增量拉取服务列表"),

    BATCH_REMOVE_SERVICE((byte) 4, "注销服务"),

    RENEW_SERVICE((byte) 5, "续约"),

    RENEW_AND_DELTA_PULL_SERVICE((byte) 6, "续约并增量拉取服务"),
    BATCH_SYNC_REGISTER_SERVICE((byte) 7, "批量同步注册服务"),
    NOTIFY_REGISTER_SERVICE((byte) 8, "通知服务"),
    UN_SUBSCRIBE_SERVICE((byte) 9, "取消订阅服务"),
    UN_REGISTERED_SERVICE((byte) 10, "通道注销"),
    ;

    private byte type;

    private String desc;
}

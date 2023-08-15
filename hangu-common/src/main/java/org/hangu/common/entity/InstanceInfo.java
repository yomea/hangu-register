package org.hangu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/11 16:04
 */
@Data
public class InstanceInfo implements Serializable {

    /**
     * 过期时间，该值随着心跳更新
     */
    private Long expireTime;

    /**
     * 该实例地址
     */
    private HostInfo hostInfo;
}

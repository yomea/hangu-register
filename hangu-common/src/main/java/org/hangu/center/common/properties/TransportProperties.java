package org.hangu.center.common.properties;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/14 11:10
 */
@Data
public class TransportProperties {

    /**
     * 每隔多久心跳一次，默认2s
     */
    private int heartbeatTimeRate;

    /**
     * 心跳超时倍率，表示几倍的心跳表示超时，默认 3 倍
     * {@link #heartbeatTimeRate * 3}
     */
    private int heartbeatTimeOutCount;

    /**
     * 拉取服务列表超时时间
     */
    private int pullServiceListTimeout;

    /**
     * 注册服务超时时间
     */
    private int registryServiceTimeout;

}

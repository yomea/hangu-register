package org.hangu.center.discover.properties;

import lombok.Data;
import org.hangu.center.common.properties.TransportProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/14 13:36
 */
@ConfigurationProperties(prefix = "hangu.center")
@Data
public class ClientProperties {

    /**
     * 配置中心节点地址 127.0.0.1:8089,192.168.218.175:8090
     */
    private String peerNodeHosts;

    private TransportProperties transport;
}

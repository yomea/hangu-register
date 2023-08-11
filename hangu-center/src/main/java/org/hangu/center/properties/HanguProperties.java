package org.hangu.center.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/3 18:18
 */
@ConfigurationProperties(prefix = "hangu.rpc")
@Data
public class HanguProperties {

    /**
     * 注册中心暴露的端口号
     */
    private int port;

    /**
     * 其他节点url
     */
    private String peerNodeUrls;

    /**
     * 处理请求线程配置
     */
    private ThreadProperties thread;
}

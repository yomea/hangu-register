package org.hangu.common.properties;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/11 16:30
 */
@Data
public class ThreadProperties {

    /**
     * 注册中心处理请求的核心线程数量
     */
    private int coreNum;

    /**
     * 注册中心处理请求的最大线程数量
     */
    private int maxNum;

}

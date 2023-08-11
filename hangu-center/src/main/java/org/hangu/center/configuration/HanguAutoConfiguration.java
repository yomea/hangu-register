package org.hangu.center.configuration;

import org.hangu.center.properties.HanguProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HanguProperties.class)
public class HanguAutoConfiguration {

    @Autowired
    private HanguProperties hanguProperties;
}

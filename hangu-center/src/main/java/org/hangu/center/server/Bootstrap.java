package org.hangu.center.server;

import org.hangu.center.common.constant.HanguCons;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author wuzhenhong
 */

@SpringBootApplication
public class Bootstrap {

    public static void main(String[] args) {
        System.setProperty(HanguCons.CENTER_NODE_MARK, "true");
        SpringApplication.run(Bootstrap.class, args);
    }
}
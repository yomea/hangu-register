package org.hangu.common.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/14 15:44
 */
@Data
public class LookupServer extends ServerInfo{

    /**
     * 拉取该时间之后注册的服务
     */
    private Long afterRegisterTime;
}

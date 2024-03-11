package org.hangu.center.common.entity;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2024/3/11 19:20
 */
@Data
public class RegistryInfoDirectory implements Serializable {

    /**
     * 注册时间，注册中心自动赋值，客户端赋值无效
     */
    private Long registerTime;

    private List<RegistryInfo> registryInfoList;
}

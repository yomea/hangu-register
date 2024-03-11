package org.hangu.center.common.entity;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2024/3/11 11:28
 */
@Data
public class CenterNodeInfo implements Serializable {

    /**
     * @see org.hangu.center.common.enums.ServerStatusEnum
     */
    private Integer status;

    private List<RegistryInfo> infoList;
}

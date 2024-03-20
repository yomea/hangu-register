package org.hangu.center.discover.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wuzhenhong
 * @date 2024/3/20 16:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientOtherInfo {

    private boolean center;

    private Long maxRegistryTime;
}

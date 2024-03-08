package org.hangu.center.common.entity;

import java.util.Objects;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:44
 */
@Data
public class RegistryInfo extends ServerInfo {

    /**
     * 注册时间，注册中心自动赋值，客户端赋值无效
     */
    private Long registerTime;

    /**
     * 标记该注册数据为注册中心自己注册自己
     */
    private boolean center;

    /**
     * 服务提供者所在的地址
     */
    private HostInfo hostInfo;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RegistryInfo)) {
            return false;
        }
        RegistryInfo other = (RegistryInfo) obj;
        return super.equals(obj) && Objects.equals(this.hostInfo, other.getHostInfo());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(this.hostInfo);
    }
}

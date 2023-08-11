package org.hangu.common.entity;

import java.util.Objects;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:44
 */
@Data
public class RegistryInfo extends ServerInfo {

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

package org.hangu.common.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:13
 */
@Data
public class HostInfo implements Serializable {

    private String host;

    private int port;

    public String toString() {
        return this.host + ":" + this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HostInfo)) {
            return false;
        }
        HostInfo other = (HostInfo) obj;
        return
            Objects.equals(this.host, other.getHost())
                &&
                Objects.equals(this.port, other.getPort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.port);
    }


}

package org.hangu.center.common.entity;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class RegistryNotifyInfo implements Serializable {

    private ServerInfo serverInfo;

    private List<RegistryInfo> registryInfos;

}
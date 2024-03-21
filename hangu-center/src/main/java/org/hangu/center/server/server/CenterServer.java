package org.hangu.center.server.server;

import java.util.concurrent.ExecutorService;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.server.properties.CenterProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author wuzhenhong
 * @date 2023/8/11 17:28
 */
public class CenterServer implements InitializingBean, DisposableBean {

    private NettyServer nettyServer;
    private CenterProperties centerProperties;

    private ExecutorService executor;

    public CenterServer(CenterProperties centerProperties, ExecutorService executor) {
        this.centerProperties = centerProperties;
        this.executor = executor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.initServer();
        this.nettyServer.setStatus(ServerStatusEnum.READY);
    }

    @Override
    public void destroy() throws Exception {
        nettyServer.close();
        executor.shutdown();
    }

    private void initServer() {
        this.nettyServer = new NettyServer();
        nettyServer.start(this.centerProperties, executor);
    }

    public ServerStatusEnum getStatus() {
        return this.nettyServer.getStatus();
    }

    public void setStatus(ServerStatusEnum status) {
        this.nettyServer.setStatus(status);
    }
}

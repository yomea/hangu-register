package org.hangu.center.server.server;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.hangu.center.server.properties.CenterProperties;
import org.hangu.center.common.properties.ThreadProperties;
import org.hangu.center.common.constant.HanguCons;
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

    public CenterServer(CenterProperties centerProperties) {
        this.centerProperties = centerProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.initServer();
    }

    @Override
    public void destroy() throws Exception {
        nettyServer.close();
        executor.shutdown();
    }

    private void initServer() {
        int coreNum = HanguCons.CPUS << 3;
        int maxNum = coreNum;
        ThreadProperties thread = centerProperties.getThread();
        if (Objects.nonNull(thread)) {
            coreNum = thread.getCoreNum() > 0 ? thread.getCoreNum() : coreNum;
            maxNum = thread.getMaxNum() > 0 ? thread.getMaxNum() : maxNum;
        }
        this.executor = new ThreadPoolExecutor(coreNum, maxNum, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        this.nettyServer = new NettyServer();
        nettyServer.start(this.centerProperties, executor);
    }

}

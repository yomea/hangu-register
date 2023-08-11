package org.hangu.discover.client;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.hangu.common.constant.HanguCons;
import org.hangu.common.properties.HanguProperties;
import org.hangu.common.properties.ThreadProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author wuzhenhong
 * @date 2023/8/11 17:42
 */
public class DiscoverClient implements InitializingBean, DisposableBean {

    private NettyClient nettyClient;

    private ExecutorService executor;
    private HanguProperties hanguProperties;

    public DiscoverClient(HanguProperties hanguProperties) {
        this.hanguProperties = hanguProperties;
    }

    @Override
    public void destroy() throws Exception {
        nettyClient.close();
        executor.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        int coreNum = HanguCons.CPUS << 3;
        int maxNum = coreNum;
        ThreadProperties thread = hanguProperties.getThread();
        if (Objects.nonNull(thread)) {
            coreNum = thread.getCoreNum() > 0 ? thread.getCoreNum() : coreNum;
            maxNum = thread.getMaxNum() > 0 ? thread.getMaxNum() : maxNum;
        }
        this.executor = new ThreadPoolExecutor(coreNum, maxNum, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));

        nettyClient = new NettyClient();
        nettyClient.start(this.executor);
    }
}

package org.hangu.center.server.manager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hangu.center.common.api.Close;
import org.hangu.center.common.api.Init;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.properties.ThreadProperties;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.ResponseHandlerFactory;
import org.hangu.center.discover.properties.ClientProperties;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.bussiness.handler.RequestHandlerFactory;
import org.hangu.center.server.client.CloudDiscoverClient;
import org.hangu.center.server.config.RequestHandlerConfig;
import org.hangu.center.server.config.ResponseHandlerConfig;
import org.hangu.center.server.properties.CenterProperties;
import org.hangu.center.server.server.CenterServer;

/**
 * @author wuzhenhong
 * @date 2024/3/25 9:59
 */
public class CenterServerManager implements Init, Close {

    private CenterServer centerServer;
    private ServiceRegisterManager serviceRegisterManager;
    private ExecutorService workExecutorService;
    private CloudDiscoverClient cloudDiscoverClient;

    public void start(CenterProperties centerProperties, ClientProperties clientProperties,
        Optional<List<ResponseHandlerConfig>> optionalResponseHandlers,
        Optional<List<RequestHandlerConfig>> optionalRequestHandlers) throws Exception {
        this.workExecutorService = this.workExecutorService(centerProperties);
        this.doStart(this.workExecutorService, centerProperties, clientProperties, optionalResponseHandlers,
            optionalRequestHandlers);
    }

    @Override
    public void init() throws Exception {
        // 先启动客户端链接，链接到其他的节点
        this.cloudDiscoverClient.init();
        // 启动当前节点服务
        this.centerServer.init();
        // 启动注册服务
        this.serviceRegisterManager.init();

    }

    @Override
    public void close() throws Exception {
        this.workExecutorService.shutdown();
        this.cloudDiscoverClient.close();
        this.centerServer.close();
    }

    private ExecutorService workExecutorService(CenterProperties centerProperties) {
        int coreNum = HanguCons.CPUS << 3;
        int maxNum = coreNum;
        ThreadProperties thread = centerProperties.getThread();
        if (Objects.nonNull(thread)) {
            coreNum = thread.getCoreNum() > 0 ? thread.getCoreNum() : coreNum;
            maxNum = thread.getMaxNum() > 0 ? thread.getMaxNum() : maxNum;
        }
        return new ThreadPoolExecutor(coreNum, maxNum, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
    }

    private void doStart(ExecutorService workExecutorService, CenterProperties centerProperties,
        ClientProperties clientProperties, Optional<List<ResponseHandlerConfig>> optionalResponseHandlers,
        Optional<List<RequestHandlerConfig>> optionalRequestHandlers) throws Exception {

        this.cloudDiscoverClient = new CloudDiscoverClient(clientProperties);
        this.centerServer = new CenterServer(centerProperties, workExecutorService);
        this.serviceRegisterManager = new ServiceRegisterManager(workExecutorService, centerServer, cloudDiscoverClient,
            centerProperties);
        // 注册响应处理器
        List<ResponseHandler> responseHandlerList = optionalResponseHandlers.orElse(Collections.emptyList()).stream()
            .flatMap(config -> config.config(serviceRegisterManager, cloudDiscoverClient).stream())
            .collect(Collectors.toList());
        ResponseHandlerFactory.registryHandlers(responseHandlerList);

        // 注册请求处理器
        List<RequestHandler> requestHandlers = optionalRequestHandlers.orElse(Collections.emptyList()).stream()
            .flatMap(config -> config.config(serviceRegisterManager).stream()).collect(Collectors.toList());
        RequestHandlerFactory.registryHandlers(requestHandlers);

        // 初始化启动
        this.init();
    }
}

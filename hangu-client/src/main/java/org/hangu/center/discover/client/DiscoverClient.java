package org.hangu.center.discover.client;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultPromise;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.RegistryInfoDirectory;
import org.hangu.center.common.entity.RegistryNotifyInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.common.exception.NoServerAvailableException;
import org.hangu.center.common.exception.RpcInvokerException;
import org.hangu.center.common.exception.RpcInvokerTimeoutException;
import org.hangu.center.common.exception.RpcStarterException;
import org.hangu.center.common.exception.ServerNodeUnCompleteException;
import org.hangu.center.common.listener.RegistryNotifyListener;
import org.hangu.center.common.properties.TransportProperties;
import org.hangu.center.common.util.CommonUtils;
import org.hangu.center.discover.manager.CenterConnectManager;
import org.hangu.center.discover.manager.NettyClientEventLoopManager;
import org.hangu.center.discover.manager.RpcRequestManager;
import org.hangu.center.discover.properties.ClientProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/11 17:42
 */
@Slf4j
public class DiscoverClient implements Client {

    private final Object  lock = new Object();
    private ClientProperties clientProperties;

    protected CenterConnectManager connectManager;

    private Map<String, RegistryInfo> registryInfoTable = new ConcurrentHashMap<>();
    private Map<String, List<RegistryNotifyListener>> keyMapListenerMap = new HashMap<>();
    private Map<String, Object> keyMapListenerLockMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService workExecutorService;

    public DiscoverClient(ClientProperties clientProperties) {
        this(clientProperties, null);
    }

    public DiscoverClient(ClientProperties clientProperties, ExecutorService executorService) {
        if(Objects.isNull(executorService)) {
            executorService = new ThreadPoolExecutor(HanguCons.CPUS << 3, HanguCons.CPUS << 3,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1024));
        }
        this.clientProperties = clientProperties;
        this.workExecutorService = executorService;
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(HanguCons.CPUS);
        this.connectManager = new CenterConnectManager(this, this.clientProperties, this.workExecutorService,
            this.scheduledExecutorService,
            this.isCenter());
    }

    @Override
    public List<RegistryInfo> lookup(LookupServer serverInfo) throws Exception {

        return doLookupService(() -> {
            Request<ServerInfo> request = new Request<>();
            request.setId(CommonUtils.snowFlakeNextId());
            request.setCommandType(CommandTypeMarkEnum.PULL_SERVICE.getType());
            request.setBody(serverInfo);
            return request;
        });
    }

    @Override
    public List<RegistryInfo> lookup() throws Exception {
        // 啥都没有指定，表示拉取所有的服务
        return this.lookup(new LookupServer());
    }

    @Override
    public List<RegistryInfo> lookupAfterTime(long registerTime) throws Exception {
        return this.doLookupService(() -> {
            Request<Long> request = new Request<>();
            request.setId(CommonUtils.snowFlakeNextId());
            request.setCommandType(CommandTypeMarkEnum.DELTA_PULL_SERVICE.getType());
            request.setBody(registerTime);
            return request;
        });
    }

    @Override
    public void register(RegistryInfo registryInfo) {
        this.doRegister(registryInfo, CommandTypeMarkEnum.BATCH_REGISTER_SERVICE, 1);
    }

    @Override
    public void register(RegistryInfo registryInfo, Integer retryCount) {
        this.doRegister(registryInfo, CommandTypeMarkEnum.BATCH_REGISTER_SERVICE, retryCount);
    }

    @Override
    public void unRegister(RegistryInfo registryInfo) {
        this.doUnRegister(registryInfo, CommandTypeMarkEnum.BATCH_REMOVE_SERVICE, 1);
    }

    @Override
    public void syncRegistry(RegistryInfo registryInfo) {
        List<NettyClient> nettyClientList = Optional.ofNullable(this.connectManager.getActiveCenterChannelList())
            .orElse(Collections.emptyList());
        nettyClientList.stream().forEach(nettyClient -> {
            try {
                this.doRegister(nettyClient, registryInfo, CommandTypeMarkEnum.BATCH_SYNC_REGISTER_SERVICE);
            } catch (Exception e) {
                // 同步某个节点失败，这里不会再做重试，通过心跳去同步
                log.error("同步 groupName: {}, interface: {}, version: {} host：{} 到 {} 节点失败！将在下次心跳时同步", registryInfo.getGroupName(),
                    registryInfo.getInterfaceName(), registryInfo.getVersion(), registryInfo.getHostInfo(), nettyClient.getHostInfo());
            }
        });

    }

    @Override
    public void syncUnRegister(RegistryInfo registryInfo) {
        List<NettyClient> nettyClientList = Optional.ofNullable(this.connectManager.getActiveCenterChannelList())
            .orElse(Collections.emptyList());
        nettyClientList.stream().forEach(nettyClient -> {
            try {
                this.doUnRegister(registryInfo, CommandTypeMarkEnum.BATCH_SYNC_REMOVE_SERVICE, 1);
            } catch (Exception e) {
                // 同步某个节点失败，这里不会再做重试，通过心跳去同步
                log.error("同步 groupName: {}, interface: {}, version: {} 到 {} 节点失败！将在下次心跳时同步", registryInfo.getGroupName(),
                    registryInfo.getInterfaceName(), registryInfo.getVersion(), registryInfo.getHostInfo());
            }
        });
    }

    @Override
    public boolean isCenter() {
        return false;
    }

    @Override
    public boolean connectPeerNode(HostInfo hostInfo) {
        try {
            // 启动netty客户端
            return this.connectManager.openNewChannel(hostInfo);
        } catch (Exception e) {
            // 链接失败，将会重试
            log.error(String.format("连接注册中心 %s:%s 失败！请检查地址", hostInfo.getHost(), hostInfo.getPort()),
                e);
            return false;
        }
    }

    @Override
    public RegistryNotifyListener getCenterNodeChangeNotify() {
        return registryNotifyInfos -> {

            if(CollectionUtil.isEmpty(registryNotifyInfos)) {
                return;
            }

            List<RegistryInfo> registryInfoList = registryNotifyInfos.stream()
                .flatMap(e -> Optional.ofNullable(e.getRegistryInfos())
                    .orElse(Collections.emptyList()).stream()).collect(Collectors.toList());

            if(CollectionUtil.isEmpty(registryInfoList)) {
                return;
            }
            List<HostInfo> hostInfoList = registryInfoList.stream().map(RegistryInfo::getHostInfo)
                .distinct().collect(Collectors.toList());
            this.connectManager.refreshCenterConnect(hostInfoList);
        };
    }

    @Override
    public void syncRenew(List<RegistryInfo> registryInfos) {

        Request<Object> request = new Request<>();
        request.setId(0L);
        request.setCommandType(CommandTypeMarkEnum.SYNC_RENEW_SERVICE.getType());
        RegistryInfoDirectory directory = new RegistryInfoDirectory();
        directory.setRegistryInfoList(registryInfos);
        request.setBody(directory);
        request.setOneWay(true);

        List<NettyClient> nettyClientList = Optional.ofNullable(this.connectManager.getActiveCenterChannelList())
            .orElse(Collections.emptyList());
        nettyClientList.stream().forEach(nettyClient -> {
            try {
                nettyClient.send(request);
            } catch (Exception e) {
                // 同步续约失败，这里不会再重试，通过下次客户端心跳发生续约同步
                log.error("向节点{}同步续约{}失败！", nettyClient.getHostInfo(), registryInfos);
            }
        });
    }

    @Override
    public void subscribe(ServerInfo serverInfo, RegistryNotifyListener notifyListener) {

        this.subscribeWithLock(serverInfo, key -> {
            List<RegistryNotifyListener> listeners = keyMapListenerMap.get(key);
            if(Objects.isNull(listeners)) {
                listeners = new ArrayList<>();
                keyMapListenerMap.put(key, listeners);
            }
            listeners.add(notifyListener);
        });
        this.sendSubscribeRequest(serverInfo);
    }

    private void subscribeWithLock(ServerInfo serverInfo, Consumer<String> consumer) {
        String key = CommonUtils.createServiceKey(serverInfo);
        while (keyMapListenerLockMap.putIfAbsent(key, lock) != null) {
            Thread.yield();
        }
        try {
            consumer.accept(key);
        } finally {
            keyMapListenerLockMap.remove(key);
        }
    }

    @Override
    public void notify(List<RegistryNotifyInfo> registryNotifyInfos) {

        registryNotifyInfos.stream().forEach(registryNotifyInfo -> {
            String key = CommonUtils.createServiceKey(registryNotifyInfo.getServerInfo());
            List<RegistryNotifyListener> listeners = this.keyMapListenerMap.getOrDefault(key, Collections.emptyList());
            listeners.stream().forEach(e -> e.notify(Collections.singletonList(registryNotifyInfo)));
        });
    }

    @Override
    public void init() throws Exception {
        List<HostInfo> hostInfos = this.parseHostInfoAndCheck(clientProperties.getPeerNodeHosts());
        this.parseOtherProperties(clientProperties);

        hostInfos.stream().forEach(hostInfo -> {
            this.connectPeerNode(hostInfo);
        });
    }

    @Override
    public void close() throws Exception {
        NettyClientEventLoopManager.close();
    }

    public void doUnRegister(RegistryInfo registryInfo, CommandTypeMarkEnum markEnum, Integer retryCount) {
        try {
            this.retryAbleJob(retryCount, nettyClient -> {
                Channel channel = nettyClient.getChannel();

                Request<List<RegistryInfo>> request = new Request<>();
                request.setId(CommonUtils.snowFlakeNextId());
                request.setCommandType(markEnum.getType());
                request.setBody(Collections.singletonList(registryInfo));

                DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(channel.eventLoop());
                RpcRequestManager.putFuture(request.getId(), defaultPromise);

                channel.writeAndFlush(request);

                this.dealResult(nettyClient, defaultPromise, clientProperties.getTransport().getRegistryServiceTimeout(),
                    markEnum.getDesc());
                return null;
            });
        } catch (RpcInvokerException e) {
            throw e;
        } catch (Exception e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "调用异常！", e);
        }
    }

    private void doRegister(RegistryInfo registryInfo, CommandTypeMarkEnum markEnum, Integer retryCount) {

        try {
            this.retryAbleJob(retryCount, nettyClient -> {
                this.doRegister(nettyClient, registryInfo, markEnum);
                return null;
            });
        } catch (NoServerAvailableException e) {
            throw e;
        } catch (RpcInvokerException e) {
            throw e;
        } catch (Exception e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "调用异常！", e);
        }
    }

    private void doRegister(NettyClient nettyClient, RegistryInfo registryInfo, CommandTypeMarkEnum markEnum) {
        Channel channel = nettyClient.getChannel();

        Request<List<RegistryInfo>> request = new Request<>();
        request.setId(CommonUtils.snowFlakeNextId());
        request.setCommandType(markEnum.getType());
        request.setBody(Collections.singletonList(registryInfo));

        DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(channel.eventLoop());
        RpcRequestManager.putFuture(request.getId(), defaultPromise);

        channel.writeAndFlush(request);

        this.dealResult(nettyClient, defaultPromise, clientProperties.getTransport().getRegistryServiceTimeout(),
            markEnum.getDesc());
        this.addRegistryInfo(registryInfo);
    }


    private NettyClient getCenterConnect(List<NettyClient> exclusionList) {
        Optional<NettyClient> optionalChannel = this.connectManager.pollActiveAndCompleteChannel(exclusionList);
        NettyClient nettyClient = optionalChannel.orElseThrow(
            () -> new NoServerAvailableException(ErrorCodeEnum.NOT_FOUND.getCode(),
                "没获取到可用的注册中心连接，请检查地址或者对应节点是否可用！"));
        return nettyClient;
    }

    private <T> T dealResult(NettyClient nettyClient, DefaultPromise<RpcResult> defaultPromise, int timeout,
        String errorMsgPrefix) {

        try {
            RpcResult result = defaultPromise.get(timeout,
                TimeUnit.SECONDS);
            int code = result.getCode();
            Object body = result.getResult();
            if (code == ErrorCodeEnum.SUCCESS.getCode()) {
                return (T) body;
            }
            if (body instanceof ServerNodeUnCompleteException) {
                ServerNodeUnCompleteException serverNodeUnCompleteException = (ServerNodeUnCompleteException) body;
                nettyClient.setStatus(ServerStatusEnum.getEnumByStatus(serverNodeUnCompleteException.getStatus()));
                throw serverNodeUnCompleteException;
            }
            if (body instanceof RpcInvokerException) {
                throw (RpcInvokerException) body;
            }
            if (body instanceof Throwable) {
                Throwable ex = (Throwable) body;
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), errorMsgPrefix + "失败！", ex);
            }
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), errorMsgPrefix + "失败！");
        } catch (InterruptedException e) {
            log.error("msg:{}", e.getMessage(), e);
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), errorMsgPrefix + "过程中，发生了中断异常！",
                e);
        } catch (ExecutionException e) {
            log.error("msg:{}", e.getMessage(), e);
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), errorMsgPrefix + "过程中，发生了调用异常！",
                e);
        } catch (TimeoutException e) {
            log.error("code:{},msg:{}", ErrorCodeEnum.TIME_OUT.getCode(), errorMsgPrefix + "超时！", e);
            throw new RpcInvokerTimeoutException(ErrorCodeEnum.TIME_OUT.getCode(), errorMsgPrefix + "超时！", e);
        } catch (RpcInvokerException e) {
            log.error("code:{},msg:{}", e.getCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("code:{},msg:{}", ErrorCodeEnum.FAILURE.getCode(), e.getMessage(), e);
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), errorMsgPrefix + "失败！", e);
        }
    }

    private List<RegistryInfo> doLookupService(Supplier<Request<?>> supplier) throws Exception {
        return this.retryAbleJob(3, nettyClient -> {
            Request request = supplier.get();
            DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(nettyClient.getChannel().eventLoop());
            RpcRequestManager.putFuture(request.getId(), defaultPromise);

            nettyClient.getChannel().writeAndFlush(request);

            return dealResult(nettyClient, defaultPromise,
                clientProperties.getTransport().getPullServiceListTimeout(), "拉取服务列表");
        });
    }

    private <T> T retryAbleJob(Integer retryCount, Function<NettyClient, T> function) throws Exception {
        retryCount = Objects.isNull(retryCount) || retryCount <= 0 ? 1 : retryCount;
        int count = 0;
        Exception ex = null;
        List<NettyClient> exclusionList = new ArrayList<>();
        while (count < retryCount) {
            NettyClient nettyClient = this.getCenterConnect(exclusionList);
            try {
                return function.apply(nettyClient);
            } catch (NoServerAvailableException e) {
                throw e;
            } catch (Exception e) {
                ex = e;
                count++;
                exclusionList.add(nettyClient);
            }
        }
        throw ex;
    }

    private List<HostInfo> parseHostInfoAndCheck(String peerNodeHosts) {

        if (StrUtil.isBlank(peerNodeHosts)) {
            throw new RpcStarterException(ErrorCodeEnum.FAILURE.getCode(), "未配置注册中心地址！");
        }

        List<String> hosts = Arrays.stream(peerNodeHosts.split(",")).filter(StrUtil::isNotBlank)
            .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(hosts)) {
            throw new RpcStarterException(ErrorCodeEnum.FAILURE.getCode(),
                "注册中心地址配置错误，正确的格式为 ip:port！");
        }

        List<String> errorIpList = new ArrayList<>();
        List<HostInfo> hostInfos = new ArrayList<>();
        hosts.stream().forEach(host -> {
            String[] ipArr = host.split(":");
            if (ipArr.length != 2) {
                errorIpList.add(host);
            } else {
                try {
                    int port = Integer.parseInt(ipArr[1]);
                    HostInfo hostInfo = new HostInfo();
                    hostInfo.setHost(ipArr[0]);
                    hostInfo.setPort(port);
                    hostInfos.add(hostInfo);
                } catch (Exception e) {
                    errorIpList.add(host);
                }
            }
        });
        if (!CollectionUtil.isEmpty(errorIpList)) {
            throw new RpcStarterException(ErrorCodeEnum.FAILURE.getCode(),
                String.format("注册中心地址【%s】配置错误，正确的格式为 ip:port！", errorIpList.stream().collect(
                    Collectors.joining(","))));
        }
        return hostInfos;
    }

    private void parseOtherProperties(ClientProperties clientProperties) {

        TransportProperties transport = clientProperties.getTransport();
        if (Objects.isNull(transport)) {
            transport = new TransportProperties();
            transport.setHeartbeatTimeRate(2);
            transport.setHeartbeatTimeOutCount(3);
            clientProperties.setTransport(transport);
            return;
        }
        int heartbeatTimeRate = transport.getHeartbeatTimeRate();
        heartbeatTimeRate = heartbeatTimeRate <= 0 ? 2 : heartbeatTimeRate;
        transport.setHeartbeatTimeRate(heartbeatTimeRate);

        int heartbeatTimeOutCount = transport.getHeartbeatTimeOutCount();
        heartbeatTimeOutCount = heartbeatTimeOutCount <= 0 ? 3 : heartbeatTimeOutCount;
        // 如果用户配置成了1，报错，至少都要是心跳超时时间的两倍
        if (heartbeatTimeOutCount == 1) {
            throw new RpcStarterException(ErrorCodeEnum.FAILURE.getCode(), "心跳超时时间，至少都要是心跳的2倍时间！");
        }
        transport.setHeartbeatTimeOutCount(heartbeatTimeOutCount);

        int pullServiceListTimeout = transport.getPullServiceListTimeout();
        pullServiceListTimeout = pullServiceListTimeout <= 0 ? 5 : pullServiceListTimeout;
        transport.setPullServiceListTimeout(pullServiceListTimeout);
    }

    private void sendSubscribeRequest(ServerInfo serverInfo) {
        try {
            Long id = CommonUtils.snowFlakeNextId();
            NettyClient nettyClient = this.sendCommonRequest(CommandTypeMarkEnum.SINGLE_SUBSCRIBE_SERVICE, id, serverInfo);
            DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(nettyClient.getChannel().eventLoop());
            RpcRequestManager.putFuture(id, defaultPromise);
            this.dealResult(nettyClient, defaultPromise, clientProperties.getTransport().getPullServiceListTimeout(), CommandTypeMarkEnum.SINGLE_SUBSCRIBE_SERVICE.getDesc());
            this.subscribeWithLock(serverInfo, key -> {
                nettyClient.addSubscribeServerInfo(serverInfo);
            });
        } catch (Exception e) {
            log.error("订阅失败！", e);
            this.scheduledExecutorService.schedule(() -> {
                this.sendSubscribeRequest(serverInfo);
            }, 2, TimeUnit.SECONDS);
        }
    }

    private <T> NettyClient sendCommonRequest(CommandTypeMarkEnum markEnum, Long id, T data) {
        NettyClient nettyClient = this.getCenterConnect(Collections.emptyList());
        this.sendCommonRequest(nettyClient, markEnum, id, data, false);
        return nettyClient;
    }

    private <T> void sendCommonRequest(NettyClient nettyClient, CommandTypeMarkEnum markEnum, Long id, T data, boolean oneWay) {
        Request<T> request = new Request<>();
        request.setId(id);
        request.setCommandType(markEnum.getType());
        request.setBody(data);
        request.setOneWay(oneWay);
        nettyClient.send(request);
    }

    public void reSendSubscribe(Set<ServerInfo> serverInfoSet) {
        Optional<NettyClient> optionalNettyClient = this.connectManager.pollActiveAndCompleteChannel(
            Collections.emptyList());
        if (optionalNettyClient.isPresent()) {
            try {
                NettyClient nettyClient = optionalNettyClient.get();
                // 重新注册
                Long id = CommonUtils.snowFlakeNextId();
                this.sendCommonRequest(nettyClient, CommandTypeMarkEnum.BATCH_SUBSCRIBE_SERVICE, id,
                    serverInfoSet, false);
                DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(nettyClient.getChannel().eventLoop());
                RpcRequestManager.putFuture(id, defaultPromise);
                this.dealResult(nettyClient, defaultPromise, clientProperties.getTransport().getPullServiceListTimeout(), CommandTypeMarkEnum.BATCH_SUBSCRIBE_SERVICE.getDesc());
                nettyClient.addSubscribeServerInfo(serverInfoSet);
            } catch (Exception e) {
                log.error("重新订阅失败！将在2s之后重试！", e);
                this.scheduledExecutorService.schedule(() -> {
                    this.reSendSubscribe(serverInfoSet);
                }, 2, TimeUnit.SECONDS);
            }
        } else {
            log.warn("重新订阅时，未从链接管理池中获取到可用的链接，2s之后重试！");
            this.scheduledExecutorService.schedule(() -> {
                this.reSendSubscribe(serverInfoSet);
            }, 2, TimeUnit.SECONDS);
        }
    }

    public void addRegistryInfo(RegistryInfo registryInfo) {
        String key = CommonUtils.createServiceKey(registryInfo);
        this.registryInfoTable.put(key, registryInfo);
    }

    public List<RegistryInfo> getRegistryInfoTableList() {
        return registryInfoTable.values().stream().collect(Collectors.toList());
    }
}

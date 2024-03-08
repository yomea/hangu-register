package org.hangu.center.discover.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.concurrent.DefaultPromise;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.discover.lookup.LookupService;
import org.hangu.center.discover.manager.ConnectManager;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.exception.RpcInvokerException;
import org.hangu.center.common.exception.RpcInvokerTimeoutException;
import org.hangu.center.common.exception.RpcStarterException;
import org.hangu.center.common.properties.TransportProperties;
import org.hangu.center.common.util.CommonUtils;
import org.hangu.center.discover.lookup.RegistryService;
import org.hangu.center.discover.manager.NettyClientEventLoopManager;
import org.hangu.center.discover.manager.RpcRequestManager;
import org.hangu.center.discover.properties.ClientProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/11 17:42
 */
@Slf4j
public class DiscoverClient implements LookupService, RegistryService, InitializingBean, DisposableBean {

    private ClientProperties clientProperties;

    private ConnectManager connectManager;


    public DiscoverClient(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
        this.connectManager = new ConnectManager();
    }

    @Override
    public void destroy() throws Exception {
        NettyClientEventLoopManager.close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        List<HostInfo> hostInfos = this.parseHostInfoAndCheck(clientProperties.getPeerNodeHosts());
        this.parseOtherProperties(clientProperties);

        hostInfos.stream().forEach(hostInfo -> {
            try {
                // 启动netty客户端
                NettyClient nettyClient = new NettyClient(this.connectManager, clientProperties.getTransport(), hostInfo);
                nettyClient.open();
                nettyClient.syncConnect();
                this.connectManager.cacheChannel(nettyClient);
            } catch (Exception e) {
                throw new RpcStarterException(ErrorCodeEnum.FAILURE.getCode(),
                    String.format("连接注册中心 %s:%s 失败！请检查地址", hostInfo.getHost(), hostInfo.getPort()), e);
            }
        });
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
    public List<RegistryInfo> lookup()  throws Exception {
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

    private List<RegistryInfo> doLookupService(Supplier<Request<?>> supplier) throws Exception {
        Set<ChannelId> exclusionList = new HashSet<>();
        int count = 0;
        Exception ex = null;

        while (count < 3) {
            Channel channel = this.getCenterConnect(exclusionList);
            Request request = supplier.get();
            DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(channel.eventLoop());
            RpcRequestManager.putFuture(request.getId(), defaultPromise);

            try {
                channel.writeAndFlush(request);

                return dealResult(defaultPromise,
                    clientProperties.getTransport().getPullServiceListTimeout(), "拉取服务列表");
            } catch (Exception e) {
                ex = e;
                count++;
                exclusionList.add(channel.id());
            }
        }
        throw ex;
    }

    private List<HostInfo> parseHostInfoAndCheck(String peerNodeHosts) {

        if (!StringUtils.hasText(peerNodeHosts)) {
            throw new RpcStarterException(ErrorCodeEnum.FAILURE.getCode(), "未配置注册中心地址！");
        }

        List<String> hosts = Arrays.stream(peerNodeHosts.split(",")).filter(StringUtils::hasText)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hosts)) {
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
        if (!CollectionUtils.isEmpty(errorIpList)) {
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

    @Override
    public void register(RegistryInfo registryInfo) {
        Channel channel = this.getCenterConnect();

        Request<RegistryInfo> request = new Request<>();
        request.setId(CommonUtils.snowFlakeNextId());
        request.setCommandType(CommandTypeMarkEnum.BATCH_REGISTER_SERVICE.getType());
        request.setBody(registryInfo);

        DefaultPromise<RpcResult> defaultPromise = new DefaultPromise<>(channel.eventLoop());
        RpcRequestManager.putFuture(request.getId(), defaultPromise);

        channel.writeAndFlush(request);

        this.dealResult(defaultPromise, clientProperties.getTransport().getPullServiceListTimeout(), "拉取服务列表");
    }

    @Override
    public void unRegister(RegistryInfo serverInfo) {

    }


    private Channel getCenterConnect() {
        return this.getCenterConnect(Collections.emptySet());
    }

    private Channel getCenterConnect(Set<ChannelId> exclusionList) {
        Optional<Channel> optionalChannel = this.connectManager.randActiveChannel(exclusionList);
        Channel channel = optionalChannel.orElseThrow(() -> new RpcStarterException(ErrorCodeEnum.NOT_FOUND.getCode(),
            "没获取到可用的注册额中心连接，请检查连接！"));
        return channel;
    }

    private <T> T dealResult(DefaultPromise<RpcResult> defaultPromise, int timeout, String errorMsgPrefix) {

        try {
            RpcResult result = defaultPromise.get(timeout,
                TimeUnit.SECONDS);
            int code = result.getCode();
            Object body = result.getResult();
            if (code == ErrorCodeEnum.SUCCESS.getCode()) {
                return (T) body;
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

}

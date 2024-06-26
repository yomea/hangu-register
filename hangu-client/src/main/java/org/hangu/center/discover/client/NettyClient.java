package org.hangu.center.discover.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.channel.handler.ByteFrameDecoder;
import org.hangu.center.common.entity.HostInfo;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.ServerInfo;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.common.properties.TransportProperties;
import org.hangu.center.discover.channel.handler.HeartBeatPongHandler;
import org.hangu.center.discover.channel.handler.RequestMessageCodec;
import org.hangu.center.discover.channel.handler.ResponseMessageHandler;
import org.hangu.center.discover.manager.CenterConnectManager;
import org.hangu.center.discover.manager.NettyClientEventLoopManager;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:19
 */
@Slf4j
public class NettyClient {

    private Bootstrap bootstrap;

    private DiscoverClient discoverClient;

    private CenterConnectManager connectManager;

    private TransportProperties transport;

    private HostInfo hostInfo;

    private Channel channel;

    private boolean center;

    private boolean release = false;

    private Long maxPullRegistryTime = 0L;

    private ServerStatusEnum status = ServerStatusEnum.UN_KNOW;

    private Set<ServerInfo> subscribeServerInfoList = new HashSet<>();

    public NettyClient(
        DiscoverClient discoverClient,
        CenterConnectManager connectManager,
        TransportProperties transport,
        HostInfo hostInfo,
        boolean center) {
        this.discoverClient = discoverClient;
        this.connectManager = connectManager;
        this.transport = transport;
        this.hostInfo = hostInfo;
        this.center = center;
    }

    public void open(ExecutorService executorService) {
        try {
            bootstrap = new Bootstrap();
//            @Sharable
            LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
            bootstrap.group(NettyClientEventLoopManager.getEventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                            .addLast(new ByteFrameDecoder())
                            .addLast(new RequestMessageCodec()) // 请求与响应编解码器
//                            .addLast("logging", loggingHandler)
                            // 每隔 2s 发送一次心跳，超过三次没有收到响应，也就是三倍的心跳时间，重连
                            .addLast(new IdleStateHandler(transport.getHeartbeatTimeRate(), 0, 0, TimeUnit.SECONDS))
                            .addLast(new HeartBeatPongHandler(NettyClient.this,
                                transport.getHeartbeatTimeOutCount())) // 心跳编码器
                            .addLast(new ResponseMessageHandler(NettyClient.this, executorService));
                    }
                });
        } catch (Exception e) {
            log.error("rpc客户端启动失败！", e);
        }
    }

    /**
     * 连接
     *
     * @return
     */
    public Channel syncConnect() throws InterruptedException {
        this.channel = this.bootstrap.connect(hostInfo.getHost(), hostInfo.getPort()).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("连接 {}:{} 失败！", hostInfo.getHost(), hostInfo.getPort());
            }
        }).sync().channel();

        return this.channel;
    }

    public void reconnect() {
        this.bootstrap.connect(hostInfo.getHost(), hostInfo.getPort()).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("连接 {}:{} 失败！", hostInfo.getHost(), hostInfo.getPort());
            }
            ChannelFuture channelFuture = (ChannelFuture) future;
            this.channel = channelFuture.channel();
        });
    }

    public CenterConnectManager getConnectManager() {
        return connectManager;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public boolean isActive() {
        return Objects.nonNull(this.channel) && this.channel.isActive();
    }

    public boolean isComplete() {
        return ServerStatusEnum.COMPLETE == status;
    }

    public boolean isUnKnow() {
        return ServerStatusEnum.UN_KNOW == status;
    }

    public boolean isCenter() {
        return this.center;
    }

    public HostInfo getHostInfo() {
        return this.hostInfo;
    }

    public ServerStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ServerStatusEnum status) {
        this.status = status;
    }

    public Set<ServerInfo> getSubscribeServerInfoList() {
        return subscribeServerInfoList;
    }

    public synchronized void addSubscribeServerInfo(ServerInfo serverInfo) {
        this.subscribeServerInfoList.add(serverInfo);
    }

    public synchronized void addSubscribeServerInfo(Set<ServerInfo> serverInfos) {
        this.subscribeServerInfoList.addAll(serverInfos);
    }

    public synchronized void clearSubscribeServerInfo() {
        this.subscribeServerInfoList.clear();
    }

    public void send(Request<?> request) {
        this.channel.writeAndFlush(request);
    }

    public Long getMaxPullRegistryTime() {
        return this.maxPullRegistryTime;
    }

    public synchronized void updateMaxPullRegistryTime(long pullRegistryTime) {
        this.maxPullRegistryTime = Math.max(this.getMaxPullRegistryTime(), pullRegistryTime);
    }

    public void markRelease(boolean release) {
        this.release = release;
    }

    public boolean isRelease() {
        return this.release;
    }

    public void close() {
        if(Objects.nonNull(this.channel)) {
            this.channel.close();
        }
    }

    public DiscoverClient getDiscoverClient() {
        return discoverClient;
    }
}

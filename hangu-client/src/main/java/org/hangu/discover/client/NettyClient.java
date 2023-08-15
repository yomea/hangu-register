package org.hangu.discover.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hangu.common.channel.handler.ByteFrameDecoder;
import org.hangu.common.channel.handler.HeartBeatEncoder;
import org.hangu.common.constant.HanguCons;
import org.hangu.common.enums.ErrorCodeEnum;
import org.hangu.common.exception.RpcStarterException;
import org.hangu.common.properties.TransportProperties;
import org.hangu.discover.channel.handler.HeartBeatPongHandler;
import org.hangu.discover.channel.handler.RequestMessageCodec;
import org.hangu.discover.channel.handler.ResponseMessageHandler;
import org.hangu.discover.manager.ConnectManager;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:19
 */
@Slf4j
public class NettyClient {

    private Bootstrap bootstrap;

    private NioEventLoopGroup nioEventLoopGroup;

    private ConnectManager connectManager;

    private TransportProperties transport;

    public NettyClient(ConnectManager connectManager, TransportProperties transport) {
        this.connectManager = connectManager;
        this.transport = transport;
    }

    public void start() {
        try {
            bootstrap = new Bootstrap();
            nioEventLoopGroup = new NioEventLoopGroup(HanguCons.CPUS << 3);
//            @Sharable
            LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
            bootstrap.group(nioEventLoopGroup)
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
                            .addLast(new HeartBeatEncoder()) // 心跳编码器
                            .addLast("logging", loggingHandler)
                            // 每隔 2s 发送一次心跳，超过三次没有收到响应，也就是三倍的心跳时间，重连
                            .addLast(new IdleStateHandler(transport.getHeartbeatTimeRate(), 0, 0, TimeUnit.SECONDS))
                            .addLast(new HeartBeatPongHandler(NettyClient.this,
                                transport.getHeartbeatTimeOutCount())) // 心跳编码器
                            .addLast(new ResponseMessageHandler());
                    }
                });
        } catch (Exception e) {
            log.error("rpc客户端启动失败！", e);
            this.close();
        }
    }

    public void close() {
        if (nioEventLoopGroup != null) {
            nioEventLoopGroup.shutdownGracefully();
        }
    }

    /**
     * 连接
     *
     * @param hostIp
     * @param port
     * @return
     */
    public ChannelFuture syncConnect(String hostIp, int port) throws InterruptedException {
        ChannelFuture channelFuture = this.bootstrap.connect(hostIp, port).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("连接 {}:{} 失败！", hostIp, port);
            }
        }).sync();

        return channelFuture;
    }

    public void connect(String hostIp, int port, GenericFutureListener<? extends Future<? super Void>> listener)
        throws InterruptedException {
        this.bootstrap.connect(hostIp, port).addListener(listener).channel();
    }

    public ChannelFuture reconnect(SocketAddress remoteAddress) {
        return this.bootstrap.connect(remoteAddress);
    }

    public ConnectManager getConnectManager() {
        return connectManager;
    }
}

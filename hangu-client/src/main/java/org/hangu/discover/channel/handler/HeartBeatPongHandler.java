package org.hangu.discover.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hangu.common.entity.PingPong;
import org.hangu.common.util.CommonUtils;
import org.hangu.discover.client.NettyClient;

/**
 * 心跳处理器
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:40
 */
@Slf4j
public class HeartBeatPongHandler extends SimpleChannelInboundHandler<PingPong> {

    private NettyClient nettyClient;

    private int retryBeat = 0;

    public HeartBeatPongHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingPong pingPong) throws Exception {
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState idleState = idleStateEvent.state();
            // 读超时，发送心跳
            if (IdleState.READER_IDLE == idleState) {
                if (!ctx.channel().isActive()) {
                    this.reconnect(ctx);
                } else {
                    PingPong pingPong = new PingPong();
                    pingPong.setId(CommonUtils.snowFlakeNextId());
                    // 发送心跳（从当前 context 往前）
                    ctx.writeAndFlush(pingPong).addListener(future -> {
                        // 发送失败，有可能是连读断了，也有可能只是网络抖动问题
                        if (!future.isSuccess() && ++retryBeat > 3) {
                            // 重连
                            this.reconnect(ctx);
                        } else {
                            retryBeat = 0;
                        }
                    });
                }
            }
        }
    }

    private void reconnect(ChannelHandlerContext ctx) {
        ctx.channel().close().addListener(future -> {
            SocketAddress remoteAddress = ctx.channel().remoteAddress();
            if (!future.isSuccess()) {
                log.warn("通道{}关闭失败！", remoteAddress.toString());
                return;
            }
            ctx.channel().eventLoop().schedule(() -> {
                // 重连创建一个新的通道
                nettyClient.reconnect(remoteAddress).addListener(f -> {
                    if (!f.isSuccess()) {
                        log.error("重新连接{}失败！", remoteAddress.toString());
                    }
                });
            }, 1, TimeUnit.SECONDS);
        });
    }
}

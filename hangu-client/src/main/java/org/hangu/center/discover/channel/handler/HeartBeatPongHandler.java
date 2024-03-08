package org.hangu.center.discover.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.discover.client.NettyClient;
import org.hangu.center.common.entity.PingPong;
import org.hangu.center.common.util.CommonUtils;

/**
 * 心跳处理器
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:40
 */
@Slf4j
public class HeartBeatPongHandler extends ChannelInboundHandlerAdapter {

    private NettyClient nettyClient;

    private int retryBeat = 0;

    private int allowRetryBeat;

    public HeartBeatPongHandler(NettyClient nettyClient, int allowRetryBeat) {
        this.nettyClient = nettyClient;
        this.allowRetryBeat = allowRetryBeat <= 1 ? 3 : allowRetryBeat;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        // 尝试重连
        this.reconnect(ctx);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到消息（无论是心跳消息还是任何其他rpc消息），重置重试发送心跳次数
        this.retryBeat = 0;
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState idleState = idleStateEvent.state();
            // 读超时，发送心跳
            if (IdleState.READER_IDLE == idleState) {
                if (retryBeat > allowRetryBeat) {
                    // 关闭重连，通过监听 channelUnregistered 发起重连
                    ctx.channel().close();
                } else {
                    PingPong pingPong = new PingPong();
                    pingPong.setId(CommonUtils.snowFlakeNextId());
                    // 发送心跳（从当前 context 往前）
                    ctx.writeAndFlush(pingPong).addListener(future -> {
                        if (!future.isSuccess()) {
                            log.error("发送心跳失败！", future.cause());
                        }
                    });
                    ++retryBeat;
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void reconnect(ChannelHandlerContext ctx) {

        // 如果连接还活着，不需要重连
        if (this.nettyClient.isActive()) {
            return;
        }
        // 每 2s 重试一次
        ctx.channel().eventLoop().schedule(() -> {
            this.nettyClient.reconnect();
        }, 2, TimeUnit.SECONDS);
    }
}

package org.hangu.center.server.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理来自客户端的心跳
 *
 * @author wuzhenhong
 * @date 2023/7/31 17:56
 */
@Slf4j
public class HeartBeatPingHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            IdleState idleState = stateEvent.state();
            // 该链接客户端很久没有向服务端发送信息或者心跳了，直接叫该链接关闭，回收资源
            // 提供者端不主动发送心跳，心跳完全由客户端主动发起
            if (IdleState.ALL_IDLE == idleState) {
                // 关闭连接
                ctx.channel().close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("心跳处理失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}

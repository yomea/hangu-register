package org.hangu.center.server.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.server.bussiness.handler.RequestHandler;
import org.hangu.center.server.bussiness.handler.RequestHandlerFactory;
import org.hangu.center.server.server.NettyServer;

/**
 * 处理请求消息
 *
 * @author wuzhenhong
 * @date 2023/8/1 14:03
 */
public class RequestMessageHandler extends SimpleChannelInboundHandler<Request> {

    private NettyServer nettyServer;
    private Executor executor;


    public RequestMessageHandler(NettyServer nettyServer, Executor executor) {
        this.nettyServer = nettyServer;
        this.executor = executor;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        // 内部通道注销通知
        RequestHandler requestHandler = RequestHandlerFactory.getRequestHandlerByType(CommandTypeMarkEnum.UN_REGISTERED_SERVICE.getType());
        if(Objects.nonNull(requestHandler)) {
            this.executor.execute(() -> {
                requestHandler.handler(null, this.nettyServer, ctx.channel());
            });
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        // 根据命令处理响应业务
        byte commonType = request.getCommandType();
        RequestHandler requestHandler = RequestHandlerFactory.getRequestHandlerByType(commonType);
        this.executor.execute(() -> {
            Response response = requestHandler.handler(request, this.nettyServer, ctx.channel());
            if(!request.isOneWay()) {
                ctx.channel().writeAndFlush(response);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}

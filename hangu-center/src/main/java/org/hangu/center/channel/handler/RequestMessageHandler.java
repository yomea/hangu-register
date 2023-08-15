package org.hangu.center.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.Executor;
import org.hangu.center.bussiness.handler.RequestHandler;
import org.hangu.center.bussiness.handler.RequestHandlerFactory;
import org.hangu.common.entity.Request;
import org.hangu.common.entity.Response;

/**
 * 处理请求消息
 *
 * @author wuzhenhong
 * @date 2023/8/1 14:03
 */
public class RequestMessageHandler extends SimpleChannelInboundHandler<Request> {

    private Executor executor;

    public RequestMessageHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        // 根据命令处理响应业务
        byte commonType = request.getCommandType();
        RequestHandler requestHandler = RequestHandlerFactory.getRequestHandlerByType(commonType);
        this.executor.execute(() -> {
            Response response = requestHandler.handler(request);
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

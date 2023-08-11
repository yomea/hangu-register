package org.hangu.disconver.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultPromise;
import java.util.Objects;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.hangu.common.entity.Response;
import org.hangu.common.entity.RpcResult;
import org.hangu.disconver.manager.RpcRequestManager;

/**
 * 处理提供者的返回响应
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:36
 */
@Slf4j
public class ResponseMessageHandler extends SimpleChannelInboundHandler<Response> {

    private Executor executor;

    public ResponseMessageHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {

        Long id = response.getId();
        DefaultPromise<RpcResult> future = RpcRequestManager.getFuture(id);
        if (Objects.isNull(future) || future.isCancelled()) {
            log.warn("无效的响应请求！id = {}", id);
            return;
        }

        RpcResult rpcResult = response.getRpcResult();
        future.trySuccess(rpcResult);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理返回响应失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}

package org.hangu.center.discover.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultPromise;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.CenterNodeInfo;
import org.hangu.center.common.entity.RegistryInfo;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.discover.bussiness.handler.ResponseHandler;
import org.hangu.center.discover.bussiness.handler.ResponseHandlerFactory;
import org.hangu.center.discover.client.NettyClient;
import org.hangu.center.discover.manager.RpcRequestManager;

/**
 * 处理提供者的返回响应
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:36
 */
@Slf4j
public class ResponseMessageHandler extends SimpleChannelInboundHandler<Response> {

    private NettyClient nettyClient;

    public ResponseMessageHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
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
        byte commandType = response.getCommandType();
        ResponseHandler responseHandler = ResponseHandlerFactory.getResponseHandlerByType(commandType);
        if(Objects.isNull(responseHandler)) {
            log.error("commonType为{}的响应处理器不存在！", commandType);
        } else {
            responseHandler.handler(response, this.nettyClient);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理返回响应失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}

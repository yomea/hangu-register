package org.hangu.center.discover.channel.handler;

import cn.hutool.json.JSONUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultPromise;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.CommandTypeMarkEnum;
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
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ResponseHandler responseHandler = ResponseHandlerFactory.getResponseHandlerByType(CommandTypeMarkEnum.UN_REGISTERED_SERVICE.getType());
        if(Objects.nonNull(responseHandler)) {
            responseHandler.handler(null, this.nettyClient);
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {

        Long id = response.getId();
        if(Objects.nonNull(id) && id > 0L) {
            DefaultPromise<RpcResult> future = RpcRequestManager.remoteFuture(id);
            if (Objects.isNull(future) || future.isCancelled()) {
                log.warn("无效的响应请求！response = {}", JSONUtil.toJsonStr(response));
                return;
            } else {
                RpcResult rpcResult = response.getRpcResult();
                future.trySuccess(rpcResult);
            }
        }
        byte commandType = response.getCommandType();
        ResponseHandler responseHandler = ResponseHandlerFactory.getResponseHandlerByType(commandType);
        // 有自定义处理的，走这里
        if(Objects.nonNull(responseHandler)) {
            responseHandler.handler(response, this.nettyClient);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理返回响应失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}

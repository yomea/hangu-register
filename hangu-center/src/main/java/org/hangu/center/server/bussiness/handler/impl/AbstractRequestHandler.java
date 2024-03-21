package org.hangu.center.server.bussiness.handler.impl;

import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.ServerStatusEnum;
import org.hangu.center.common.exception.ServerNodeUnCompleteException;
import org.hangu.center.server.bussiness.handler.RequestHandler;

/**
 * @author wuzhenhong
 * @date 2024/3/11 9:23
 */
public abstract class AbstractRequestHandler<T> implements RequestHandler<T> {

    @Override
    public Response handler(Request<T> request, ServerStatusEnum status) {

        Response response;
        if (ServerStatusEnum.STOP == status) {
            response = this.buildErrorResponse(request, status);
        } else if (ServerStatusEnum.READY == status) {
            response = this.buildErrorResponse(request, status);
        } else if (ServerStatusEnum.COMPLETE == status) {
            response = this.doHandler(request);
        } else {
            throw new UnsupportedOperationException(String.format("服务状态：%s不支持！", status.getDesc()));
        }
        return response;
    }

    public abstract Response doHandler(Request<T> request);

    private Response buildErrorResponse(Request<T> request, ServerStatusEnum statusEnum) {

        Response response = new Response();
        response.setId(request.getId());
        response.setCommandType(request.getCommandType());
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(new ServerNodeUnCompleteException(ErrorCodeEnum.STATUS_UN_COMPLETE.getCode(), statusEnum.getStatus(), String.format("当前节点正在%s中，暂时无法提供服务！", statusEnum.getDesc())));
        rpcResult.setReturnType(ServerNodeUnCompleteException.class);
        rpcResult.setCode(ErrorCodeEnum.STATUS_UN_COMPLETE.getCode());
        response.setRpcResult(rpcResult);

        return response;
    }
}

package org.hangu.center.discover.channel.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.constant.HanguCons;
import org.hangu.center.common.entity.PingPong;
import org.hangu.center.common.entity.Request;
import org.hangu.center.common.entity.Response;
import org.hangu.center.common.entity.RpcResult;
import org.hangu.center.common.enums.ErrorCodeEnum;
import org.hangu.center.common.enums.MsgTypeMarkEnum;
import org.hangu.center.common.exception.RpcInvokerException;
import org.hangu.center.common.serialization.SerialInput;
import org.hangu.center.common.serialization.SerialOutput;
import org.hangu.center.common.serialization.factory.Hessian2SerialInputFactory;
import org.hangu.center.common.serialization.factory.Hessian2SerialOutputFactory;
import org.hangu.center.common.util.CommonUtils;
import org.hangu.center.common.util.DescClassUtils;

@Slf4j
public class RequestMessageCodec extends MessageToMessageCodec<ByteBuf, Request> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Request request, List<Object> out) throws Exception {
        ByteBuf byteBuf = ctx.alloc().buffer();
        // 魔数 2bytes
        byteBuf.writeShort(HanguCons.MAGIC);
        // 请求类型，序列化方式 1bytes
        byte finalMsgType = (byte) (MsgTypeMarkEnum.REQUEST_FLAG.getMark() | request.getCommandType());
        if(request.isOneWay()) {
            finalMsgType |= MsgTypeMarkEnum.ONE_WAY_FLAG.getMark();
        }
        // 消息类型 1byte
        byteBuf.writeByte(finalMsgType);
        // 写入请求id
        byteBuf.writeLong(request.getId());
        // 写入内容
        Object requestBody = request.getBody();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            SerialOutput serialOutput = Hessian2SerialOutputFactory.createSerialization(outputStream);
            serialOutput.writeString(DescClassUtils.getDesc(requestBody.getClass()));
            serialOutput.writeObject(requestBody);
            serialOutput.flush();
            byte[] contentBuff = outputStream.toByteArray();
            //内容对象长度 int 4bytes
            byteBuf.writeInt(contentBuff.length);
            //内容数据
            byteBuf.writeBytes(contentBuff);
        }
        out.add(byteBuf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("请求编码失败！", cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        short magic = byteBuf.readShort();
        // 如果魔数不相等，那么认为这是一个无效的请求
        if (magic != HanguCons.MAGIC) {
            return;
        }
        byte msgType = byteBuf.readByte();
        // 判断请求类型
        int requstFlag = MsgTypeMarkEnum.REQUEST_FLAG.getMark() & msgType;
        // 请求id
        Long id = byteBuf.readLong();
        try {
            if ((MsgTypeMarkEnum.HEART_FLAG.getMark() & msgType) != 0) {

                PingPong pingPong = this.dealHeart(id);
                list.add(pingPong);
                // 响应
            } else if (requstFlag == 0) {
                byte commandType = (byte) (HanguCons.COMMAND_MARK & msgType);
                Response response = this.dealResponse(id, byteBuf, commandType);
                list.add(response);
            }
        } catch (RpcInvokerException e) {
            Response response = CommonUtils.createResponseInfo(id, e.getCode(), e.getClass(), e);
            ctx.writeAndFlush(response);
            throw e;
        } catch (IOException e) {
            RpcInvokerException cause = new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "反序列化失败！", e);
            Response response = CommonUtils.createResponseInfo(id, cause.getCode(), cause.getClass(),
                cause);
            ctx.writeAndFlush(response);
            throw e;
        } catch (Exception e) {
            Response response = CommonUtils.createResponseInfo(id, ErrorCodeEnum.FAILURE.getCode(),
                e.getClass(), e);
            ctx.writeAndFlush(response);
            throw e;
        }
    }

    private Response dealResponse(Long id, ByteBuf byteBuf, byte commandType)
        throws IOException, ClassNotFoundException {

        int bodyLength = byteBuf.readInt();
        byte[] body = new byte[bodyLength];
        // 读取内容
        byteBuf.readBytes(body);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        SerialInput serialInput = Hessian2SerialInputFactory.createSerialization(inputStream);
        // 响应编码
        int code = serialInput.readInt();
        String returnDesc = serialInput.readString();
        Class<?> clss = DescClassUtils.desc2class(returnDesc);
        Object value = serialInput.readObject(clss);
        RpcResult result = new RpcResult();
        result.setCode(code);
        result.setReturnType(clss);
        result.setResult(value);
        Response response = new Response();
        response.setId(id);
        response.setCommandType(commandType);
        response.setRpcResult(result);

        return response;
    }

    private PingPong dealHeart(Long id) {
        PingPong pingPong = new PingPong();
        pingPong.setId(id);
        return pingPong;
    }
}
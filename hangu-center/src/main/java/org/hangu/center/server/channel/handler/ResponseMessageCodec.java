package org.hangu.center.server.channel.handler;

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

/**
 * 响应编码与请求解码
 *
 * @author wuzhenhong
 * @date 2023/8/2 9:44
 */
@Slf4j
public class ResponseMessageCodec extends MessageToMessageCodec<ByteBuf, Response> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Response response, List<Object> out) throws Exception {

        ByteBuf byteBuf = ctx.alloc().buffer();
        // 魔数 2bytes
        byteBuf.writeShort(HanguCons.MAGIC);
        // 请求类型，命令类型方式 1bytes
        byte finalMsgType = (byte) (MsgTypeMarkEnum.REQUEST_FLAG.getMark() & 0);
        finalMsgType |= response.getCommandType();
        // 消息类型 1byte
        byteBuf.writeByte(finalMsgType);
        // 写入请求id
        byteBuf.writeLong(response.getId());
        // 写入内容
        RpcResult rpcResult = response.getRpcResult();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            SerialOutput serialOutput = Hessian2SerialOutputFactory.createSerialization(outputStream);
            int code = rpcResult.getCode();
            serialOutput.writeInt(code);
            Class<?> aClass = rpcResult.getReturnType();
            String typeDesc = DescClassUtils.getDesc(aClass);
            serialOutput.writeString(typeDesc);
            Object value = rpcResult.getResult();
            serialOutput.writeObject(value);
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        short magic = byteBuf.readShort();
        // 如果魔数不相等，那么认为这是一个无效的请求
        if (magic != HanguCons.MAGIC) {
            return;
        }
        byte msgType = byteBuf.readByte();
        // 请求id
        Long id = byteBuf.readLong();
        try {
            // 表示是来自客户端的请求
            if ((MsgTypeMarkEnum.REQUEST_FLAG.getMark() & msgType) == MsgTypeMarkEnum.REQUEST_FLAG.getMark()) {
                Request request = this.dealRequest(id, byteBuf, msgType);
                list.add(request);
                // 心跳
            } else if ((MsgTypeMarkEnum.HEART_FLAG.getMark() & msgType) == MsgTypeMarkEnum.HEART_FLAG.getMark()) {
                PingPong pingPong = this.dealHeart(id);
                list.add(pingPong);
            }
        } catch (RpcInvokerException e) {
            Response response = CommonUtils.createResponseInfo(id, e.getCode(), e.getClass(), e);
            ctx.channel().writeAndFlush(response);
            throw e;
        } catch (IOException e) {
            RpcInvokerException cause = new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "反序列化失败！", e);
            Response response = CommonUtils.createResponseInfo(id, cause.getCode(), cause.getClass(),
                cause);
            ctx.channel().writeAndFlush(response);
            throw e;
        } catch (Exception e) {
            Response response = CommonUtils.createResponseInfo(id, ErrorCodeEnum.FAILURE.getCode(),
                e.getClass(), e);
            ctx.channel().writeAndFlush(response);
            throw e;
        }
    }

    private Request dealRequest(Long id, ByteBuf byteBuf, byte msgType) throws IOException, ClassNotFoundException {

        int bodyLength = byteBuf.readInt();
        byte[] body = new byte[bodyLength];
        // 读取内容
        byteBuf.readBytes(body);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        SerialInput serialInput = Hessian2SerialInputFactory.createSerialization(inputStream);
        String desc = serialInput.readString();
        Class<?> clazz = DescClassUtils.desc2class(desc);
        Object content = serialInput.readObject(clazz);

        byte commandType = (byte) (msgType & HanguCons.COMMAND_MARK);
        boolean oneWay = (byte) (msgType & MsgTypeMarkEnum.ONE_WAY_FLAG.getMark()) != 0;

        Request request = new Request();
        request.setId(id);
        request.setOneWay(oneWay);
        request.setCommandType(commandType);
        request.setBody(content);

        return request;
    }

    private PingPong dealHeart(Long id) {
        PingPong pingPong = new PingPong();
        pingPong.setId(id);
        return pingPong;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("响应编码失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}
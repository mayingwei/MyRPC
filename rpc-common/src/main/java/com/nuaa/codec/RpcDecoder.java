package com.nuaa.codec;
import com.nuaa.serializer.CustomSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 自定义解码器
 *
 *
 * ByteToMessageDecoder 的作用就是接收字节流，并将其解码为应用程序可以处理的消息对象。
 */
public class RpcDecoder extends ByteToMessageDecoder {
    // 反序列化成 genericClass 类型的对象
    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }



    /**
     *
     * @Parm ChannelHandlerContext channelHandlerContext: 通道处理上下文对象，提供了与通道和处理器链相关的操作和状态。
     * 它可以用于与其他处理器进行交互，如发送消息、修改通道属性等。
     * @Parm ByteBuf in: 输入的字节流，包含待解码的数据。解码器将从这个字节流中读取数据进行解码。
     * @Parm List<Object> out: 输出列表，用于存储解码后的消息对象。解码器将解码后的消息对象添加到这个列表中，供后续的处理器进行处理。
     * */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        // 消息头占 4B，所以 "入站"数据（待解码的字节序列）的可读字节必须大于 4
        // 判断 ByteBuf 可读取字节
        if (in.readableBytes() < 4){
            return ;
        }
        // 标记当前readIndex的位置，以便后面重置 readIndex 的时候使用
        in.markReaderIndex();
        // 读取消息体（消息的长度）. readInt 操作会增加 readerIndex
        int dataLength = in.readInt();
        // 如果可读字节数小于消息长度，说明是不完整的消息
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return ;
        }
        // 开始反序列化
        byte[] body = new byte[dataLength];
        in.readBytes(body);
        Object obj = CustomSerializer.deserialize(body, genericClass);
        out.add(obj);
    }
}

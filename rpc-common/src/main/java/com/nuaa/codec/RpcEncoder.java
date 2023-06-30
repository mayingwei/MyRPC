package com.nuaa.codec;
import com.nuaa.serializer.CustomSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义编码器
 *
 * MessageToByteEncoder 的作用是接收应用程序定义的消息对象，并将其编码为字节流，以便通过网络进行传输。
 *
 * 消息长度 + 消息内容是项目开发中最常用的一种协议。
 * 消息头中存放消息的总长度，例如使用 4 字节的 int 值记录消息的长度，消息体实际的二进制的字节数据。
 * 接收方在解析数据时，首先读取消息头的长度字段 Len，然后紧接着读取长度为 Len 的字节数据，该数据即判定为一个完整的数据报文。
 */
public class RpcEncoder extends MessageToByteEncoder {
    //待编码的对象类型
    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }


    /**
     *
     * @Parm ChannelHandlerContext channelHandlerContext: 通道处理上下文对象，提供了与通道和处理器链相关的操作和状态。
     * 它可以用于与其他处理器进行交互，如发送消息、修改通道属性等
     * @Parm Object in: 输入的消息对象，即待编码的数据。编码器将对这个消息对象进行编码，将其转换为字节流表示形式。
     * @Parm ByteBuf out: 输出的字节缓冲区，用于存储编码后的字节流。编码器将编码后的字节流写入这个字节缓冲区中。
     * */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            // 将对象序列化为字节数组
            byte[] data = CustomSerializer.serialize(in);
            // 将消息体长度写入消息头
            out.writeInt(data.length);
            // 将数据写入消息体
            out.writeBytes(data);
        }
    }
}

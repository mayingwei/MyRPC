package com.nuaa.rpc.exception;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
/**
 * ChannelDuplexHandler 类用于处理网络通信的双向数据流。它是 Netty 框架中的一个抽象类，可以作为处理器添加到通信管道（ChannelPipeline）中，
 * 用于对入站（inbound）和出站（outbound）的数据进行处理和转换。
 *
 * 1 channelRead(ChannelHandlerContext ctx, Object msg): 当从远程端点接收到新的数据时，该方法被调用。
 * 你可以在这个方法中处理入站数据，进行解码、验证、转换等操作，并将处理后的数据传递给下一个处理器。
 *
 * 2 write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise): 当要发送数据到远程端点时，该方法被调用。
 * 你可以在这个方法中处理出站数据，进行编码、加密、转换等操作，并将处理后的数据传递给下一个处理器。
 *
 * 3 exceptionCaught(ChannelHandlerContext ctx, Throwable cause): 当发生异常时，该方法被调用。
 * 你可以在这个方法中处理异常情况，例如记录日志、关闭连接、发送错误响应等。
 * */
public class ExceptionHandler extends ChannelDuplexHandler {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof RuntimeException) {
            System.out.println("Handle Business Exception Success.");
        }
    }
}
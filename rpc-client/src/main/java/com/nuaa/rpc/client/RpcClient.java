package com.nuaa.rpc.client;

import com.nuaa.codec.RpcDecoder;
import com.nuaa.codec.RpcEncoder;
import com.nuaa.entity.RpcRequest;
import com.nuaa.entity.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 客户端（建立连接，发送 RPC 请求，接收 RPC 响应）
 *
 * * SimpleChannelInboundHandler用于处理入站消息（即从客户端接收到的消息）
 *  * 1 提供泛型类型参数：RpcRequest ，重写 channelRead0() 方法时，方法的参数类型就是指定的泛型类型，使得处理消息更加方便和类型安全。
 *  * 2 自动释放资源：SimpleChannelInboundHandler 在处理完消息后，会自动释放消息相关的资源，包括 ByteBuf 缓冲区对象。
 *  * 3 提供默认实现：开发者只需要重写 channelRead0() 方法，实现自定义的消息处理逻辑即可，无需关心其他方法的实现。
 *  * 3 提供异常处理：SimpleChannelInboundHandler 提供了对异常的处理机制。当出现异常时，可以通过重写 exceptionCaught() 方法来处理异常情况，进行相应的错误处理和日志记录。
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    /**
     * RPC 服务器的主机名和端口号。
     * */
    private final String host;
    private final int port;

    /**
     * RPC 响应
     * */
    private RpcResponse response;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 处理服务端发送过来的响应消息 RpcResponse
     * @param channelHandlerContext
     * @param response
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) throws Exception {
        this.response = response;
    }

    /**
     * 发生异常时此方法被调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("api caught exception", cause);
        ctx.close();
    }

    /**
     * 建立连接，发送请求，接收响应
     * 该方法的真正调用在代理类 RpcProxy 中（通过代理对此方法进行增强，屏蔽远程方法调用的细节）
     * @param rpcRequest RPC 请求对象
     * @return RPC 响应对象
     * @throws InterruptedException 当线程被中断时抛出异常
     */
    public RpcResponse send(RpcRequest rpcRequest) throws InterruptedException {
        // 创建 NIO 事件循环组
        // NioEventLoopGroup是一个多线程事件循环组，用于处理网络事件，例如接受连接、读写数据等。
        // 1.线程管理：NioEventLoopGroup使用线程池来管理一组线程，这些线程负责处理客户端和服务器端的网络事件。
        // 2.事件循环：每个NioEventLoop都是一个事件循环，它负责处理I/O事件和任务执行。
        // 3.线程调度：NioEventLoopGroup负责将连接分配给可用的NioEventLoop线程进行处理。

        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建 Bootstrap 实例
            // Bootstrap类的作用是用于配置和启动Netty客户端的引导程序。
            // 它提供了一些方法和功能来设置客户端的参数、协议、处理器等，并最终启动客户端与服务器进行通信。
            Bootstrap bootstrap = new Bootstrap();
            // 指定事件循环组
            bootstrap.group(group);
            // 指定通道类型为 NIO Socket 通道，异步 TCP 客户端
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline pipeline = socketChannel.pipeline();
                    // 添加编码器，将请求对象转换为字节流
                    pipeline.addLast(new RpcEncoder(RpcRequest.class));
                    // 添加解码器，将字节流转换为响应对象
                    pipeline.addLast(new RpcDecoder(RpcResponse.class));
                    // 添加自身为处理器，用于处理 RPC 响应
                    pipeline.addLast(RpcClient.this);
                }
            });
            // 设置 TCP_NODELAY 选项，Netty 默认是 true，表示立即发送数据。
            // 如果设置为 false 表示启用 Nagle 算法，该算法会将 TCP 网络数据包累积到一定量才会发送，
            // 虽然可以减少报文发送的数量，但是会造成一定的数据延迟。Netty 为了最小化数据传输的延迟，默认禁用了 Nagle 算法
            bootstrap.option(ChannelOption.TCP_NODELAY, true);

            //设置为 true 代表启用了 TCP SO_KEEPALIVE 属性，TCP 会主动探测连接状态，即连接保活
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

            // 连接 RPC 服务器,发起与指定host和port的连接，并等待连接操作完成。
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();

            // 写入 RPC 请求
            // 获取到与连接操作关联的 Channel 对象，Channel 表示一个与远程服务器的连接，可以用于发送和接收数据。
            Channel channel = channelFuture.channel();
            // rpcRequest 对象写入到 channel 中，并等待发送操作完成
            channel.writeAndFlush(rpcRequest).sync();

            // 关闭连接,当前线程中等待通道关闭操作完成
            channel.closeFuture().sync();

            // 返回 RPC 响应对象
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }
}

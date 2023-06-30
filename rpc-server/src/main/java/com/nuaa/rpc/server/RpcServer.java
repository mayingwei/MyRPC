package com.nuaa.rpc.server;

import com.nuaa.codec.RpcDecoder;
import com.nuaa.codec.RpcEncoder;
import com.nuaa.entity.RpcRequest;
import com.nuaa.entity.RpcResponse;
import com.nuaa.rpc.exception.ExceptionHandler;
import com.nuaa.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ApplicationContextAware接口
 * 作用:获取带有@RpcService 注解的Bean
 * 实现:ApplicationContextAware是Spring框架中的一个接口，用于获取当前应用程序的上下文（ApplicationContext）。
 * 通过实现该接口并实现setApplicationContext方法，可以在Bean中获取到应用程序上下文对象，从而访问Spring容器的其他Bean。
 *
 * InitializingBean接口
 * 作用: 初始化工作：启动 Netty 服务器进行服务端和客户端的通信，接收并处理客户端发来的请求,并且还要将服务名称和服务地址注册进 Zookeeper（注册中心）
 * 实现:用于在Bean实例化之后和依赖注入完成之后执行初始化操作。
 * 通过实现该接口并实现其中的afterPropertiesSet()方法，可以在Bean的生命周期中进行一些特定的初始化工作。
 *
 * */
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    /**
     * 服务地址（比如服务被暴露在 Netty 的 8000 端口，服务地址就是 127.0.0.1:8000）
     * */
    private String serviceAddress;

    /**
     * 服务注册组件（Zookeeper）
     * */
    private ServiceRegistry serviceRegistry;

    /**
     * 存储服务名称与服务对象之间的映射关系
     * */
    private Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    /**
     * 该构造函数用于提供给用户在配置文件中注入服务地址和服务注册组件
     * @param serviceAddress
     * @param serviceRegistry
     */
    public RpcServer(String serviceAddress, ServiceRegistry serviceRegistry) {
        this.serviceAddress = serviceAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Spring 容器在加载的时候会自动调用一次 setApplicationContext, 并将上下文 ApplicationContext 传递给这个方法
     * 该方法的作用就是获取带有 @RpcSerivce 注解的类的 value (被暴露的实现类的接口名称) 和 version (被暴露的实现类的版本号，默认为 “”)
     * @param applicationContext 应用程序上下文对象
     * @throws BeansException 如果在设置应用程序上下文时发生异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 获取带有 @RpcService 注解的服务对象
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
           for (Object serviceBean: serviceBeanMap.values()) {
               RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
               // 获取服务名称
               String serviceName = rpcService.interfaceName().getName();
               // 获取服务版本
               String serviceVersion = rpcService.serviceVersion();
               if(serviceVersion != null){
                   serviceVersion = serviceVersion.trim();
                   if(!StringUtils.isEmpty(serviceVersion)){
                       serviceName = serviceName + "-" + serviceVersion;
                   }
               }
               handlerMap.put(serviceName, serviceBean);
               //LOGGER.info("serviceName:"+serviceName+" 已注册");
           }
        }
    }

    /**
     * 在初始化 Bean 的时候会自动执行该方法
     * 该方法的目标就是启动 Netty 服务器进行服务端和客户端的通信，接收并处理客户端发来的请求,
     * 并且还要将服务名称和服务地址注册进 Zookeeper（注册中心）
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //1 NioEventLoopGroup是Netty框架中用于处理I/O事件的多线程事件循环器。它负责管理处理客户端连接请求和网络I/O操作的线程池。
        // Boss 线程组负责接受传入的连接请求，并将接受的连接请求注册到 Worker 线程组中的某个线程上进行处理。Boss 线程组的作用是处理连接的接受和分发，不会进行实际的数据处理。
        // Worker 线程组负责处理接受连接后的数据处理，包括读取数据、解码、处理业务逻辑以及发送响应数据等操作。
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //2 创建ServerBootstrap实例，用于设置服务器的参数
            // ServerBootstrap是Netty框架中用于创建和配置服务器的引导类。它提供了一组用于配置服务器的方法，
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            //3 设置bossGroup和workerGroup，主从多线程模型
            // 可以将 Boss 线程组和 Worker 线程组设置给 ServerBootstrap 对象，用于处理服务器的连接和数据处理
            serverBootstrap.group(bossGroup, workerGroup);

            //4 指定使用的channel类型为NioServerSocketChannel，异步 TCP 服务端。
            // 表示服务器将使用NIO（非阻塞I/O）的网络通道来处理传入的连接请求，它提供了异步的、基于事件驱动的处理方式，能够更高效地处理大量的并发连接。
            serverBootstrap.channel(NioServerSocketChannel.class);

            //5 childOption 对应的是 Worker 线程组，保持连接状态为true
            // 当启用SO_KEEPALIVE选项时，操作系统会定期向对方发送心跳探测，以检测连接是否已经断开或变得不可用。
            // 如果连接断开或不可用，操作系统会关闭连接，以便服务器端能够及时释放相关资源。
            // 当2个小时没有发生数据交换时，TCP会发送一个探针给对方，如果收到的是ACK标记的应答，则连接保持，否则关闭连接。
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            //6 设置子通道的处理器，初始化ChannelPipeline
            // 用于为每个接受的子通道设置一个ChannelInitializer。在这个ChannelInitializer的initChannel()方法中
            // 添加自定义的解码器、编码器和处理器，以实现对RPC请求的处理和响应。
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    // 获取ChannelPipeline，用于管理处理器链
                    ChannelPipeline pipeline = socketChannel.pipeline();

                    //1. IdleStateHandler 是 netty 提供的处理空闲状态的处理器
                    //2. long readerIdleTime : 表示多长时间没有读, 就会发送一个心跳检测包检测是否连接
                    //3. long writerIdleTime : 表示多长时间没有写, 就会发送一个心跳检测包检测是否连接
                    //4. long allIdleTime : 表示多长时间没有读写, 就会发送一个心跳检测包检测是否连接
                    //30 秒之内没有收到客户端请求的话就关闭连接
                    LOGGER.info("初始化");
                    pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));

                    // 添加自定义的解码器RpcDecoder
                    pipeline.addLast(new RpcDecoder(RpcRequest.class));
                    // 添加自定义的编码器RpcEncoder
                    pipeline.addLast(new RpcEncoder(RpcResponse.class));

                    // 添加自定义的处理器RpcServerHandler，用于处理RPC请求
                    pipeline.addLast(new RpcServerHandler(handlerMap));

                    //加入自定义异常处理
                    pipeline.addLast(new ExceptionHandler());

                }
            });

            //7 解析服务地址和端口号
            String[] addressArray = StringUtils.split(serviceAddress, ":");
            String ip = addressArray[0];
            int port = Integer.parseInt(addressArray[1]);

            //8 绑定服务地址和端口号，并返回一个ChannelFuture
            LOGGER.info("服务端绑定服务地址"+ip);
            LOGGER.info("服务端绑定服务端口号"+port);
            ChannelFuture future = serverBootstrap.bind(ip, port).sync();

            //9 注册服务到服务注册中心
            if (serviceRegistry != null) {
                // 遍历handlerMap中的接口名称，将服务注册到注册中心
                for (String interfaceName : handlerMap.keySet()) {
                    serviceRegistry.register(interfaceName, serviceAddress);
                    LOGGER.info("服务注册: {} => {}", interfaceName, serviceAddress);
                }
            }



            //10 阻塞等待服务器通道关闭
            future.channel().closeFuture().sync();
        } finally {
            //11 关闭NioEventLoopGroup 优雅下线
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}

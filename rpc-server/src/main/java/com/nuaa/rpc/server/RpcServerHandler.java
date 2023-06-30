package com.nuaa.rpc.server;

import com.nuaa.entity.RpcRequest;
import com.nuaa.entity.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * RPC 服务端处理器，接收请求并响应
 *
 * SimpleChannelInboundHandler用于处理入站消息（即从客户端接收到的消息）
 * 1 提供泛型类型参数：RpcRequest ，重写 channelRead0() 方法时，方法的参数类型就是指定的泛型类型，使得处理消息更加方便和类型安全。
 * 2 自动释放资源：SimpleChannelInboundHandler 在处理完消息后，会自动释放消息相关的资源，包括 ByteBuf 缓冲区对象。
 * 3 提供默认实现：开发者只需要重写 channelRead0() 方法，实现自定义的消息处理逻辑即可，无需关心其他方法的实现。
 * 3 提供异常处理：SimpleChannelInboundHandler 提供了对异常的处理机制。当出现异常时，可以通过重写 exceptionCaught() 方法来处理异常情况，进行相应的错误处理和日志记录。
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);

    // 存储服务名称及服务对象之间的映射关系
    private final Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    /**
     * 处理/响应客户端的请求消息
     @param channelHandlerContext ChannelHandlerContext 对象，用于与客户端进行通信
     @param rpcRequest RPC 请求对象，包含客户端发送的请求信息
     @throws Exception 可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        // 创建 RPC 响应对象
        RpcResponse rpcResponse = new RpcResponse();
        // 将请求的唯一标识设置到响应对象中
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        try {
            // 调用核心处理方法，处理客户端请求，返回处理结果
            Object result = handle(rpcRequest);
            // 将处理结果设置到响应对象中
            rpcResponse.setResult(result);
        } catch (Exception e) {
            // 发生异常时，记录错误日志
            LOGGER.error("handle result failure", e);
            // 将异常设置到响应对象中
            rpcResponse.setException(e);
        }
        // 1 用于将 RPC 响应对象写入通道并发送给客户端
        // 2 添加一个 ChannelFutureListener 监听器，用于在写入完成后关闭连接。
        channelHandlerContext.writeAndFlush(rpcResponse).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }

    /**
     * 获取客户端请求的方法和参数，通过反射进行调用）
     *@param rpcRequest RPC 请求对象，包含客户端发送的请求信息
     *@return 处理结果对象
     *@throws NoSuchMethodException 当请求的方法不存在时抛出该异常
     *@throws InvocationTargetException 当反射调用方法时抛出该异常
     *@throws IllegalAccessException 当反射调用方法时抛出该异常
     */
    private Object handle(RpcRequest rpcRequest) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // 获取服务名和服务版本
        String serviceName = rpcRequest.getInterfaceName();
        String serviceVersion = rpcRequest.getServiceVersion();
        if(serviceVersion != null){
            serviceVersion = serviceVersion.trim();
            if(!StringUtils.isEmpty(serviceVersion)){
                serviceName += "-" + serviceVersion;
            }
        }

        // 获取服务对象
        Object serviceBean = handlerMap.get(serviceName);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("can not find service bean by key: %s", serviceName));
        }
        LOGGER.info(serviceName+" 发现");
        // 获取反射调用所需的参数 类信息、方法名、参数值和参数类型
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = rpcRequest.getMethodName();
        Object[] parameters = rpcRequest.getParameters();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        // 通过反射调用客户端请求的方法
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        LOGGER.info(serviceName+" 调用");
        return method.invoke(serviceBean, parameters);
    }
}

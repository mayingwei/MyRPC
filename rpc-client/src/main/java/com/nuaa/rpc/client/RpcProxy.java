package com.nuaa.rpc.client;

import com.nuaa.entity.RpcRequest;
import com.nuaa.entity.RpcResponse;
import com.nuaa.rpc.registry.ServiceDiscovery;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * RPC 动态代理
 * 代理客户端进行建立连接，发送请求，接收请求（即屏蔽远程方法调用细节）
 */
public class RpcProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);
    // 服务地址
    private String serviceAddress;
    // 服务发现组件
    private ServiceDiscovery serviceDiscovery;

    /**
     * 该构造函数用于提供给用户通过配置文件注入服务发现组件
     * @param serviceDiscovery
     */
    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * 对 send 方法进行增强
     * 使用示例：HelloService helloServiceImpl = rpcProxy.create(HelloService.class);
     * @param interfaceClass 接口类
     * @param <T>
     * @return 返回接口实例
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass) {
        return create(interfaceClass, "");
    }

    /**
     * 对 send 方法进行增强
     * 使用示例：HelloService helloService2 = rpcProxy.create(HelloService.class, "sample.hello2");
     * @param interfaceClass 接口类
     * @param serviceVersion 服务版本号
     * @param <T> 返回接口实例
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass, final String serviceVersion) {
        // 使用 CGLIB 动态代理机制
        Enhancer enhancer = new Enhancer();
        // 设置类加载器
        enhancer.setClassLoader(interfaceClass.getClassLoader());
        // 设置父类（接口类）
        enhancer.setSuperclass(interfaceClass);
        enhancer.setCallback(new MethodInterceptor() {
            /**
             * 方法拦截器，拦截被代理对象的方法调用
             *
             * @param o           被代理的对象（需要增强的对象）
             * @param method      被拦截的方法（需要增强的方法）
             * @param args        方法入参
             * @param methodProxy 用于调用原始方法的代理对象
             * @return 方法执行结果
             * @throws Throwable 抛出的异常
             */
            @Override
            public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                // 创建 RPC 请求并设置属性
                RpcRequest rpcRequest = new RpcRequest();
                // 设置请求ID
                rpcRequest.setRequestId(UUID.randomUUID().toString());
                // 设置方法名
                rpcRequest.setMethodName(method.getName());
                // 设置参数类型
                rpcRequest.setParameterTypes(method.getParameterTypes());
                // 设置参数值
                rpcRequest.setParameters(args);
                // 设置接口名
                rpcRequest.setInterfaceName(interfaceClass.getName());
                // 设置服务版本号
                rpcRequest.setServiceVersion(serviceVersion);

                // 根据服务名称和版本号查询服务地址
                if (serviceDiscovery != null) {
                    // 获取服务名称
                    String serviceName = interfaceClass.getName();
                    if (serviceVersion != null) {
                        // 获取服务版本号
                        String service_Version = serviceVersion.trim();
                        if (!StringUtils.isEmpty(service_Version)) {
                            serviceName += "-" + service_Version;
                        }
                    }
                    // 获取服务地址（用于建立连接）
                    try {
                        serviceAddress = serviceDiscovery.discovery(serviceName);
                    }catch(Exception e){
                        System.out.println("Exception thrown  :" + e);
                        return e.toString();
                    }
                    LOGGER.info("discover service: {} => {}", serviceName, serviceAddress);
                }

                // 检查服务地址是否为空
                if (serviceAddress != null) {
                    serviceAddress = serviceAddress.trim();
                    if (StringUtils.isEmpty(serviceAddress)) {
                        throw new RuntimeException("server address is empty");
                        //System.out.println("server address is empty");
                        //return "server address is empty";
                    }
                }

                // 解析主机名与端口号
                String[] array = StringUtils.split(serviceAddress, ":");
                // 主机名
                String host = array[0];
                // 端口号
                int port = Integer.parseInt(array[1]);

                // 创建 RPC 客户端对象，建立连接/发送请求/接收请求
                RpcClient client = new RpcClient(host, port);
                // 记录当前时间
                long time = System.currentTimeMillis();
                // 发送 RPC 请求并获取响应
                LOGGER.info("RPC请求:"+rpcRequest.toString());
                LOGGER.info("请求主机名:"+host);
                LOGGER.info("请求端口号:"+port);
                RpcResponse rpcResponse = client.send(rpcRequest);
                System.out.println("RPC响应:"+rpcResponse.toString());
                LOGGER.info("耗时time: {}ms", System.currentTimeMillis() - time);

                // 处理 RPC 响应结果
                if (rpcResponse == null) {
                    throw new RuntimeException("response is null");
                }
                if (rpcResponse.hasException()) {
                    // 响应中包含异常信息，抛出异常
                    throw rpcResponse.getException();
                }
                else {
                    // 响应中包含结果，返回结果
                    return rpcResponse.getResult();
                }
            }
        });
        // 创建代理对象并返回
        return (T) enhancer.create();
    }


}

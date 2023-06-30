package com.nuaa.rpc.sample.client;

import com.nuaa.rpc.client.RpcProxy;
import com.nuaa.rpc.sample.api.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HelloClient {

    public static void main(String[] args) throws Exception {

        // 加载 Spring 配置文件
        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        // 获取 RpcProxy 动态代理对象
        RpcProxy rpcProxy = context.getBean(RpcProxy.class);

        /**
         * 测试 HelloService 接口的实现类 1
         */
        // 调用 RpcProxy 对象的 create 方法来创建 RPC 代理接口
        HelloService helloServiceImpl1 = rpcProxy.create(HelloService.class,"v1.0");
        // 调用 RPC 代理接口的方法(调用远程接口方法就像调用本地方法一样简单）
        String result = helloServiceImpl1.hello("Jack1");

        /**
         * 测试 HelloService 接口的实现类 2
         */
        HelloService helloServiceImpl2 = rpcProxy.create(HelloService.class, "v2.0");
        String result2 = helloServiceImpl2.hello("Jack2");

        /**
         * 测试 HelloService 接口的实现类 3
         */
        HelloService helloServiceImpl3 = rpcProxy.create(HelloService.class, "v3.0");
        helloServiceImpl3.hello("Tom3");

        /**
         * 测试 HelloService 接口的实现类 4
         */
        HelloService helloServiceImpl4 = rpcProxy.create(HelloService.class, "v4.0");
        helloServiceImpl4.hello("Tom4");


    }
}

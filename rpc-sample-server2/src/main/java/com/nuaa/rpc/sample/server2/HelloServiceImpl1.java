package com.nuaa.rpc.sample.server2;

import com.nuaa.rpc.sample.api.HelloService;
import com.nuaa.rpc.server.RpcService;

@RpcService(interfaceName = HelloService.class,serviceVersion="v1.0")
public class HelloServiceImpl1 implements HelloService {

    @Override
    public String hello(String name) {
        return "server2: "+name + " Hello from " + "HelloServiceImpl1" ;
    }
}

package com.nuaa.rpc.sample.server2;

import com.nuaa.rpc.sample.api.HelloService;
import com.nuaa.rpc.server.RpcService;

//@RpcService(interfaceName = HelloService.class,serviceVersion="v3.0")
public class HelloServiceImpl3 implements HelloService {

    @Override
    public String hello(String name) {
        return name + " Hello from " + "HelloServiceImpl3" ;
    }
}

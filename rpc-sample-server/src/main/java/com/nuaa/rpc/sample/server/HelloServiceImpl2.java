package com.nuaa.rpc.sample.server;

import com.nuaa.rpc.sample.api.HelloService;
import com.nuaa.rpc.server.RpcService;

//@RpcService(interfaceName = HelloService.class,serviceVersion="v2.0")
public class HelloServiceImpl2 implements HelloService {

    @Override
    public String hello(String name) {
        return name + " GoodBye from " + "HelloServiceImpl2" ;
    }
}

package com.nuaa.rpc.registry;

import com.nuaa.rpc.registry.zookeeper.ZookeeperServiceDiscovery;
import com.nuaa.rpc.registry.zookeeper.ZookeeperServiceRegistry;

/**
 * @Description TODO
 */
public class Test {

    public static void main(String[] args) {
        ServiceRegistry registry = new ZookeeperServiceRegistry("10.233.7.109:2181");
        registry.register("rpc", "192.168.20.49:8080");
        ServiceDiscovery discovery = new ZookeeperServiceDiscovery("10.233.7.109:2181");
        String address = discovery.discovery("rpc");
        System.out.println("服务RPC的地址是：" + address);
    }
}

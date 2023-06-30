package com.nuaa.rpc.registry.zookeeper;

import com.nuaa.rpc.registry.ServiceRegistry;
import com.nuaa.rpc.registry.constant.Constant;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 使用 Zookeeper 实现服务注册（使用 Zookeeper 客户端 ZkClient）
 */
public class ZookeeperServiceRegistry implements ServiceRegistry {

    // slf4j 日志
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperServiceRegistry.class);

    // Zookeeper 客户端 ZkClient
    private ZkClient zkClient;

    /**
     * 该构造方法提供给用户（用户通过配置文件指定 zkAddress 完成服务注册组件的注入）
     * @param zkAddress 注册中心地址
     */
    public ZookeeperServiceRegistry(String zkAddress) {
        // 服务器地址
        String zkServers = zkAddress ;
        // 会话超时时间
        int sessionTimeout = Constant.ZK_SESSION_TIMEOUT;
        // 连接超时时间
        int connectionTimeout = Constant.ZK_CONNECTION_TIMEOUT;
        zkClient = new ZkClient(zkServers, sessionTimeout,connectionTimeout);
        LOGGER.info("zookeeper Registry");
    }

    /**
     * 服务注册
     * @param serviceName 服务名称（被暴露的实现类的接口名称）
     * @param serviceAddress 服务地址（比如该服务被暴露在 Netty 的 8000 端口，则服务地址为 127.0.0.1:8000）
     */
    @Override
    public void register(String serviceName, String serviceAddress) {
        // 持久节点：该数据节点被创建后，就会一直存在于zookeeper服务器上，直到有删除操作来主动删除这个节点。
        // 创建 registry 持久节点，该节点下存放所有的 service 节点
        String registryPath = Constant.ZK_REGISTRY_PATH;
        LOGGER.info("持久注册节点路径: {}", registryPath);
        if (!zkClient.exists(registryPath)) {
            zkClient.createPersistent(registryPath);
            LOGGER.info("创建持久注册节点: {}", registryPath);
        }
        // 在 registry 节点下创建 service 持久节点，存放服务名称
        String servicePath = registryPath + "/" + serviceName;
        LOGGER.info("持久服务节点路径: {}", servicePath);
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
            LOGGER.info("创建持久服务节点: {}", servicePath);
        }
        // 在 service 节点下创建 address 临时节点,存放服务地址
        // 临时节点的生命周期和客户端会话绑定在一起，客户端会话失效，则这个节点就会被自动清除。
        String addressPath = servicePath + "/address-";
        String addressNode = zkClient.createEphemeralSequential(addressPath, serviceAddress);
        LOGGER.info("创建临时服务节点: {}", addressNode);
        LOGGER.info("临时节点对应地址: {}", serviceAddress);
    }
}

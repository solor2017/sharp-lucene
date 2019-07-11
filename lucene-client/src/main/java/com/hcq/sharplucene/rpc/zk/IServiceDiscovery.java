package com.hcq.sharplucene.rpc.zk;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public interface IServiceDiscovery {

    /**
     * 获取服务地址
     * @param serviceName
     * @return
     */
    String discover(String serviceName);
}

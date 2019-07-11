package com.hcq.sharplucene.rpc.zk;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public interface IRegisterCenter {

    /**
     * 注册服务名称和服务地址
     * @param serviceName
     * @param serviceAddress
     */
    void register(String serviceName, String serviceAddress);
}

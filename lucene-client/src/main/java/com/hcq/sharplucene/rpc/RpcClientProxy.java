package com.hcq.sharplucene.rpc;

import com.hcq.sharplucene.rpc.zk.IServiceDiscovery;

import java.lang.reflect.Proxy;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class RpcClientProxy {

    private IServiceDiscovery serviceDiscovery;

    public RpcClientProxy(IServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * 创建客户端的远程代理。通过远程代理进行访问
     * @param interfaceCls
     * @param <T>
     * @return
     */
    public <T> T clientProxy(final Class<T> interfaceCls,String version,Object... construct){
        //使用到了动态代理。
        return (T)Proxy.newProxyInstance(interfaceCls.getClassLoader(),
                new Class[]{interfaceCls},new RemoteInvocationHandler(serviceDiscovery,version,construct));
    }




}

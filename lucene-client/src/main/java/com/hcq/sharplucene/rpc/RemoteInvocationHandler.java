package com.hcq.sharplucene.rpc;

import com.hcq.sharplucene.rpc.zk.IServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Slf4j
public class RemoteInvocationHandler implements InvocationHandler {
    private IServiceDiscovery serviceDiscovery;

    private String version;

    private Object[] construct;

    public RemoteInvocationHandler(IServiceDiscovery serviceDiscovery,String version,Object... construct) {
        this.serviceDiscovery=serviceDiscovery;
        this.version=version;
        this.construct = construct;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //组装请求
        RpcRequest request=new RpcRequest();
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameters(args);
        request.setVersion(version);
        request.setConstryctArgs(construct);

        String serviceAddress=serviceDiscovery.discover(request.getClassName()); //根据接口名称得到对应的服务地址
        //通过tcp传输协议进行传输
        TCPTransport tcpTransport=new TCPTransport(serviceAddress);
        //发送请求
        log.info("tcpTransport.rpcInvoke(request)=="+tcpTransport.rpcInvoke(request));
        return tcpTransport.rpcInvoke(request);
    }
}

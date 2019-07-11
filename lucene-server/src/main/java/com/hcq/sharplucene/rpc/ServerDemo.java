package com.hcq.sharplucene.rpc;

import com.hcq.sharplucene.core.client.IndexService;
import com.hcq.sharplucene.core.client.LocalIndexService;
import com.hcq.sharplucene.rpc.zk.IRegisterCenter;
import com.hcq.sharplucene.rpc.zk.RegisterCenterImpl;

import java.io.IOException;

/**
 * @Author: solor
 * @Since: 2019/7/8
 * @Description:ddd
 */
public class ServerDemo {
    public static void main(String[] args) throws IOException {
        IRegisterCenter registerCenter=new RegisterCenterImpl();
        RpcServer rpcServer=new RpcServer(registerCenter,"127.0.0.1:8080");
        rpcServer.start();
        System.in.read();
    }
}

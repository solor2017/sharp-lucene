package com.hcq.sharplucene.rpc.zk;

import com.hcq.sharplucene.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Slf4j
public class RegisterCenterImpl implements IRegisterCenter{

    private CuratorFramework curatorFramework;

    {
        curatorFramework=CuratorFrameworkFactory.builder().
                connectString(Config.getInstance().getKey("zk.address")).
                sessionTimeoutMs(4000).
                retryPolicy(new ExponentialBackoffRetry(1000,
                        10)).build();
        curatorFramework.start();
    }

    @Override
    public void register(String serviceName, String serviceAddress) {
        //注册相应的服务

        String servicePath=Config.getInstance().getKey("zk.registypth")+"/"+serviceName;

        try {
            //判断 /registrys/...，不存在则创建
            if(curatorFramework.checkExists().forPath(servicePath)==null){
                curatorFramework.create().creatingParentsIfNeeded().
                        withMode(CreateMode.PERSISTENT).forPath(servicePath,"0".getBytes());
            }

            String addressPath=servicePath+"/"+serviceAddress;
            String rsNode=curatorFramework.create().withMode(CreateMode.EPHEMERAL).
                    forPath(addressPath,"0".getBytes());
            log.info("服务注册成功："+rsNode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

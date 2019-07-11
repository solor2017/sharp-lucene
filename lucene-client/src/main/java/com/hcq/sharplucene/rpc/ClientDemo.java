package com.hcq.sharplucene.rpc;

import com.hcq.sharplucene.core.client.IndexService;
import com.hcq.sharplucene.core.client.QueryResults;
import com.hcq.sharplucene.core.sample.SampleJavaBean;
import com.hcq.sharplucene.rpc.zk.IServiceDiscovery;
import com.hcq.sharplucene.rpc.zk.ServiceDiscoveryImpl;
import com.hcq.sharplucene.rpc.zk.ZkConfig;
import com.hcq.sharplucene.util.Config;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class ClientDemo {

    public static void main(String[] args) throws Exception {

        IServiceDiscovery serviceDiscovery=new ServiceDiscoveryImpl(Config.getInstance().getKey("zk.address"));
        RpcClientProxy rpcClientProxy=new RpcClientProxy(serviceDiscovery);

//        for(int i=0;i<10;i++) {
            IndexService indexService = rpcClientProxy.clientProxy(IndexService.class, null,"COMMENT");
//            for (int j = 0; j < 6; j++){
//                SampleJavaBean bean = new SampleJavaBean();
//                bean.setCheckFlag(true);
//                bean.setRegistTime(new Date());
//                bean.setUrl("www.baidu.com");
//                bean.setUserName("solor");
//                bean.setCommentId(20000 );
////                try {
//                    indexService.add(bean);
//        System.out.println(indexService.sayHello("aaa"));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            String queryString = "userName='solor'";
            try
            {
                QueryResults queryResults = indexService.query(queryString, 1, 500, true);
                System.out.println("PageNo :" + queryResults.getPageNo());
                System.out.println("PageSize :" + queryResults.getPageSize());
                System.out.println("TotalHit :" + queryResults.getTotalHit());
                System.out.println("TotalPage :" + queryResults.getTotalPage());

                List<SampleJavaBean> beanList = queryResults.getResultBeans(SampleJavaBean.class);

                for (SampleJavaBean bean : beanList)
                    System.out.println(bean.getCommentId() + " | " + bean.getUserName());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
//            System.out.println(indexService.sayHello("lll"));
//        }
//        for (int i = 0;i<5;i++) {
//            Hello hello = rpcClientProxy.clientProxy(Hello.class,null,null);
            System.out.println(indexService.sayHello("lll"));
//        }

    }
}

package com.hcq.sharplucene.rpc.zk.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * @Author: solor
 * @Author: solor
 * @Since: 2.0
 * @Description:随机
 */
public class RandomLoadBanalce extends AbstractLoadBanance{

    @Override
    protected String doSelect(List<String> repos) {
        int len=repos.size();
        Random random=new Random();
        return repos.get(random.nextInt(len));
    }
}

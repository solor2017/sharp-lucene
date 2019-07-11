package com.hcq.sharplucene.rpc.zk.loadbalance;

import java.util.List;

/**
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public interface LoadBanalce {

    String selectHost(List<String> repos);
}



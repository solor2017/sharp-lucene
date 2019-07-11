package com.hcq.sharplucene.core.client;

import java.net.URL;
/**
 * 索引服务工厂类
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class IndexServiceFactory {
	/**
	 * 获取本地索引服务
	 * @param indexName
	 * @return
	 */
	public static IndexService getLocalIndexService(String indexName) {
		return new LocalIndexService(indexName);
	}
	/**
	 * http 获取远程索引服务
	 * @param indexName
	 * @param remoteHttpURL
	 * @return
	 */
	public static IndexService getRemoteIndexService(String indexName,URL remoteHttpURL) {
		return new RemoteIndexService(indexName, remoteHttpURL);
	}
}

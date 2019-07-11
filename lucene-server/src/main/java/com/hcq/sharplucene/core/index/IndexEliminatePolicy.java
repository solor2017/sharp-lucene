package com.hcq.sharplucene.core.index;

import org.apache.lucene.search.Query;
/**
 * 索引淘汰操作接口
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public abstract interface IndexEliminatePolicy {
	/**
	 * 获取索引淘汰操作条件
	 * @param paramIndexConfig
	 * @return
	 */
	public abstract Query getEliminateCondition(IndexConfig paramIndexConfig);
}

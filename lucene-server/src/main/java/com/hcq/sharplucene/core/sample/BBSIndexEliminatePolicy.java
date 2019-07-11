package com.hcq.sharplucene.core.sample;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import com.hcq.sharplucene.core.index.IndexConfig;
import com.hcq.sharplucene.core.index.IndexEliminatePolicy;

/**
 * 索引淘汰策略
 * @Author: solor
 * @Description:
 */
public class BBSIndexEliminatePolicy implements IndexEliminatePolicy {
	
	public Query getEliminateCondition(IndexConfig indexConfig) {
		long timeMillis = System.currentTimeMillis()- indexConfig.getMigrateCritical();
		String timeQueryStr = new SimpleDateFormat("yyyyMMdd000000").format(new Date(timeMillis));
		Query query = new TermRangeQuery("POSTTIME", null, timeQueryStr, false,true);
		return query;
	}
}
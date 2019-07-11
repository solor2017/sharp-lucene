package com.hcq.sharplucene.core.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.hcq.sharplucene.util.SpringContextLoader;

/**
 * 多索引上下文容器
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public class IndexContextContainer {
	private static final String SPRING_ID = "HXLuci";
	/**
	 * 多个索引配置
	 */
	private List<IndexConfig> indexConfigs;
	/**
	 * 索引上下文映射
	 * IndexName --> IndexContext
	 * IndexContext持有对当前索引库的特征
	 */
	private HashMap<String, IndexContext> indexContexts;

	private IndexContextContainer() {
		this.indexConfigs = new ArrayList(4);
		this.indexContexts = new HashMap(4);
//		if (SingletonHolder.INSTANCE!=null) { throw new RuntimeException("不允许创建多个实例");}
	}
	/**
	 * 单例模式构造器
	 * @return
	 */
	public static IndexContextContainer getInstance() {
		return SingletonHolder.INSTANCE;
	}
	/**
	 * 加载timer类里scheme对应的IndexControllerContext
	 * @param indexName
	 * @return
	 */
	public static IndexContext loadIndexContext(String indexName) {
		//读取spring.xml。得到索引控制器--在spring.xml里配置的
		IndexContextContainer singleton = (IndexContextContainer) SpringContextLoader.getBean("HXLuci");
		return singleton.getIndexControllerContext(indexName);
	}

	public List<IndexConfig> getIndexConfigs() {
		return this.indexConfigs;
	}

	public void setIndexConfigs(List<IndexConfig> indexConfigs) {
		this.indexConfigs = indexConfigs;
	}
	/**
	 * 构造scheme对应的IndexControllerContext
	 * @param indexName
	 * @return
	 */
	private IndexContext getIndexControllerContext(String indexName) {
		if (indexName == null) {
			throw new IllegalArgumentException("非法参数异常：indexName为null");
		}

		IndexContext context = (IndexContext) this.indexContexts.get(indexName);
		if (context == null) {
			synchronized (this.indexContexts) {
				context = (IndexContext) this.indexContexts.get(indexName);
				if (context == null) {
					//取indexName对应的IndexConfig
					for (IndexConfig config : this.indexConfigs) {
						if (!indexName.equals(config.getIndexName()))
							continue;
						//构造IndexControllerContext
						context = new IndexContext(config);
						//缓存到map
						this.indexContexts.put(indexName, context);
						return context;
					}
				}
			}
		}

		return context;
	}

	/**
	 * 静态内部类实现单利
	 */
	private static class SingletonHolder {
		static IndexContextContainer INSTANCE = new IndexContextContainer();
	}
}
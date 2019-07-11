package com.hcq.sharplucene.core.provider;

import com.hcq.sharplucene.core.client.DocumentDataFormatter;
import com.hcq.sharplucene.core.client.IndexService;
import com.hcq.sharplucene.core.client.QueryResults;
import com.hcq.sharplucene.core.index.IndexContext;
import com.hcq.sharplucene.core.index.IndexContextContainer;
import com.hcq.sharplucene.core.sample.SampleJavaBean;
import com.hcq.sharplucene.core.search.PagedResultSet;
import com.hcq.sharplucene.rpc.anno.RpcAnnotation;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.wltea.analyzer.lucene.IKQueryParser;

import java.util.List;

/**
 * 本地索引服务实现
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@RpcAnnotation(value = IndexService.class)
public class LocalIndexService implements IndexService {
	/**
	 * 索引本地上下文对象
	 */
	private IndexContext indexContext;

	public LocalIndexService() {
	}

	public LocalIndexService(String indexName) {
		this.indexContext = IndexContextContainer.loadIndexContext(indexName);
	}

	@Override
	public String sayHello(String msg) {
		return "lucene"+msg;
	}

	public void build(Object dataBean) {
		Document doc = DocumentDataFormatter.createDocumentfromBean(dataBean,this.indexContext.getIndexConfig());
		this.indexContext.build(doc);
	}

	public void build(List<?> dataBeanList) {
		List<Document> docs = DocumentDataFormatter.createDocumentfromBeans(dataBeanList, this.indexContext.getIndexConfig());
		for (Document doc : docs)
			this.indexContext.build(doc);
	}

	public void backup(Object dataBean) {
		Document doc = DocumentDataFormatter.createDocumentfromBean(dataBean,this.indexContext.getIndexConfig());
		this.indexContext.backup(doc);
	}

	public void backup(List<?> dataBeanList) {
		List<Document> docs = DocumentDataFormatter.createDocumentfromBeans(dataBeanList, this.indexContext.getIndexConfig());
		for (Document doc : docs)
			this.indexContext.backup(doc);
	}
	public void add(SampleJavaBean dataBean) {
		Document doc = DocumentDataFormatter.createDocumentfromBean(dataBean,this.indexContext.getIndexConfig());
		this.indexContext.add(doc);
	}

	public void add(List<?> dataBeanList) {
		List<Document> docs = DocumentDataFormatter.createDocumentfromBeans(dataBeanList, this.indexContext.getIndexConfig());
		for (Document doc : docs)
			this.indexContext.add(doc);
	}

	public void update(Object dataBean) {
		Document doc = DocumentDataFormatter.createDocumentfromBean(dataBean,this.indexContext.getIndexConfig());
		this.indexContext.update(doc);
	}

	public void update(List<?> dataBeanList) {
		List<Document> docs = DocumentDataFormatter.createDocumentfromBeans(dataBeanList, this.indexContext.getIndexConfig());
		for (Document doc : docs)
			this.indexContext.update(doc);
	}

	public void delete(Object dataBean) {
		Document doc = DocumentDataFormatter.createDocumentfromBean(dataBean,this.indexContext.getIndexConfig());
		this.indexContext.delete(doc);
	}

	public void delete(List<?> dataBeanList) {
		List<Document> docs = DocumentDataFormatter.createDocumentfromBeans(dataBeanList, this.indexContext.getIndexConfig());
		for (Document doc : docs)
			this.indexContext.delete(doc);
	}

	public void optimize(boolean immediately) {
		this.indexContext.optimize(immediately);
	}

	public void optimizeBackup(boolean immediately) {
		this.indexContext.optimizeBackupIndex(immediately);
	}
	@Override
	public QueryResults query(String queryString, Integer pageNo, Integer pageSize, Boolean reverse) {
		if (queryString == null) {
			throw new IllegalArgumentException("Parameter 'queryString' is undefined.");
		}
		//queryString转换成lucene的Query对象
		Query query = IKQueryParser.parse(queryString);
		//构造Sort对象
		//初始化默认排序方式
		Sort querySort = new Sort(new SortField(null, SortField.DOC , reverse));
		return query(query, pageNo, pageSize, querySort, false);
	}

	public QueryResults query(String queryString, int pageNo, int pageSize,boolean reverse, String sortFieldName, String sortFieldType) {
		if (queryString == null) {
			throw new IllegalArgumentException("Parameter 'queryString' is undefined.");
		}

		Sort querySort = null;
		if ((sortFieldType != null) && (!"DOC".equals(sortFieldType))) {
			if ("SCORE".equals(sortFieldType)) {
				//使用lucene默认排序
				querySort = new Sort(new SortField(null, SortField.SCORE, reverse));
			} else {
				//使用lucene相关度排序
				if (sortFieldName == null) {
					throw new IllegalArgumentException("Unkown query mode. 'sortFieldName' is null.");
				}
				if ("BYTE".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.BYTE,reverse));
				else if ("SHORT".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.SHORT,reverse));
				else if ("INT".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.INT,reverse));
				else if ("LONG".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.LONG,reverse));
				else if ("FLOAT".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.FLOAT,reverse));
				else if ("DOUBLE".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.DOUBLE,reverse));
				else if ("STRING".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.STRING ,reverse));
				else {
					throw new IllegalArgumentException("Unkown query mode. 'sortType' is Unkown.");
				}
			}
		}
		//queryString转换成lucene的Query对象
		Query query = IKQueryParser.parse(queryString);
		return query(query, pageNo, pageSize, querySort, false);
	}

	public QueryResults queryBackup(String queryString, int pageNo,int pageSize, boolean reverse) {
		if (queryString == null) {
			throw new IllegalArgumentException("Parameter 'queryString' is undefined.");
		}
		//queryString转换成lucene的Query对象
		Query query = IKQueryParser.parse(queryString);

		Sort querySort = new Sort(new SortField(null, SortField.DOC, reverse));
		return query(query, pageNo, pageSize, querySort, true);
	}

	public QueryResults queryBackup(String queryString, int pageNo,int pageSize, boolean reverse, String sortFieldName,
			String sortFieldType) {
		if (queryString == null) {
			throw new IllegalArgumentException("Parameter 'queryString' is undefined.");
		}

		Sort querySort = null;
		if ((sortFieldType != null) && (!"DOC".equals(sortFieldType))) {
			 //使用lucene docid 默认排序
			if ("SCORE".equals(sortFieldType)) {
				//使用Lucene相识度评分排序
				querySort = new Sort(new SortField(null, SortField.SCORE, reverse));
			} else {
				if (sortFieldName == null) {
					throw new IllegalArgumentException("Unkown query mode. 'sortFieldName' is null.");
				}
				if ("BYTE".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.BYTE,reverse));
				else if ("SHORT".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.SHORT,reverse));
				else if ("INT".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.INT,reverse));
				else if ("LONG".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.LONG,reverse));
				else if ("FLOAT".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.FLOAT,reverse));
				else if ("DOUBLE".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.DOUBLE,reverse));
				else if ("STRING".equals(sortFieldType))
					querySort = new Sort(new SortField(sortFieldName, SortField.STRING ,reverse));
				else {
					throw new IllegalArgumentException("Unkown query mode. 'sortType' is Unkown.");
				}
			}
		}
		Query query = IKQueryParser.parse(queryString);
		return query(query, pageNo, pageSize, querySort, true);
	}
	/**
	 * 查询索引，本地接口
	 * @param query
	 * @param pageNo
	 * @param pageSize
	 * @param sort
	 * @param isBackup
	 * @return
	 */
	public QueryResults query(Query query, int pageNo, int pageSize, Sort sort,
			boolean isBackup) {
		PagedResultSet resultSet = this.indexContext.search(query, pageNo,pageSize, sort, isBackup);
		return pack(resultSet);
	}
	/**
	 * 将PagedResultSet 包装成 QueryResults
	 * @param resultSet
	 * @return
	 */
	private QueryResults pack(PagedResultSet resultSet) {
		QueryResults queryResults = new QueryResults();
		if (resultSet != null) {
			queryResults.setPageNo(resultSet.getPageNo());
			queryResults.setPageSize(resultSet.getPageSize());
			queryResults.setResults(resultSet.getResults());
			queryResults.setTotalHit(resultSet.getTotalHit());
		}
		return queryResults;
	}
}
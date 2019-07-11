package com.hcq.sharplucene.core.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * 查询结果对象
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class QueryResults implements Serializable {
	private int totalHit;//查询命中数
	private int pageNo;//当前页码
	private int pageSize = 1;//页面数
	private List<Map<String, String>> results;

	public int getTotalHit() {
		return this.totalHit;
	}

	public void setTotalHit(int totalHit) {
		this.totalHit = totalHit;
	}

	public int getPageNo() {
		return this.pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public List<Map<String, String>> getResults() {
		return this.results;
	}

	public void setResults(List<Map<String, String>> results) {
		this.results = results;
	}

	public <T> List<T> getResultBeans(Class<T> resultType) {
		List resultBeans = new ArrayList();
		if (this.results != null) {
			for (Map rowData : this.results) {
				Object bean = BasicDataFormatter.createBeanFromMap(rowData,
						resultType);
				resultBeans.add(bean);
			}
		}
		return resultBeans;
	}

	public int getTotalPage() {
		int totalPage = this.totalHit / this.pageSize;
		if (this.totalHit % this.pageSize != 0) {
			totalPage++;
		}
		return totalPage;
	}
}
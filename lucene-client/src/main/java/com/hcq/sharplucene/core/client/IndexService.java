package com.hcq.sharplucene.core.client;

import com.hcq.sharplucene.core.sample.SampleJavaBean;

import java.io.IOException;
import java.util.List;

/**
 * 索引服务接口
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public  interface IndexService {
	String sayHello(String msg);
	/**
	 * 创建索引，用于索引的第一次初始化批量增加
	 */
	public abstract void build(Object paramObject) throws IOException;
	/**
	 * 批量创建索引，用于第一次创建
	 * @param paramList
	 * @throws IOException
	 */
	public abstract void build(List<?> paramList) throws IOException;
	/**
	 * 备份（想备份索引库增加索引）
	 * @param paramObject
	 * @throws IOException
	 */
	public abstract void backup(Object paramObject) throws IOException;
	/**
	 * 批量备份（想备份索引库增加索引）
	 * @param paramList
	 * @throws IOException
	 */
	public abstract void backup(List<?> paramList) throws IOException;
	/**
	 * 新增索引
	 * @param paramObject
	 * @throws IOException
	 */
	public abstract void add(SampleJavaBean paramObject) throws IOException;
	/**
	 * 批量新增索引
	 * @param paramList
	 * @throws IOException
	 */
	public abstract void add(List<?> paramList) throws IOException;
	/**
	 * 修改
	 * @param paramObject
	 * @throws IOException
	 */
	public abstract void update(Object paramObject) throws IOException;
	/**
	 * 批量修改
	 * @param paramList
	 * @throws IOException
	 */
	public abstract void update(List<?> paramList) throws IOException;
	/**
	 * 删除
	 * @param paramObject
	 * @throws IOException
	 */
	public abstract void delete(Object paramObject) throws IOException;
	/**
	 * 批量删除
	 * @param paramList
	 * @throws IOException
	 */
	public abstract void delete(List<?> paramList) throws IOException;
	/**
	 * 优化索引
	 * @param paramBoolean 是否立即优化
	 * @throws IOException
	 */
	public abstract void optimize(boolean paramBoolean) throws IOException;
	/**
	 * 优化备份索引（针对备份索引库）
	 * @throws IOException
	 */
	public abstract void optimizeBackup(boolean paramBoolean)throws IOException;
	/**
	 * 查询主索引
	 * @param queryString
	 * @param pageNo
	 * @param pageSize
	 * @return
	 * @throws IOException
	 */
	 QueryResults query(String queryString, Integer pageNo, Integer pageSize, Boolean reverse) throws IOException;
	/**
	 * 
	 * @param queryString
	 * @param pageNo
	 * @param pageSize
	 * @param reverse
	 * @param sortFieldName
	 * @param sortFieldType
	 * @return
	 * @throws IOException
	 */
	public abstract QueryResults query(String queryString, int pageNo, int pageSize, boolean reverse, String sortFieldName, String sortFieldType) throws IOException;
	/**
	 * 查询备份索引库
	 * @param queryString
	 * @param pageNo
	 * @param pageSize
	 * @param reverse
	 * @return
	 * @throws IOException
	 */
	public abstract QueryResults queryBackup(String queryString, int pageNo, int pageSize, boolean reverse) throws IOException;
	/**
	 * 查询备份索引库
	 * @param queryString
	 * @param pageNo
	 * @param pageSize
	 * @param reverse
	 * @param sortFieldName
	 * @param sortFieldType
	 * @return
	 * @throws IOException
	 */
	public abstract QueryResults queryBackup(String queryString, int pageNo, int pageSize,
                                             boolean reverse, String sortFieldName, String sortFieldType) throws IOException;
}
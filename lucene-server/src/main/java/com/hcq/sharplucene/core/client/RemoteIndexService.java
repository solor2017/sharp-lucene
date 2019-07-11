package com.hcq.sharplucene.core.client;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hcq.sharplucene.core.sample.SampleJavaBean;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import com.hcq.sharplucene.util.SimpleHttpClient;
/**
 * 远程索引接口实现
 * @Author: solor
 * @Since: 2.0
 * @Description:http形式的rpc调用
 */
public class RemoteIndexService implements IndexService {
	private URL remoteAddress;//远程服务地址
	private String indexName;//索引名称

	public RemoteIndexService(String indexName, URL remoteAddress) {
		if (indexName == null) {
			throw new IllegalArgumentException("参数 'indexName' 为空.");
		}
		if (remoteAddress == null) {
			throw new IllegalArgumentException("参数 'remoteAddress' 为空.");
		}
		this.remoteAddress = remoteAddress;
		this.indexName = indexName;
	}

	@Override
	public String sayHello(String msg) {
		return null;
	}

	public void build(Object dataBean) throws IOException {
		if (dataBean == null) {
			return;
		}
		List dataBeanList = new ArrayList();
		dataBeanList.add(dataBean);
		build(dataBeanList);
	}

	public void build(List<?> dataBeanList) throws IOException {
		if ((dataBeanList == null) || (dataBeanList.isEmpty())) {
			return;
		}
		String xmlData = XMLDataFormatter.createXMLfromBeans(dataBeanList);
		postIndexCommand("build", xmlData);
	}

	public void backup(Object dataBean) throws IOException {
		if (dataBean == null) {
			return;
		}
		List dataBeanList = new ArrayList();
		dataBeanList.add(dataBean);
		backup(dataBeanList);
	}

	public void backup(List<?> dataBeanList) throws IOException {
		if ((dataBeanList == null) || (dataBeanList.isEmpty())) {
			return;
		}
		String xmlData = XMLDataFormatter.createXMLfromBeans(dataBeanList);
		postIndexCommand("backup", xmlData);
	}

	public void add(SampleJavaBean dataBean) throws IOException {
		if (dataBean == null) {
			return;
		}
		List dataBeanList = new ArrayList();
		dataBeanList.add(dataBean);
		add(dataBeanList);
	}

	public void add(List<?> dataBeanList) throws IOException {
		if ((dataBeanList == null) || (dataBeanList.isEmpty())) {
			return;
		}
		String xmlData = XMLDataFormatter.createXMLfromBeans(dataBeanList);
		//
		postIndexCommand("add", xmlData);
	}

	public void update(Object dataBean) throws IOException {
		if (dataBean == null) {
			return;
		}
		List dataBeanList = new ArrayList();
		dataBeanList.add(dataBean);
		update(dataBeanList);
	}

	public void update(List<?> dataBeanList) throws IOException {
		if ((dataBeanList == null) || (dataBeanList.isEmpty())) {
			return;
		}
		String xmlData = XMLDataFormatter.createXMLfromBeans(dataBeanList);
		try{
			postIndexCommand("update", xmlData);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void delete(Object dataBean) throws IOException {
		if (dataBean == null) {
			return;
		}
		List dataBeanList = new ArrayList();
		dataBeanList.add(dataBean);
		delete(dataBeanList);
	}

	public void delete(List<?> dataBeanList) throws IOException {
		if ((dataBeanList == null) || (dataBeanList.isEmpty())) {
			return;
		}
		String xmlData = XMLDataFormatter.createXMLfromBeans(dataBeanList);
		try{
			postIndexCommand("delete", xmlData);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void optimize(boolean immediately) throws IOException {
		try{
			this.postOptimizeCommand("optimize", immediately);
		}catch(IOException e){
			e.printStackTrace();
		}
		}

	public void optimizeBackup(boolean immediately) throws IOException {
		try{
			postOptimizeCommand("optimizeBackup", immediately);
		}catch(IOException e ){
			e.printStackTrace();
		}
		}

	public QueryResults query(String queryString, Integer pageNo, Integer pageSize, Boolean reverse) throws IOException {
		try{
			String jsonResponse = postQueryCommand("query", queryString, pageNo,
					pageSize, reverse, null, null);
			//JSON输出反序列化
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
			QueryResults queryResult = (QueryResults) mapper.readValue(jsonResponse, QueryResults.class);
			return queryResult;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public QueryResults query(String queryString, int pageNo, int pageSize,boolean reverse, 
								String sortFieldName, String sortFieldType)throws IOException {
		try{
			String jsonResponse = postQueryCommand("query", queryString, pageNo,pageSize, reverse, sortFieldName, sortFieldType);
	
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
			QueryResults queryResult = (QueryResults) mapper.readValue(jsonResponse, QueryResults.class);
			return queryResult;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public QueryResults queryBackup(String queryString, int pageNo,int pageSize, boolean reverse) throws IOException {
		try{
		String jsonResponse = postQueryCommand("queryBackup", queryString,pageNo, pageSize, reverse, null, null);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
		QueryResults queryResult = (QueryResults) mapper.readValue(jsonResponse, QueryResults.class);
		return queryResult;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public QueryResults queryBackup(String queryString, int pageNo,int pageSize, boolean reverse, String sortFieldName,
			String sortFieldType) throws IOException {
		try{
		String jsonResponse = postQueryCommand("queryBackup", queryString,pageNo, pageSize, reverse, sortFieldName, sortFieldType);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
		QueryResults queryResult = (QueryResults) mapper.readValue(jsonResponse, QueryResults.class);
		return queryResult;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 提交索引指令
	 * @param operate
	 * @param xmlData
	 * @return
	 * @throws IOException
	 */
	private String postIndexCommand(String operate, String xmlData)throws IOException {
		Map httpParams = new HashMap();
		httpParams.put("index-name", this.indexName);
		httpParams.put("operate", operate);
		httpParams.put("xml-data", xmlData);
		//post
		byte[] responseData = SimpleHttpClient.post(this.remoteAddress,httpParams, "UTF-8");
		if ((responseData != null) && (responseData.length > 0)) {
			String responseString = new String(responseData, "UTF-8");
			return responseString;
		}
		return "";
	}
	/**|
	 * 提交优化索引操作指令
	 * @param operate
	 * @param immediately
	 * @return
	 * @throws IOException
	 */
	private String postOptimizeCommand(String operate, boolean immediately)throws IOException {
		Map httpParams = new HashMap();
		httpParams.put("index-name", this.indexName);
		httpParams.put("operate", operate);
		httpParams.put("right-now", String.valueOf(immediately));

		byte[] responseData = SimpleHttpClient.post(this.remoteAddress,httpParams, "UTF-8");
		if ((responseData != null) && (responseData.length > 0)) {
			String responseString = new String(responseData, "UTF-8");
			return responseString;
		}
		return "";
	}
	/**
	 * 提交搜索操作指令
	 * @param operate
	 * @param query
	 * @param pageNo
	 * @param pageSize
	 * @param reverse
	 * @param sortFieldName
	 * @param sortFieldType
	 * @return
	 * @throws IOException
	 */
	private String postQueryCommand(String operate, String query, int pageNo,int pageSize, boolean reverse, String sortFieldName,
			String sortFieldType) throws IOException {
		Map httpParams = new HashMap();
		httpParams.put("index-name", this.indexName);
		httpParams.put("operate", operate);
		httpParams.put("query", query);
		httpParams.put("page-no", String.valueOf(pageNo));
		httpParams.put("page-size", String.valueOf(pageSize));
		httpParams.put("sort-reverse", String.valueOf(reverse));
		httpParams.put("sort-by", sortFieldName);
		httpParams.put("sort-type", sortFieldType);

		byte[] responseData = SimpleHttpClient.post(this.remoteAddress,httpParams, "UTF-8");
		if ((responseData != null) && (responseData.length > 0)) {
			String responseString = new String(responseData, "UTF-8");
			return responseString;
		}
		return "";
	}
}

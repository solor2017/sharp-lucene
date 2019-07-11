package com.hcq.sharplucene.rpc.http;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.codehaus.jackson.map.ObjectMapper;
import org.wltea.analyzer.lucene.IKQueryParser;
import com.hcq.sharplucene.core.index.IndexContext;
import com.hcq.sharplucene.core.index.IndexContextContainer;
import com.hcq.sharplucene.core.search.PagedResultSet;
import com.hcq.sharplucene.util.HttpRequestHelper;

/**
 * 组件索引服务RPC HTTP协议服务端Servelt
 * 提供以下索引服务
 * build ： 新建索引 
 * backup ： 备份索引
 * add ： 新增索引
 * update ： 修改索引
 * delete ： 删除索引
 * delete ： 删除索引
 * delete ： 删除索引
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
@WebServlet(urlPatterns = {"/myServlet"})
public class LucIndexServlet extends HttpServlet {
	private static final long serialVersionUID = 8716697712905428514L;

	public void destroy() {
		super.destroy();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		response.setContentType("appliction/json");
		response.setCharacterEncoding("utf-8");
		PrintWriter p = response.getWriter();
		p.write("dafasfaf");
		p.close();
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		String errorMessage = null;

		String indexName = request.getParameter("index-name");

		String operate = request.getParameter("operate");

		if (indexName == null) {
			errorMessage = "HTTP parameter 'indexName' is null.";
			outputError(response, errorMessage);
			return;
		}
		if (operate == null) {
			errorMessage = "HTTP parameter 'operate' is null.";
			outputError(response, errorMessage);
			return;
		}

		//根据indexName加载索引上下文
		IndexContext indexContext = IndexContextContainer.loadIndexContext(indexName);
		if (indexContext == null) {
			errorMessage = "No Found index named '" + indexName + "'";
			outputError(response, errorMessage);
			return;
		}
		//build ： 新建索引
		if ("build".equalsIgnoreCase(operate)) {
			opBuild(request, response, indexContext);
			return;
		}
		//backup ： 备份索引
		if ("backup".equalsIgnoreCase(operate)) {
			opBackup(request, response, indexContext);
			return;
		}
		//add ： 新增索引
		if ("add".equalsIgnoreCase(operate)) {
			opAdd(request, response, indexContext);
			return;
		}
		//update ： 修改索引
		if ("update".equalsIgnoreCase(operate)) {
			opUpdate(request, response, indexContext);
			return;
		}
		//delete ：删除索引
		if ("delete".equalsIgnoreCase(operate)) {
			opDelete(request, response, indexContext);
			return;
		}
		//optimize ：优化索引
		if ("optimize".equalsIgnoreCase(operate)) {
			opOptimize(request, response, indexContext);
			return;
		}
		//optimizeBackup ：优化备份索引
		if ("optimizeBackup".equalsIgnoreCase(operate)) {
			opOptimizeBackup(request, response, indexContext);
			return;
		}
		//query ：查询主索引
		if ("query".equalsIgnoreCase(operate)) {
			opQuery(request, response, indexContext);
			return;
		}
		//queryBackup ：查询备份索引
		if ("queryBackup".equalsIgnoreCase(operate)) {
			opQueryBackup(request, response, indexContext);
			return;
		}
	}

	public void init() throws ServletException {
	}
	/**
	 * build操作
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opBuild(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		String xmlDataString = request.getParameter("xml-data");
		if (xmlDataString != null)
			indexContext.build(xmlDataString);
	}
	/**
	 * backup操作
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opBackup(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		String xmlDataString = request.getParameter("xml-data");
		if (xmlDataString != null)
			indexContext.backup(xmlDataString);
	}
	/**
	 * add操作
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opAdd(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		String xmlDataString = request.getParameter("xml-data");
		if (xmlDataString != null)
			indexContext.add(xmlDataString);
	}
	/**
	 * update操作
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opUpdate(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		String xmlDataString = request.getParameter("xml-data");
		if (xmlDataString != null)
			indexContext.update(xmlDataString);
	}
	/**
	 * delete操作
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opDelete(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		String xmlDataString = request.getParameter("xml-data");
		if (xmlDataString != null)
			indexContext.delete(xmlDataString);
	}
	/**
	 * 优化操作
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opOptimize(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		boolean immediately = HttpRequestHelper.getBooleanParameter(request,"right-now", false);
		indexContext.optimize(immediately);
	}
	/**
	 * 优化备份索引
	 * @param request
	 * @param response
	 * @param indexContext
	 */
	private void opOptimizeBackup(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext) {
		boolean immediately = HttpRequestHelper.getBooleanParameter(request,"right-now", false);
		indexContext.optimizeBackupIndex(immediately);
	}
	/**
	 * 查询操作
	 * @param request
	 * @param response
	 * @param indexContext
	 * @throws IOException
	 */
	private void opQuery(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext)
			throws IOException {
		query(request, response, indexContext, false);
	}
	/**
	 * 查询操作
	 * @param request
	 * @param response
	 * @param indexContext
	 * @throws IOException
	 */
	private void opQueryBackup(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext)
			throws IOException {
		query(request, response, indexContext, true);
	}

	/**
	 * 查询操作
	 * @param request
	 * @param response
	 * @param indexContext
	 * @param inBackupIndex
	 * @throws IOException
	 */
	private void query(HttpServletRequest request,
			HttpServletResponse response, IndexContext indexContext,boolean inBackupIndex) throws IOException {
		PagedResultSet pagedResultSet = new PagedResultSet();
		String queryString = request.getParameter("query");
		if (queryString != null && !"".equals(queryString.trim())) {
			// 页码
			int pageNo = HttpRequestHelper.getIntParameter(request, "page-no",1);
			// 每页大小
			int pageSize = HttpRequestHelper.getIntParameter(request,"page-size", 20);
			// 是否倒序
			boolean reverse = HttpRequestHelper.getBooleanParameter(request,"sort-reverse", true);
			// 初始化默认排序方式
			Sort querySort = new Sort(new SortField(null, SortField.DOC,reverse));
			// 排序类型
			String sortType = request.getParameter("sort-type");
			if (sortType == null || "DOC".equals(sortType)) {
				// 使用lucene docid 默认排序
			} else if ("SCORE".equals(sortType)) {
				// 使用Lucene相识度评分排序
				querySort = new Sort(new SortField(null, SortField.SCORE,reverse));
			} else {
				// 获取指定的排序字段
				String sortFieldName = request.getParameter("sort-by");
				if (sortFieldName == null) {
					String errorMessage = "Unkown query mode. 'sortFieldName' is null.";
					this.outputError(response, errorMessage);
					return;
				}
				if ("BYTE".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.BYTE, reverse));
				} else if ("SHORT".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.SHORT, reverse));
				} else if ("INT".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.INT, reverse));
				} else if ("LONG".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.LONG, reverse));
				} else if ("FLOAT".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.FLOAT, reverse));
				} else if ("DOUBLE".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.DOUBLE, reverse));
				} else if ("STRING".equals(sortType)) {
					querySort = new Sort(new SortField(sortFieldName,SortField.STRING, reverse));
				} else {
					String errorMessage = "Unkown query mode. 'sortType' is Unkown.";
					this.outputError(response, errorMessage);
					return;
				}
			} 
			// 解析query String
			Query query = IKQueryParser.parse(queryString);
			pagedResultSet = indexContext.search(query, pageNo, pageSize,
					querySort, inBackupIndex);
		}
		// 输出JSON结果
		this.outputQueryResults(response, pagedResultSet);
	}
//	private void query(HttpServletRequest request,HttpServletResponse response, IndexContext indexContext,boolean inBackupIndex) throws IOException {
//		PagedResultSet pagedResultSet = new PagedResultSet();
//
//		String queryString = request.getParameter("query");
//		if ((queryString != null) && (!"".equals(queryString.trim()))) {
//			//页码
//			int pageNo = HttpRequestHelper.getIntParameter(request, "page-no",1);
//			//每页大小
//			int pageSize = HttpRequestHelper.getIntParameter(request,"page-size", 20);
//			//是否倒排序sort-reverse
//			boolean reverse = HttpRequestHelper.getBooleanParameter(request,"sort-reverse", true);
//			//初始化默认排序
//			Sort querySort = new Sort(new SortField(null, SortField.SCORE , reverse));
//			//排序方式
//			String sortType = request.getParameter("sort-type");
//
//			if ((sortType != null) && (!"DOC".equals(sortType))) {
//				if ("SCORE".equals(sortType)) {
//					querySort = new Sort(new SortField(null, 0, reverse));
//				} else {
//					String sortFieldName = request.getParameter("sort-by");
//					if (sortFieldName == null) {
//						String errorMessage = "Unkown query mode. 'sortFieldName' is null.";
//						outputError(response, errorMessage);
//						return;
//					}
//					if ("BYTE".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 10,reverse));
//					} else if ("SHORT".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 8,reverse));
//					} else if ("INT".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 4,reverse));
//					} else if ("LONG".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 6,reverse));
//					} else if ("FLOAT".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 5,reverse));
//					} else if ("DOUBLE".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 7,reverse));
//					} else if ("STRING".equals(sortType)) {
//						querySort = new Sort(new SortField(sortFieldName, 3,reverse));
//					} else {
//						String errorMessage = "Unkown query mode. 'sortType' is Unkown.";
//						outputError(response, errorMessage);
//						return;
//					}
//				}
//			}
//			Query query = IKQueryParser.parse(queryString);
//			pagedResultSet = indexContext.search(query, pageNo, pageSize,querySort, inBackupIndex);
//		}
//
//		outputQueryResults(response, pagedResultSet);
//	}

	/**
	 * JSON输出查询结果
	 * @param response
	 * @param pagedResultSet
	 */
	private void outputQueryResults(HttpServletResponse response,PagedResultSet pagedResultSet) throws IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter writer = response.getWriter();
		//JSON转换输出
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(writer, pagedResultSet);
	}

	/**
	 * 输出错误信息
	 * @param response
	 * @param errorMessage
	 * @throws IOException
	 */
	private void outputError(HttpServletResponse response, String errorMessage)throws IOException {
		response.setContentType("text/html;charset=UTF-8");
		response.sendError(500, errorMessage);
	}
}
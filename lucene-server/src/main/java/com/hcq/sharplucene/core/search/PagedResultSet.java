package com.hcq.sharplucene.core.search;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.wltea.analyzer.lucene.IKAnalyzer;


/**
 * 带分页的文档列表结果集
 * @Author: solor
 * @Description:
 */
public class PagedResultSet implements Serializable {
	
	private static final long serialVersionUID = -5534812624903533365L;
	/**
	 * 查询总命中数
	 */
	private int totalHit;
	/**
	 * 当前页码
	 */
	private int pageNo;
	/**
	 * 页面大小
	 */
	private int pageSize = 1;
	/**
	 * 查询结果集
	 */
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

	public void setResults(List<Map<String, String>> results) {
		this.results = results;
	}

	public List<Map<String, String>> getResults() {
		return this.results;
	}

	public void setResultDocument(Query query, Document[] docs)
			throws IOException {
		this.results = new ArrayList();
		if (docs != null)
			for (Document doc : docs)
				this.results.add(documentToMap(query, doc));
	}
	/**
	 * 计算总页数
	 * @return
	 */
	public int getTotalPage() {
		int totalPage = this.totalHit / this.pageSize;
		if (this.totalHit % this.pageSize != 0) {
			totalPage++;
		}
		return totalPage;
	}
	/**
	 * 将Document转换成Map
	 * @param query
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	private Map<String, String> documentToMap(Query query, Document doc)
			throws IOException {
		Map result = new HashMap();
		if (doc != null) {
			List<Fieldable> fields = doc.getFields();
			for (Fieldable f : fields) {
				// if(!f.name().equals("url")){
				// result.put(f.name(), f.stringValue());
				// }else{
				result.put(f.name(), getHighlighter(query, f.name(), f
						.stringValue()));// 高亮部分
			}
		}
		return result;
	}

	// 高亮关键字
	private String getHighlighter(Query query, String term, String kw)
			throws IOException {
		try {
			SimpleHTMLFormatter simpleHtmlFormatter = new SimpleHTMLFormatter(
					"<B><font color=\"red\">", "</font></B>");
			Highlighter highlighter = new Highlighter(simpleHtmlFormatter,
					new QueryScorer(query));
			highlighter.setTextFragmenter(new SimpleFragmenter(100));
			String res = "";
			Analyzer luceneAnalyzer = new IKAnalyzer();
			TokenStream ts = luceneAnalyzer.tokenStream(term, new StringReader(
					kw));
			// String ss = f.stringValue();
			String kww = highlighter.getBestFragment(ts, kw);
			// TermAttribute tm = ts.addAttribute(TermAttribute.class);
			// while(ts.incrementToken()){
			// res = res+tm.term()+" ";
			// }
			return kww == null ? kw : kww;
		} catch (Exception e) {
		}
		return null;
	}
}
package com.hcq.sharplucene.core.sample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import com.hcq.sharplucene.core.client.IndexService;
import com.hcq.sharplucene.core.client.IndexServiceFactory;
import com.hcq.sharplucene.core.client.QueryResults;

/**http形式的rpc调用
 * @Author: solor
 * @Description:
 */
public class SharpLucenRemoteSample {
	public static void main(String[] args) {
		URL remoteHttpURL = null;
		try {
			remoteHttpURL = new URL("http://localhost:8080/myServlet");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		//获取远程实例
		IndexService indexService = IndexServiceFactory.getRemoteIndexService("SAMPLE", remoteHttpURL);

		for (int i = 0; i < 20; i++) {
			SampleJavaBean bean = new SampleJavaBean();
			bean.setCheckFlag(true);
			bean.setRegistTime(new Date());
			bean.setUrl("http://sample.lucimint.org");
			bean.setUserName("LuciMint" + i);
			bean.setCommentId(20000 + i);
			try {
				indexService.add(bean);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(3000L);
			System.out.println("*****************************");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String queryString = "url='http://sample.lucimint.org'";
		try {
			QueryResults queryResults = indexService.query(queryString, 1, 20,
					true);
			System.out.println("PageNo :" + queryResults.getPageNo());
			System.out.println("PageSize :" + queryResults.getPageSize());
			System.out.println("TotalHit :" + queryResults.getTotalHit());
			System.out.println("TotalPage :" + queryResults.getTotalPage());

			List<SampleJavaBean> beanList = queryResults
					.getResultBeans(SampleJavaBean.class);

			for (SampleJavaBean bean : beanList)
				System.out.println(bean.getCommentId() + " | "
						+ bean.getUserName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
package com.hcq.sharplucene.core.sample;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import com.hcq.sharplucene.core.client.IndexService;
import com.hcq.sharplucene.core.client.IndexServiceFactory;
import com.hcq.sharplucene.core.client.QueryResults;

/**
 * @Author: solor
 * @Description:
 */
public class SharpLuceneLocalSample {
  public static void main(String[] args)
  {
    IndexService indexService = IndexServiceFactory.getLocalIndexService("COMMENT");

//    for (int i = 0; i < 100; i++){
//      SampleJavaBean bean = new SampleJavaBean();
//      bean.setCheckFlag(true);
//      bean.setRegistTime(new Date());
//      bean.setUrl("www.baidu.com");
//      bean.setUserName("胡长青");
//      bean.setCommentId(20000 + i);
//      try {
//        indexService.add(bean);
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
//    try
//    {
//      Thread.sleep(10L);
//      System.out.println("*****************************");
//    }
//    catch (InterruptedException e) {
//      e.printStackTrace();
//    }

    String queryString = "userName='胡长青'";
    try
    {
      QueryResults queryResults = indexService.query(queryString, 1, 500, true);
      System.out.println("PageNo :" + queryResults.getPageNo());
      System.out.println("PageSize :" + queryResults.getPageSize());
      System.out.println("TotalHit :" + queryResults.getTotalHit());
      System.out.println("TotalPage :" + queryResults.getTotalPage());

      List <SampleJavaBean>beanList = queryResults.getResultBeans(SampleJavaBean.class);

      for (SampleJavaBean bean : beanList)
        System.out.println(bean.getCommentId() + " | " + bean.getUserName());
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
}
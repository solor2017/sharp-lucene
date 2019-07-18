package com.hcq.sharplucene;

import com.hcq.sharplucene.core.provider.LocalIndexService;
import com.hcq.sharplucene.core.sample.SampleJavaBean;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LuceneServerApplicationTests {

    //com.hcq.sharplucene.core.provider.LocalIndexService

    @Test
    public void contextLoads() {
        LocalIndexService localIndexService;
        localIndexService =Reflect.on(LocalIndexService.class).create("COMMENT").get();

        SampleJavaBean bean = new SampleJavaBean();
      bean.setCheckFlag(true);
      bean.setRegistTime(new Date());
      bean.setUrl("www.baidu.com");
      bean.setUserName("胡长青");
      bean.setCommentId(20000 );
        localIndexService.add(bean);
    }

}

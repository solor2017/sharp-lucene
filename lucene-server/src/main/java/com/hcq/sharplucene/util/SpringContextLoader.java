package com.hcq.sharplucene.util;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 监听器，需在web.xml里配置
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public class SpringContextLoader implements InitializingBean {
	//配置文件路径
	private static final String CONFIG_FILE_LOCATION = "spring.xml";
	//文件上下文
	private static ApplicationContext context;

//	public void contextInitialized(ServletContextEvent event) {
//		if (context == null)
//			synchronized (SpringContextLoader.class) {
//				if (context == null) {
//					super.contextInitialized(event);
//					ServletContext servletCtx = event.getServletContext();
//					//获取web环境下的ApplicationContext
//					context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletCtx);
//				}
//			}
//	}

	/**
	 * 获取spring的ApplicationContext
	 * @return
	 */
	public static ApplicationContext getSpringContext() {
		if (context == null) {
			synchronized (SpringContextLoader.class) {
				if (context == null) {
					context = new ClassPathXmlApplicationContext("spring.xml");
					((AbstractApplicationContext) context).registerShutdownHook();
				}
			}
		}
		return context;
	}

	/**
	 * 获取配置文件的bean
	 * @param beanName
	 * @return
	 */
	public static Object getBean(String beanName) {
		return getSpringContext().getBean(beanName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		getSpringContext();
	}
}
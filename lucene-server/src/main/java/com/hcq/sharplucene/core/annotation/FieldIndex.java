package com.hcq.sharplucene.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 索引自定义注解
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { java.lang.annotation.ElementType.FIELD })
public @interface FieldIndex {
	/**
	 * 不索引
	 */
	public static final String NO_INDEX = "NO";
	/**
	 * 索引不分词
	 */
	public static final String NOT_ANALYZED = "NOT_ANALYZED";
	/**
	 * 索引、分词
	 */
	public static final String ANALYZED = "ANALYZED";
	/**
	 * 取得自定义注解属性值
	 * @return
	 */
	public abstract String value();
}
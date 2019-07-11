package com.hcq.sharplucene.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { java.lang.annotation.ElementType.FIELD })
public @interface FieldStore {
	/**
	 * 不存储
	 */
	public static final String NO = "NO";
	/**
	 * 存储
	 */
	public static final String YES = "YES";

	public abstract String value();
}

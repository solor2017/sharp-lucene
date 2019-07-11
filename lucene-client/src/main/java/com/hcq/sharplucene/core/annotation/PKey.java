package com.hcq.sharplucene.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，对应javabean的主键标志
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { java.lang.annotation.ElementType.FIELD })
public @interface PKey {
}

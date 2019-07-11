package com.hcq.sharplucene.core.client;

import com.hcq.sharplucene.core.annotation.FieldIndex;
import com.hcq.sharplucene.core.annotation.FieldStore;
import com.hcq.sharplucene.core.annotation.PKey;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 *  索引数据格式转换
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class BasicDataFormatter {
	/**
	 * 读取dataBean指定属性的值
	 * @param beanField
	 * @param dataBean
	 * @return
	 */
	static String readDocFieldValue(Field beanField, Object dataBean) {
		//取属性的实际值（对象）
		Object fieldValue = null;
		try {
			fieldValue = beanField.get(dataBean);
			if (fieldValue == null)
				return null;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		if ((fieldValue instanceof Date)) {
			//如果是date型， 转8位日期格式
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			return sdf.format((Date) fieldValue);
		}
		return fieldValue.toString();
	}
	/**
	 * 读取Bean的指定属性的FieldStore注释
	 * @param beanField
	 * @return
	 */
	static String readDocFieldStore(Field beanField) {
		FieldStore storeAnno = (FieldStore) beanField.getAnnotation(FieldStore.class);
		if (storeAnno == null) {
			return "NO";
		}
		return storeAnno.value();
	}
	/**
	 * 读取Bean的指定属性的FieldIndex注释
	 * @param beanField
	 * @return
	 */
	static String readDocFieldIndex(Field beanField) {
		FieldIndex indexAnno = (FieldIndex) beanField.getAnnotation(FieldIndex.class);
		if (indexAnno == null) {
			return "NO";
		}
		return indexAnno.value();
	}
	/**
	 * 使用Map属性值生成Bean
	 * @param <T>
	 * @param dataMap
	 * @param beanType
	 * @return
	 */
	public static <T> T createBeanFromMap(Map<String, String> dataMap,Class<T> beanType) {
		Object dataBean = null;
		try {
			//生成Bean实例
			dataBean = beanType.newInstance();
			Field[] beanFields = beanType.getDeclaredFields();

			for (Field beanField : beanFields) {
				beanField.setAccessible(true);
				//忽略serialVersionUID属性
				if (beanField.getName().equals("serialVersionUID")) {
					continue;
				}
				//忽略没有索引相关注释的属性
				if ((beanField.getAnnotation(PKey.class) == null)
					&& (beanField.getAnnotation(FieldStore.class) == null)
					&& (beanField.getAnnotation(FieldIndex.class) == null)) {
					continue;
				}
				//忽略不存储的属性
				FieldStore fsAnno = (FieldStore) beanField.getAnnotation(FieldStore.class);
				if (((fsAnno == null) || ("NO".equals(fsAnno.value())))&& (beanField.getAnnotation(PKey.class) == null)) {
					continue;
				}
				//给dataBean的属性赋值
				String fieldValue = (String) dataMap.get(beanField.getName());
				
				if (fieldValue != null) {
					Class fieldType = beanField.getType();
					try {
						if (String.class.equals(fieldType)) {
							beanField.set(dataBean, fieldValue);
						} else if (Byte.TYPE.equals(fieldType)) {
							beanField.setByte(dataBean, Byte.parseByte(fieldValue));
						} else if (Byte.class.equals(fieldType)) {
							beanField.set(dataBean, Byte.valueOf(fieldValue));
						} else if (Boolean.TYPE.equals(fieldType)) {
							beanField.setBoolean(dataBean, Boolean.parseBoolean(fieldValue));
						} else if (Boolean.class.equals(fieldType)) {
							beanField.set(dataBean, Boolean.valueOf(fieldValue));
						} else if (Short.TYPE.equals(fieldType)) {
							beanField.setShort(dataBean, Short.parseShort(fieldValue));
						} else if (Short.class.equals(fieldType)) {
							beanField.set(dataBean, Short.valueOf(fieldValue));
						} else if (Integer.TYPE.equals(fieldType)) {
							beanField.setInt(dataBean, Integer.parseInt(fieldValue));
						} else if (Integer.class.equals(fieldType)) {
							beanField.set(dataBean, Integer.valueOf(fieldValue));
						} else if (Long.TYPE.equals(fieldType)) {
							beanField.setLong(dataBean, Long.parseLong(fieldValue));
						} else if (Long.class.equals(fieldType)) {
							beanField.set(dataBean, Long.valueOf(fieldValue));
						} else if (Float.TYPE.equals(fieldType)) {
							beanField.setFloat(dataBean, Float.parseFloat(fieldValue));
						} else if (Float.class.equals(fieldType)) {
							beanField.set(dataBean, Float.valueOf(fieldValue));
						} else if (Double.TYPE.equals(fieldType)) {
							beanField.setDouble(dataBean, Double.parseDouble(fieldValue));
						} else if (Double.class.equals(fieldType)) {
							beanField.set(dataBean, Double.valueOf(fieldValue));
						} else if (Date.class.equals(fieldType)) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
							beanField.set(dataBean, sdf.parse(fieldValue));
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return (T) dataBean;
	}
}

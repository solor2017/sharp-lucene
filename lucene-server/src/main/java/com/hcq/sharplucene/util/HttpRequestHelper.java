package com.hcq.sharplucene.util;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
/**
 * Http request参数读取处理
 * 实现各种常用数据类型的参数转换及空值处理
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public class HttpRequestHelper {
	private static String NULLSTRING = "";
	public static final String DEFAULT_ENCODING = "ISO-8859-1";
	public static final String TARGET_ENCODING = "UTF-8";

	/**
	 * 对http接受的参数进行转码转换
	 * @param request
	 * @param name
	 * @param encoding
	 * @param defautlValue
	 * @return
	 */
	public static String getEncodedParameter(HttpServletRequest request,String name, String encoding, String defautlValue) {
		String temp = request.getParameter(name);
		if ((temp == null) || (temp.trim().equals(NULLSTRING))) {
			return defautlValue;
		}
		if (encoding == null)
			return temp;
		try {
			temp = new String(temp.getBytes(DEFAULT_ENCODING), encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return defautlValue;
		}
		return temp;
	}

	/**
	 * 获取http参数，为空时==null
	 * @param request
	 * @param name
	 * @param encoding
	 * @return
	 */
	public static String getEncodedParameter(HttpServletRequest request,String name, String encoding) {
		return getEncodedParameter(request, name, encoding, null);
	}

	/**
	 * 取得HTTP参数，值为空字符串或null时返回默认值
	 * @param request
	 * @param name
	 * @return
	 */
	public static String getParameter(HttpServletRequest request, String name) {
		return getEncodedParameter(request, name, TARGET_ENCODING, null);
	}
	/**
	 * 取得HTTP参数，值为空字符串或null时返回默认值
	 * @param request
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getParameter(HttpServletRequest request, String name,String defaultValue) {
		return getEncodedParameter(request, name, TARGET_ENCODING, defaultValue);
	}
	/**
	 * 对HTTP接收的参数数组进行编码转换
	 * @param request
	 * @param name
	 * @param encoding
	 * @return
	 */
	public static String[] getEncodedParameters(HttpServletRequest request,String name, String encoding) {
		String[] temp = request.getParameterValues(name);
		if (temp == null) {
			return null;
		}
		if (encoding == null)
			return temp;
		try {
			for (int i = 0; i < temp.length; i++)
				if (temp[i] != null)
					temp[i] = new String(temp[i].getBytes(DEFAULT_ENCODING),encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return temp;
	}
	/**
	 * 对HTTP接收的参数数组进行编码转换
	 * @param request
	 * @param name
	 * @return
	 */
	public static String[] getParameters(HttpServletRequest request, String name) {
		return getEncodedParameters(request, name, TARGET_ENCODING);
	}
	/**
	 * 把取得的参数传化为boolean类型
	 * 值为"trur"或'y"时返回true，否则返回false
	 * @param request
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	public static boolean getBooleanParameter(HttpServletRequest request,String name, boolean defaultVal) {
		String temp = request.getParameter(name);
		if (("true".equalsIgnoreCase(temp)) || ("y".equalsIgnoreCase(temp)))
			return true;
		if (("false".equalsIgnoreCase(temp)) || ("n".equalsIgnoreCase(temp))) {
			return false;
		}
		return defaultVal;
	}
	/**
	 * 把取得的参数传化为int类型
	 * @param request
	 * @param name
	 * @param defaultNum
	 * @return
	 */
	public static int getIntParameter(HttpServletRequest request, String name,int defaultNum) {
		String temp = request.getParameter(name);
		if ((temp == null) || (temp.trim().equals(NULLSTRING)))
			return defaultNum;
		try {
			defaultNum = Integer.parseInt(temp);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return defaultNum;
	}
	/**
	 * 把取得的参数传化为float类型
	 * @param request
	 * @param name
	 * @param defaultNum
	 * @return
	 */
	public static float getFloatParameter(HttpServletRequest request,String name, float defaultNum) {
		String temp = request.getParameter(name);
		if ((temp == null) || (temp.trim().equals(NULLSTRING)))
			return defaultNum;
		try {
			defaultNum = Float.parseFloat(temp);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return defaultNum;
	}
	/**
	 * 把取得的参数传化为double类型
	 * @param request
	 * @param name
	 * @param defaultNum
	 * @return
	 */
	public static double getDoubleParameter(HttpServletRequest request,String name, double defaultNum) {
		String temp = request.getParameter(name);
		if ((temp == null) || (temp.trim().equals(NULLSTRING)))
			return defaultNum;
		try {
			defaultNum = Double.parseDouble(temp);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return defaultNum;
	}
	/**
	 * 把取得的参数传化为long类型
	 * @param request
	 * @param name
	 * @param defaultNum
	 * @return
	 */
	public static long getLongParameter(HttpServletRequest request,String name, long defaultNum) {
		String temp = request.getParameter(name);
		if ((temp == null) || (temp.trim().equals(NULLSTRING)))
			return defaultNum;
		try {
			defaultNum = Long.parseLong(temp);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return defaultNum;
	}
}

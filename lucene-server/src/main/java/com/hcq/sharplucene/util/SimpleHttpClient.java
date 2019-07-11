package com.hcq.sharplucene.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
/**
 * 模拟http请求客户端
 * 非java客户端传送数据，按json格式封装，即httpParamters
 * @Author: solor
 * @Since: 1.0
 * @Description: 简单http远程调用
 */
public class SimpleHttpClient {
	/**
	 * Http post请求处理
	 * @param httpURL
	 * @param httpParamters
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static byte[] post(URL httpURL, Map<String, String> httpParamters,String encoding) throws IOException {
		if (httpURL == null) {
			throw new IllegalArgumentException("Parameter 'httpURL' is undefined.");
		}
		if (encoding == null) {
			encoding = "UTF-8";
		}
		//根据HTTP参数，构造HTTP POST的文本
		StringBuffer postContents = new StringBuffer();
		if (httpParamters != null) {
			Set<Entry<String, String>> entries = httpParamters.entrySet();
			for (Entry entry : entries) {
				try {
					if (entry.getValue() != null)
						postContents.append((String) entry.getKey())
						.append("=")
						.append(URLEncoder.encode((String) entry.getValue(), encoding))
						.append("&"
								);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			//删除尾部的&号
			postContents.deleteCharAt(postContents.length() - 1);
		}
		//开始Http请求处理
		HttpURLConnection urlconn = null;
		OutputStream os = null;
		InputStream in = null;
		try {
			urlconn = (HttpURLConnection) httpURL.openConnection();
			//初始化连接
			urlconn.setConnectTimeout(3000);

			urlconn.setReadTimeout(1800000);
			urlconn.setRequestMethod("POST");
			urlconn.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=" + encoding);
			urlconn.setDoInput(true);//  下载HTTP资源，需要将setDoInput方法的参数值设为true  
			urlconn.setDoOutput(true);//  上传数据，需要将setDoOutput方法的参数值设为true  
			//发起请求连接
			urlconn.connect();//实际是建立一个tcp连接，此时并未真正发送数据
			os = urlconn.getOutputStream();
			os.write(postContents.toString().getBytes());
			os.flush();

//			if (200 == urlconn.getResponseCode()) {
			if (HttpURLConnection.HTTP_OK == urlconn.getResponseCode()) {
				in = urlconn.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int count = 0;
				while ((count = in.read(buffer)) > 0) {
					baos.write(buffer, 0, count);
				}
				byte[] arrayOfByte1 = baos.toByteArray();//网页的二进制数据
				return arrayOfByte1;
			}
			String error = "HTTP " + urlconn.getResponseCode() + "  error : "+ urlconn.getResponseMessage();
			throw new IOException(error);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} finally {
			if (urlconn != null) {
				urlconn.disconnect();
			}

			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}

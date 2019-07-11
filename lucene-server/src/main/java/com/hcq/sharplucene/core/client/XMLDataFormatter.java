package com.hcq.sharplucene.core.client;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.hcq.sharplucene.core.annotation.FieldIndex;
import com.hcq.sharplucene.core.annotation.FieldStore;
import com.hcq.sharplucene.core.annotation.PKey;
/**
 * 索引数据格式转换（针对查询）
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class XMLDataFormatter extends BasicDataFormatter {
	/**
	 *从javabean列表生成XML String
	 *XML DEMO :
	 *           <index-data>
	 *        		 <document> 
	 *        			 <field name="uuid"  pkey="true"> 
	 *           			  10000
	 *           		 </field> 
	 *            		<field name="userName"  store="true" index="ANALYZED">
	 *             				用户   
	 *             		</field> 
	 *             		<field name="url" store="true">  
	 *              			<![CDATA[http://www.sohu.com]]> 
	 *              	</field> 
	 *              	<field name="registTime" store="true" index="NO_ANALYZED" > 
	 *              			10000   
	 *              	</field>   
	 *             </document> 
	 *           </index-data>
	 *	@param dataBeans  
	 * 	@return 
	 **/ 
	public static String createXMLfromBeans(List<?> dataBeans) {
		Elemet_IndexData eleIndexData = new Elemet_IndexData();
		if (dataBeans != null) {
			for (Iterator localIterator = dataBeans.iterator(); localIterator.hasNext();) {
				Object dataBean = localIterator.next();
				Element_Document document = beanToXML(dataBean);
				eleIndexData.addDocument(document);
			}
		}
		return eleIndexData.toXML();
	}
	/**
	 * 解析单个bean
	 * @param dataBean
	 * @return
	 */
	private static Element_Document beanToXML(Object dataBean) {
		Element_Document eleDoc = new Element_Document();
		 //获取data bean的类对象
		Class dataBeanClass = dataBean.getClass();
		//获取当前Bean中声明的所有属性（不包括继承的类）
		Field[] fields = dataBeanClass.getDeclaredFields();
		//将Bean的属性转成lucene document的Field
		boolean foundPkey = false;
		for (Field beanField : fields) {
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
			String docFieldValue = readDocFieldValue(beanField, dataBean);
			 //对非空值属性，添加到索引中,忽略null值属性
			if (docFieldValue != null) {
				Element_Field eleField = new Element_Field();
				eleField.name = beanField.getName();
				eleField.value = regularizeXmlString(docFieldValue);
				//处理PKey属性
				PKey pKeyAnno = (PKey) beanField.getAnnotation(PKey.class);
				if ((!foundPkey) && (pKeyAnno != null)) {
					foundPkey = true;
					eleField.pkey = true;
				} else {
					eleField.store = readDocFieldStore(beanField);
					eleField.index = readDocFieldIndex(beanField);
				}

				eleDoc.addField(eleField);
			}

		}
		//在配置文件中没有找到主键，跑异常
		if (!foundPkey) {
			throw new IllegalArgumentException("数据对象PKey属性校验失败，名称为空或不匹配!");
		}
		return eleDoc;
	}
	/**
	 * 过滤xml中的非法字符窜
	 * @param strInput
	 * @return
	 */
	public static String regularizeXmlString(String strInput) {
		String emptyString = "";
		if ((strInput == null) || (strInput.length() == 0)) {
			return emptyString;
		}
		String result = strInput.replaceAll("[\\x00-\\x08|\\x0b-\\x0c|\\x0e-\\x1f]", emptyString);

		result = escapeCDATA(result);

		result = escapeHTMLTag(result);

		result = escapeNBSP(result);
		return result;
	}

	public static String escapeCDATA(String strInput) {
		String emptyString = "";
		if ((strInput == null) || (strInput.length() == 0)) {
			return emptyString;
		}
		String result = Pattern.compile("<!\\[CDATA\\[.*?\\]\\]>", 32).matcher(strInput).replaceAll(emptyString);
		return result;
	}
	/**
	 * 过滤html标签
	 * @param strInput
	 * @return
	 */
	public static String escapeHTMLTag(String strInput) {
		String emptyString = "";
		if ((strInput == null) || (strInput.length() == 0)) {
			return emptyString;
		}
		String result = strInput.replaceAll("<[^>]*>", emptyString);
		return result;
	}

	public static String escapeNBSP(String strInput) {
		String emptyString = "";
		if ((strInput == null) || (strInput.length() == 0)) {
			return emptyString;
		}
		String result = strInput.replaceAll("&[a-z0-9#]+;", emptyString);
		return result;
	}

	public static void main(String[] args) {
		String testStr = "<field name=\"url\" store=\"true\"><![CDATA[http://www.>sohu]sdfa.]]>com]]></field>";
		System.out.println(regularizeXmlString(testStr));
	}
	/**
	 * XML <index-data> 元素
	 * @author huchangqing
	 *
	 */
	static class Element_Document {
		private List<Element_Field> fields;

		public Element_Document() {
			this.fields = new ArrayList();
		}

		public void addField(Element_Field field) {
			this.fields.add(field);
		}

		public String toXML() {
			StringBuffer sb = new StringBuffer();
			sb.append("\t").append("<document>").append("\r\n");
			for (Element_Field field : this.fields) {
				sb.append("\t").append(field.toXML());
			}
			sb.append("\t").append("</document>").append("\r\n");
			return sb.toString();
		}
	}
	/**
	 * XML <index-data> 元素
	 * @author huchangqing
	 *
	 */
	static class Element_Field {
		private String name;
		private String value;
		private boolean pkey;
		private String store;
		private String index;

		public String toXML() {
			StringBuffer sb = new StringBuffer();

			sb.append("\t").append("<field ").append("name=\"").append(this.name).append("\" ");

			if (this.pkey) {
				sb.append("pkey=\"").append(this.pkey).append("\" ");
			}

			if ("YES".equals(this.store)) {
				sb.append("store=\"").append(true).append("\" ");
			}

			if ("NOT_ANALYZED".equals(this.index))
				sb.append("index=\"").append("NOT_ANALYZED").append("\" ");
			else if ("ANALYZED".equals(this.index)) {
				sb.append("index=\"").append("ANALYZED").append("\" ");
			}

			sb.append(" >").append("<![CDATA[").append(this.value).append("]]>").append("</field>").append("\r\n");

			return sb.toString();
		}
	}
	/**
	 * XML <index-data> 元素
	 * @author huchangqing
	 *
	 */
	static class Elemet_IndexData {
		private List<Element_Document> documents;

		public Elemet_IndexData() {
			this.documents = new ArrayList();
		}

		public void addDocument(Element_Document document) {
			this.documents.add(document);
		}

		public String toXML() {
			StringBuffer sb = new StringBuffer();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\r\n");
			sb.append("<index-data>").append("\r\n");
			for (Element_Document document : this.documents) {
				sb.append(document.toXML());
			}
			sb.append("</index-data>").append("\r\n");
			return sb.toString();
		}
	}
}
package com.hcq.sharplucene.core.index;

import com.sun.xml.internal.stream.XMLInputFactoryImpl;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

/**
 * 索引数据适配器，转换索引数据格式
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public class IndexDataAdapter {
	//field常量，用来代步某个可以被忽略的field
	private static Field IGNORE_FIELD = new Field("Luci.IGNORE_FIELD", "", Store.YES, Index.NO);
	private XMLInputFactory xmlInputFactory;//xml解析器
	private IndexConfig indexConfig;//索引配置实例

	public IndexDataAdapter(IndexConfig indexConfig) {
		this.indexConfig = indexConfig;
		this.xmlInputFactory = new XMLInputFactoryImpl();
	}
	/**
	 * xmlDataString-->document
	 * @param xmlDataString
	 * @return
	 */
	public List<Document> xmlToDocument(String xmlDataString) {
		if (xmlDataString == null) {
			return new ArrayList(0);
		}
		StringReader xmlReader = new StringReader(xmlDataString);
		return xmlToDocument(xmlReader);
	}
	/**          
	* 从Reader接口读取XML，并生成Document           
	*  XML DEMO :          
	* <index-data>           
	*              <document>          
	*                      <field name="uuid"  pkey="true">          
	*                              10000          
	*                      </field>          
	*              <field name="userName"  store="true" index="ANALYZED">          
	*                              hcq          
	*                      </field>          
	*                      <field name="url" store="true">          
	*                              <![CDATA[http://www.baidu.com]]>          
	*              </field>          
	*              <field name="registTime" store="true" index="NO_ANALYZED" >          
	*                      10000          
	*              </field>          
	*              </document>          
	* </index-data>           
	* @param xmlReader          
	* @return          
	*/ 
//	public List<Document> xmlToDocument(Reader xmlReader) {
//		List docs = new ArrayList();
//		String elementName = null;
//		Field docField = null;
//		XMLStreamReader streamReader = null;
//		try {
//			boolean foundPkey = false;
//			streamReader = this.xmlInputFactory.createXMLStreamReader(xmlReader);
//			int event = 0;
//			Stack xmlElements = new Stack();
//
//			while (streamReader.hasNext()) {
//				event = streamReader.next();
//				switch (event) {
//				case XMLStreamConstants.START_DOCUMENT://文档开始
//					break;
//				case XMLStreamConstants.START_ELEMENT:
//					 //xml标签开始，记录当前
//					elementName = streamReader.getName().toString();
//					if ("index-data".equals(elementName))
//						continue;
//					if ("document".equals(elementName)) {
//						Document doc = new Document();
//						xmlElements.push(doc);
//						foundPkey = false;
//					} else {
//						if (!"field".equals(elementName))
//							continue;
//						String name = null;
//						boolean pkey = false;
//						boolean store = false;
//						String index = null;
//						int attrCount = streamReader.getAttributeCount();
//						for (int i = 0; i < attrCount; i++) {
//							String attrName = streamReader.getAttributeName(i).toString();
//							String attrValue = streamReader.getAttributeValue(i);
//
//							if ("name".equals(attrName)) {
//								name = attrValue;
//							} else if ((!foundPkey)&& ("pkey".equals(attrName))) {
//								pkey = Boolean.parseBoolean(attrValue);
//							} else if ("store".equals(attrName)) {
//								store = Boolean.parseBoolean(attrValue);
//							} else if ("index".equals(attrName)) {
//								index = attrValue;
//							}
//
//						}
//
//						String docFieldName = name;
//						Field.Store docFieldStore = null;
//						Field.Index docFieldIndex = null;
//						if (pkey) {
//							foundPkey = true;
//
//							if ((docFieldName == null)
//									|| (this.indexConfig.getKeyFieldName() == null)
//									|| (!docFieldName.equals(this.indexConfig.getKeyFieldName()))) {
//								throw new IllegalArgumentException(
//										"数据对象PKey属性校验失败，名称为空或不匹配!");
//							}
//
//							docFieldStore = Field.Store.YES;
//
//							docFieldIndex = Field.Index.NOT_ANALYZED_NO_NORMS;
//						} else {
//							if (store)
//								docFieldStore = Field.Store.YES;
//							else {
//								docFieldStore = Field.Store.NO;
//							}
//							if (index != null) {
//								if ("ANALYZED".equals(index))
//									docFieldIndex = Field.Index.ANALYZED_NO_NORMS;
//								else
//									docFieldIndex = Field.Index.NOT_ANALYZED_NO_NORMS;
//							} else {
//								docFieldIndex = Field.Index.NO;
//							}
//
//						}
//
//						// Field docField = null;
//						if ((docFieldStore == Field.Store.NO)
//								&& (docFieldIndex == Field.Index.NO))
//							docField = IGNORE_FIELD;
//						else {
//							docField = new Field(docFieldName, "",docFieldStore, docFieldIndex);
//						}
//
//						xmlElements.push(docField);
//					}
//					break;
//				case 4:
//					if (streamReader.isWhiteSpace()) {
//						continue;
//					}
//					docField = (Field) xmlElements.peek();
//					if (IGNORE_FIELD == docField)
//						continue;
//					String docFieldValue = streamReader.getText();
//					docField.setValue(docFieldValue);
//
//					break;
//				case 2:
//					elementName = streamReader.getName().toString();
//					if ("field".equals(elementName)) {
//						docField = (Field) xmlElements.pop();
//						if (IGNORE_FIELD == docField)
//							continue;
//						Document doc = (Document) xmlElements.peek();
//
//						doc.add(docField);
//					} else if ("document".equals(elementName)) {
//						if (!foundPkey) {
//							throw new IllegalArgumentException("数据对象缺少PKey属性!");
//						}
//
//						Document doc = (Document) xmlElements.pop();
//
//						docs.add(doc);
//					} else {
//						"index-data".equals(elementName);
//					}
//				case 3:
//				case 5:
//				case 6:
//				case 8:
//				}
//			}
//		} catch (XMLStreamException e) {
//			e.printStackTrace();
//
//			if (streamReader != null)
//				try {
//					streamReader.close();
//				} catch (XMLStreamException e1) {
//					e1.printStackTrace();
//				}
//		} finally {
//			if (streamReader != null) {
//				try {
//					streamReader.close();
//				} catch (XMLStreamException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return docs;
//	}
	public List<Document> xmlToDocument(Reader xmlReader) {
		List<Document> docs = new ArrayList<Document>();
		XMLStreamReader streamReader = null;
		try {
			boolean foundPkey = false;
			streamReader = xmlInputFactory.createXMLStreamReader(xmlReader);
			int event = 0;
			Stack<Object> xmlElements = new Stack<Object>();
			while (streamReader.hasNext()) {
				event = streamReader.next();
				switch (event) {
				case XMLStreamConstants.START_DOCUMENT:
					// 文档开始
					break;
				case XMLStreamConstants.START_ELEMENT:
					// xml标签开始
					// 记录当前的标签
					String elementName = streamReader.getName().toString();
					if ("index-data".equals(elementName)) {
						// XML根节点
					} else if ("document".equals(elementName)) {
						// 如果是一个Record的开始，根据docType属性，创建一个Record
						Document doc = new Document();
						xmlElements.push(doc);
						foundPkey = false;
					} else if ("field".equals(elementName)) {
						// 读取name\pkey\index\store属性
						String name = null;
						boolean pkey = false;
						boolean store = false;
						String index = null;
						int attrCount = streamReader.getAttributeCount();
						for (int i = 0; i < attrCount; i++) {
							String attrName = streamReader.getAttributeName(i)
									.toString();
							String attrValue = streamReader
									.getAttributeValue(i);
							if ("name".equals(attrName)) {
								name = attrValue;
							} else if (!foundPkey && "pkey".equals(attrName)) {
								pkey = Boolean.parseBoolean(attrValue);
							} else if ("store".equals(attrName)) {
								store = Boolean.parseBoolean(attrValue);
							} else if ("index".equals(attrName)) {
								index = attrValue;
							}
						}
						// 根据XML属性，设定Lucene Field的属性
						String docFieldName = name;
						Store docFieldStore = null;
						Index docFieldIndex = null;
						if (pkey) {
							foundPkey = true;
							// 校验PKey FieldName
							if (docFieldName == null
									|| this.indexConfig.getKeyFieldName() == null
									|| !docFieldName.equals(this.indexConfig
											.getKeyFieldName())) {
								throw new IllegalArgumentException(
										"数据对象PKey属性校验失败，名称为空或不匹配!");
							}
							// PKey必须存储
							docFieldStore = Store.YES;
							// PKey索引，不切分
							docFieldIndex = Index.NOT_ANALYZED_NO_NORMS;
						} else {
							if (store) {
								docFieldStore = Store.YES;
							} else {
								docFieldStore = Store.NO;
							}
							if (index != null) {
								if ("ANALYZED".equals(index)) {
									docFieldIndex = Index.ANALYZED_NO_NORMS;
								} else {
									docFieldIndex = Index.NOT_ANALYZED_NO_NORMS;
								}
							} else {
								docFieldIndex = Index.NO;
							}
						}
						// 构造Lucene Field,暂时不设定值
						Field docField = null;
						if (docFieldStore == Store.NO
								&& docFieldIndex == Index.NO) {
							docField = IGNORE_FIELD;
						} else {
							docField = new Field(
									docFieldName, "", docFieldStore,
									docFieldIndex);
						}
						// 把当前的docField压入栈顶
						xmlElements.push(docField);
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					// xml标签CDATA内容
					// 跳过空格
					if (streamReader.isWhiteSpace()) {
						break;
					}
					// 读出栈顶的Field
					Field docField = (Field) xmlElements
							.peek();
					if (IGNORE_FIELD != docField) {
						// 读出xml field值
						String docFieldValue = streamReader.getText();
						docField.setValue(docFieldValue);
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					// XML元素结束
					elementName = streamReader.getName().toString();
					if ("field".equals(elementName)) {
						// 弹出出栈顶的Field（必须的）
						docField = (Field) xmlElements
								.pop();
						if (IGNORE_FIELD != docField) {
							// 读出栈顶的Document（不是弹出哦！！）
							Document doc = (Document) xmlElements.peek();
							// 加入当前的docField
							doc.add(docField);
						}
					} else if ("document".equals(elementName)) {
						if (!foundPkey) {
							throw new IllegalArgumentException("数据对象缺少PKey属性!");
						}
						// 弹出栈顶的Document
						Document doc = (Document) xmlElements.pop();
						// 加入结果集
						docs.add(doc);
					} else if ("index-data".equals(elementName)) {
						// XML根元素退出
					}
					break;
				case XMLStreamConstants.END_DOCUMENT:
					// 文档结束
					break;
				}
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} finally {
			if (streamReader != null) {
				try {
					streamReader.close();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			}
		}
		return docs;
	}
}
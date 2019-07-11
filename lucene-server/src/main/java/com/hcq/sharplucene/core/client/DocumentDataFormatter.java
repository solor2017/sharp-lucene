package com.hcq.sharplucene.core.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import com.hcq.sharplucene.core.annotation.FieldIndex;
import com.hcq.sharplucene.core.annotation.FieldStore;
import com.hcq.sharplucene.core.annotation.PKey;
import com.hcq.sharplucene.core.index.IndexConfig;
/**
 * 索引格式转换
 * @Author: solor
 * @Since: 2.0
 * @Description:
 */
public class DocumentDataFormatter extends BasicDataFormatter {
	/**
	 * 从javaBean列表生成List<Document>
	 * @param dataBeans
	 * @param indexConfig
	 * @return
	 */
	public static List<Document> createDocumentfromBeans(List<?> dataBeans,IndexConfig indexConfig) {
		List documents = new ArrayList();
		if (dataBeans != null) {
			for (Iterator localIterator = dataBeans.iterator(); localIterator.hasNext();) {
				Object dataBean = localIterator.next();
				Document document = createDocumentfromBean(dataBean,indexConfig);
				documents.add(document);
			}
		}
		return documents;
	}
	/**
	 * 解析单个bean对象
	 * @param dataBean
	 * @param indexConfig
	 * @return
	 */
	public static Document createDocumentfromBean(Object dataBean,IndexConfig indexConfig) {
		Document doc = new Document();
		//获取dataBean类对象
		Class dataBeanClass = dataBean.getClass();
		//获取单签Bean的所有属性，本类的
		java.lang.reflect.Field[] fields = dataBeanClass.getDeclaredFields();
		//将Bean的属性转成lucene document的Field
		boolean foundPkey = false;
		
		for (java.lang.reflect.Field beanField : fields) {
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
				String docFieldName = beanField.getName();
				Store docFieldStore = null;
				Index docFieldIndex = null;
				//处理PKey属性
				PKey pKeyAnno = (PKey) beanField.getAnnotation(PKey.class);
				if ((!foundPkey) && (pKeyAnno != null)) {
					if ((indexConfig.getKeyFieldName() == null)|| (!docFieldName.equals(indexConfig.getKeyFieldName()))) {
						throw new IllegalArgumentException("数据对象PKey属性校验失败，名称为空或不匹配!");
					}
					foundPkey = true;
					//PKey必须存储
					docFieldStore = Store.YES;
					//PKey索引，不切分
					docFieldIndex = Index.NOT_ANALYZED_NO_NORMS;
				} else {
					String store = readDocFieldStore(beanField);
					if ("YES".equals(store))
						docFieldStore = Store.YES;
					else {
						docFieldStore = Store.NO;
					}

					String index = readDocFieldIndex(beanField);
					if ("NO".equals(index))
						docFieldIndex = Index.NO;
					else if ("NOT_ANALYZED".equals(index))
						docFieldIndex = Index.NOT_ANALYZED_NO_NORMS;
					else if ("ANALYZED".equals(index)) {
						docFieldIndex = Index.ANALYZED_NO_NORMS;
					}
				}
				Field docField = new Field(
						docFieldName, docFieldValue, docFieldStore,
						docFieldIndex);

				doc.add(docField);
			}

		}
		//如果在配置文件中没有主键，抛出异常
		if (!foundPkey) {
			throw new IllegalArgumentException("数据对象缺少PKey属性!");
		}
		return doc;
	}
}
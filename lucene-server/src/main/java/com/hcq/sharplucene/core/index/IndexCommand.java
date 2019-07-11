package com.hcq.sharplucene.core.index;

import org.apache.lucene.document.Document;

/**
 * 索引操作指令
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
class IndexCommand {
	private Operate operate;
	private Document document;
	private Status status;

	IndexCommand(Operate operate, Document document) {
		this.operate = operate;
		this.document = document;
		this.status = Status.TODO;
	}

	public Operate getOperate() {
		return this.operate;
	}

	public void setOperate(Operate operate) {
		this.operate = operate;
	}

	public Document getDocument() {
		return this.document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	static enum Operate {
		NULL,

		BUILD,//新建

		ADD,//新增

		MODIFY, //修改(复合操作 - 内存索引(删|增) , 主索引(删|增) , 历史索引(删|增))

		DELETE,//删除

		OPTIMIZE,//优化

		CLEAR;//清除
	}

	static enum Status {
		TODO,//待处理

		DELETED,//已删除

		DONE;//已处理
	}
}

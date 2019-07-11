package com.hcq.sharplucene.core.index;

import java.io.File;
import org.apache.lucene.analysis.Analyzer;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 *  索引配置信息类
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public class IndexConfig {
	private String indexName;//索引名称
	private String keyFieldName;//索引主键field名称，默认
	private String rootDir;//索引根不露
	private Analyzer luceneAnalyzer = new IKAnalyzer();//分词器，不可更改

	private int queueTriggerCritical = 500;//队列允许触发的值，当指令队列中存放的指令个数超过这个值时，则出发执行线程

	private int queueHoldLimited = 3000;//指令队列装载上限，当装载队列存放的个数超过此值时，则杜塞队列，停止向此队列中放入

	private int queuePollPeriod = 60000;//wait的时间，指令队列轮休时间

	private int maxFieldLength = 100000;//document中允许存放的最大词元数（Term数）

	private int bufferedDocs = 3000;//建索引时，文档缓冲数，即内存中的文档数超过此值时，则迁移到磁盘中

	private int RAMBufferSizeMB = 256;//建索引时，内存缓冲区大小

	//当新的文档呗加入的时候，开始被默认写入内存，再根据maxMergeDocs的值确定写入硬盘。
	//提高lucene索引性能的最简单的方法是调整IndexWriter类的mergeFactor成员变量的值，该值告诉lucene在写入硬盘之前在内存中存储多少文档
	//每增加maxMergeDocs个文档，lucene会增加第mergeFactor个大小为maxMergeDocs的片段
	private int maxMergeDocs = 1000000;

	private int mergeFactor = 64;//lucene索引合并系数

	/**
	 * 文档迁移的临界值，当大于此值时，要求迁移。（对索引性能非常非常重要）单位毫秒
	 */
	private long migrateCritical = 31536000000L;

	private int maxMigrateDocs = 500000;//单次文档迁移的最大值

	private long maintainTaskIdlePeriod = 3600000L;//维护任务线程允许空闲的时间

	private boolean enableBackup = false;//是否启用备份索引
	private IndexEliminatePolicy eliminatePolicy;//索引淘汰策略

	public File getMainDirectory() {
		String mainDirPath = this.rootDir + "/main";
		return new File(mainDirPath);
	}

	public File getBackupDirectory() {
		String backupDirPath = this.rootDir + "/history";
		return new File(backupDirPath);
	}

	public String getIndexName() {
		return this.indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getKeyFieldName() {
		return this.keyFieldName;
	}

	public void setKeyFieldName(String keyFieldName) {
		this.keyFieldName = keyFieldName;
	}

	public String getRootDir() {
		return this.rootDir;
	}

	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}

	public Analyzer getLuceneAnalyzer() {
		return this.luceneAnalyzer;
	}

	public void setLuceneAnalyzer(Analyzer luceneAnalyzer) {
		this.luceneAnalyzer = luceneAnalyzer;
	}

	public int getQueueTriggerCritical() {
		return this.queueTriggerCritical;
	}

	public void setQueueTriggerCritical(int queueTriggerCritical) {
		this.queueTriggerCritical = queueTriggerCritical;
	}

	public int getQueueHoldLimited() {
		return this.queueHoldLimited;
	}

	public void setQueueHoldLimited(int queueHoldLimited) {
		this.queueHoldLimited = queueHoldLimited;
	}

	public int getQueuePollPeriod() {
		return this.queuePollPeriod;
	}

	public void setQueuePollPeriod(int queuePollPeriod) {
		this.queuePollPeriod = queuePollPeriod;
	}

	public int getMaxFieldLength() {
		return this.maxFieldLength;
	}

	public void setMaxFieldLength(int maxFieldLength) {
		this.maxFieldLength = maxFieldLength;
	}

	public int getBufferedDocs() {
		return this.bufferedDocs;
	}

	public void setBufferedDocs(int bufferedDocs) {
		this.bufferedDocs = bufferedDocs;
	}

	public int getRAMBufferSizeMB() {
		return this.RAMBufferSizeMB;
	}

	public void setRAMBufferSizeMB(int bufferSizeMB) {
		this.RAMBufferSizeMB = bufferSizeMB;
	}

	public int getMaxMergeDocs() {
		return this.maxMergeDocs;
	}

	public void setMaxMergeDocs(int maxMergeDocs) {
		this.maxMergeDocs = maxMergeDocs;
	}

	public int getMergeFactor() {
		return this.mergeFactor;
	}

	public void setMergeFactor(int mergeFactor) {
		this.mergeFactor = mergeFactor;
	}

	public long getMigrateCritical() {
		return this.migrateCritical;
	}

	public void setMigrateCritical(long migrateCritical) {
		this.migrateCritical = migrateCritical;
	}

	public int getMaxMigrateDocs() {
		return this.maxMigrateDocs;
	}

	public void setMaxMigrateDocs(int maxMigrateDocs) {
		this.maxMigrateDocs = maxMigrateDocs;
	}

	public long getMaintainTaskIdlePeriod() {
		return this.maintainTaskIdlePeriod;
	}

	public void setMaintainTaskIdlePeriod(long maintainTaskIdlePeriod) {
		this.maintainTaskIdlePeriod = maintainTaskIdlePeriod;
	}

	public boolean isEnableBackup() {
		return this.enableBackup;
	}

	public void setEnableBackup(boolean enableBackup) {
		this.enableBackup = enableBackup;
	}

	public IndexEliminatePolicy getEliminatePolicy() {
		return this.eliminatePolicy;
	}

	public void setEliminatePolicy(IndexEliminatePolicy eliminatePolicy) {
		this.eliminatePolicy = eliminatePolicy;
	}
}
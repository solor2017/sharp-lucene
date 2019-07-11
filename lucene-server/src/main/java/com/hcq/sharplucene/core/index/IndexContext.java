package com.hcq.sharplucene.core.index;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.ParallelMultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import com.hcq.sharplucene.core.search.PagedResultSet;

/**
 * 索引上下文信息，每个索引索引控制器上下文包含一类（docType）的索引目录
 * 同一类索引目录包括：内存及时索引 ； 主索引 两个部分
 * 内存及时索引--提供及时搜索功能，在记录归并到文件索引（磁盘索引）后，及时删除对应记录
 * 主索引--文件索引，文件索引保存近期的索引记录，支持三种触发条件
 * 1.队列中，任务数达到触发点（例如：500条）
 * 2.索引间隔时间到达触发点（例如：60秒）
 * 3.手动触发
 * ---备份索引--包含内存索引和主索引的全部内容
 * 所有的对doc的处理都交给线程类 MainIndexController和MemoryIndexController
 * 新来的文档全部索引到内存索引中，并且是索引完IndexWriter就commit，IndexReader就重新打开
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
public class IndexContext {
	/**
	 * 索引配置参数
	 */
	private IndexConfig indexConfig;
	/**|
	 * 内存实时索引目录
	 */
	private Directory memIndexDir;
	/**
	 * 内存索引控制器
	 */
	private MemoryIndexController memoryIndexController;
	/**
	 * 磁盘索引控制器
	 */
	private MainIndexController mainIndexController;
	/**
	 * 备份索引控制器
	 */
	private BackupIndexController backupIndexController;
	/**
	 * 内存索引reader
	 */
	private IndexReader memoryReader;
	/**
	 * 内存索引reader锁
	 */
	private Object memoryReaderLock = new Object();
	/**
	 * 主索引reader
	 */
	private IndexReader mainReader;
	/**
	 * 主索引reader锁
	 */
	private Object mainReaderLock = new Object();
	/**
	 * 备份索引reader
	 */
	private IndexReader backupReader;
	/**
	 * 备份索引reader锁
	 */
	private Object backupReaderLock;
	/**
	 * 内存索引优化标志
	 */
	private boolean memoryIndexOptFlag;
	/**
	 * 主索引优化标志
	 */
	private boolean mainIndexOptFlag;
	/**
	 * 备份索引优化标志
	 */
	private boolean backupIndexOptFlag;
	/**
	 * 定时器（用于控制索引优化）
	 */
	private Timer indexTimer;
	/**
	 * 清除标志
	 */
	private boolean cleanupFlag;

	public IndexContext(IndexConfig indexConfig) {
		
		this.indexConfig = indexConfig;
		//初始化内存索引
		this.memIndexDir = new RAMDirectory();
		//初始化内存索引控制器
		this.memoryIndexController = new MemoryIndexController(this);
		this.memoryIndexOptFlag = false;
		//初始化主索引控制器
		this.mainIndexController = new MainIndexController(this);
		this.mainIndexOptFlag = false;
		/**
		 * 如果开启了备份索引，则初始化备份索引控制器
		 */
		if (indexConfig.isEnableBackup()) {
			this.backupIndexController = new BackupIndexController(this);
			this.backupReaderLock = new Object();
			this.backupIndexOptFlag = false;
		}
		/**
		 * 初始化定时器
		 */
		this.indexTimer = new Timer(true);
		this.indexTimer.schedule(new IndexMaintianTimerTask(this),
									this.indexConfig.getMaintainTaskIdlePeriod(), 
									this.indexConfig.getMaintainTaskIdlePeriod()
									);
	}

	/**
	 * 关闭整个context上下文
	 */
	public void close() {
		//内存索引锁
		synchronized (this.memoryReaderLock) {
			if (this.memoryReader != null) {
				try {
					this.memoryReader.close();
					this.memoryReader = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		this.memoryIndexController.stopService();
		//主索引
		synchronized (this.mainReaderLock) {
			if (this.mainReader != null) {
				try {
					this.mainReader.close();
					this.mainReader = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		this.mainIndexController.stopService();
		//备份索引
		if (this.indexConfig.isEnableBackup()) {
			synchronized (this.backupReaderLock) {
				if (this.backupReader != null) {
					try {
						this.backupReader.close();
						this.backupReader = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			this.backupIndexController.stopService();
		}
		//关闭定时器
		this.indexTimer.cancel();
	}

	public IndexConfig getIndexConfig() {
		return this.indexConfig;
	}

	public Directory getMemIndexDir() {
		return this.memIndexDir;
	}

	public MemoryIndexController getMemoryIndexController() {
		return this.memoryIndexController;
	}

	public MainIndexController getMainIndexController() {
		return this.mainIndexController;
	}

	public BackupIndexController getBackupIndexController() {
		if (!this.indexConfig.isEnableBackup()) {
			throw new UnsupportedOperationException("没有可用的备份索引器!");
		}
		return this.backupIndexController;
	}
	/**
	 * 建立索引
	 * @param xmlDataString
	 */
	public void build(String xmlDataString) {
		IndexDataAdapter dataAdapter = new IndexDataAdapter(this.indexConfig);
		List<Document> indexDocs = dataAdapter.xmlToDocument(xmlDataString);
		for (Document doc : indexDocs)
			build(doc);
	}
	/**
	 * 建立索引
	 * @param doc
	 */
	public void build(Document doc) {
		if (doc == null) {
			return;
		}
		IndexCommand command = new IndexCommand(IndexCommand.Operate.BUILD, doc);
		this.mainIndexController.sendCommand(command, false);
	}
	/**
	 * 备份索引
	 * @param xmlDataString
	 */
	public void backup(String xmlDataString) {
		if (!this.indexConfig.isEnableBackup()) {
			throw new UnsupportedOperationException("没有可用的备份索引器!");
		}
		IndexDataAdapter dataAdapter = new IndexDataAdapter(this.indexConfig);
		List<Document> indexDocs = dataAdapter.xmlToDocument(xmlDataString);
		for (Document doc : indexDocs)
			backup(doc);
	}
	/**
	 * 备份索引（直接往备份索引里插入）
	 * @param doc
	 */
	public void backup(Document doc) {
		if (!this.indexConfig.isEnableBackup()) {
			throw new UnsupportedOperationException("没有可用的备份索引器!");
		}
		if (doc == null) {
			return;
		}
		IndexCommand command = new IndexCommand(IndexCommand.Operate.BUILD, doc);
		this.backupIndexController.sendCommand(command, false);
	}
	/**
	 * 新增索引
	 * @param xmlDataString
	 */
	public void add(String xmlDataString) {
		IndexDataAdapter dataAdapter = new IndexDataAdapter(this.indexConfig);
		List<Document> indexDocs = dataAdapter.xmlToDocument(xmlDataString);
		for (Document doc : indexDocs)
			add(doc);
	}
	/**
	 * 新增索引
	 * 内存索引能保证实时性，indexreader开始时读取磁盘索引，查询时读取内存索引
	 * @param doc
	 */
	public void add(Document doc) {
		if (doc == null) {
			return;
		}
		IndexCommand command = new IndexCommand(IndexCommand.Operate.ADD, doc);
		this.memoryIndexController.sendCommand(command);//线程加队列实现索引创建请求
	}
	/**
	 * 修改索引
	 * @param xmlDataString
	 */
	public void update(String xmlDataString) {
		IndexDataAdapter dataAdapter = new IndexDataAdapter(this.indexConfig);
		List<Document> indexDocs = dataAdapter.xmlToDocument(xmlDataString);
		for (Document doc : indexDocs)
			update(doc);
	}
	/**
	 * 修改索引
	 * @param doc
	 */
	public void update(Document doc) {
		if (doc == null) {
			return;
		}
		IndexCommand command = new IndexCommand(IndexCommand.Operate.MODIFY,doc);
		this.memoryIndexController.sendCommand(command);
	}
	/**
	 * 删除索引
	 * @param xmlDataString
	 */
	public void delete(String xmlDataString) {
		IndexDataAdapter dataAdapter = new IndexDataAdapter(this.indexConfig);
		List<Document> indexDocs = dataAdapter.xmlToDocument(xmlDataString);
		for (Document doc : indexDocs)
			delete(doc);
	}
	/**
	 * 删除索引
	 * @param doc
	 */
	public void delete(Document doc) {
		if (doc == null) {
			return;
		}
		IndexCommand command = new IndexCommand(IndexCommand.Operate.DELETE,doc);
		this.memoryIndexController.sendCommand(command);
	}
	/**
	 * 优化索引
	 * @param immediately 是否立即优化
	 */
	public void optimize(boolean immediately) {
		IndexCommand command = new IndexCommand(IndexCommand.Operate.OPTIMIZE,null);
		this.mainIndexController.sendCommand(command, immediately);
	}
	/**
	 * 优化备份索引
	 * @param immediately 是否立即优化
	 */
	public void optimizeBackupIndex(boolean immediately) {
		IndexCommand command = new IndexCommand(IndexCommand.Operate.OPTIMIZE,null);
		this.backupIndexController.sendCommand(command, immediately);
	}
	/**
	 * 搜索，返回带分页是索引集
	 * @param query
	 * @param pageNo
	 * @param pageSize
	 * @param sort
	 * @param inBackupIndex
	 * @return
	 */
	public PagedResultSet search(Query query, int pageNo, int pageSize,
			Sort sort, boolean inBackupIndex) {
		PagedResultSet pagedResultSet = new PagedResultSet();

		if (query == null) {
			return pagedResultSet;
		}
		if (sort == null) {
			sort = new Sort(new SortField(null, 1, true));
		}
		if (pageNo <= 0) {
			pageNo = 1;
		}
		pagedResultSet.setPageNo(pageNo);
		if (pageSize <= 0) {
			pageSize = 20;
		}
		pagedResultSet.setPageSize(pageSize);
		 //2.计算搜索规模
		long searchScale = pageNo * pageSize;
		//如果搜索规模过大
		if (searchScale >= 2147483647L) {
			throw new IllegalArgumentException("搜索范围过大");
		}
		//3.获取查询器
		Searcher seeker = null;
		if ((inBackupIndex) && (this.indexConfig.isEnableBackup()))
			seeker = getBackupIndexSearcher();
		else {
			seeker = getIndexSearcher();
		}

		if (seeker == null) {
			return pagedResultSet;
		}
		//4.计算结果集起始位置
		int resultBegin = (pageNo - 1) * pageSize;
		int resultEnd = (int) searchScale;
		try {
			//5.执行搜索
			TopDocs topDocs = seeker.search(query, null, (int) searchScale,sort);
			pagedResultSet.setTotalHit(topDocs.totalHits);
			//如果起始位置越界
			if (resultBegin > topDocs.totalHits)
				return pagedResultSet;
			//计算结束位置
			if (resultEnd > topDocs.totalHits) {
				resultEnd = topDocs.totalHits;
			}
			//读取结果集
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			Document[] docs = new Document[resultEnd - resultBegin];
			for (int i = resultBegin; i < resultEnd; i++) {
				Document resultDoc = seeker.doc(scoreDocs[i].doc);
				docs[(i - resultBegin)] = resultDoc;
			}
			pagedResultSet.setResultDocument(query,docs);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeSearcher(seeker);
		}
		return pagedResultSet;
	}
	/**
	 * 获取内存索引reader
	 * @return
	 */
	public IndexReader getMemoryIndexReader() {
		IndexReader cloneReader = null;
		try {
			synchronized (this.memoryReaderLock) {
				if (this.memoryReader == null) {
					//如果索引存在，构建memoryReader
					if (IndexReader.indexExists(this.memIndexDir)) {
						System.out.println(new Date()+ " openMemoryIndexReader");
						this.memoryReader = IndexReader.open(this.memIndexDir,true);
					}
				} else if (!this.memoryIndexOptFlag) {//如果当前没有优化内存索引的任务存在
					IndexReader oldReader = this.memoryReader;
					//更新reader
					this.memoryReader = this.memoryReader.reopen(true);
					//关闭酒reader
					if (this.memoryReader != oldReader) {
						oldReader.close();
					}

				}
				//克隆当前的reader
				if (this.memoryReader != null)
					cloneReader = this.memoryReader.clone(true);
			}
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cloneReader;
	}
	/**
	 * 获取主索引reader
	 * @return
	 */
	public IndexReader getMainIndexReader() {
		IndexReader cloneReader = null;
		try {
			synchronized (this.mainReaderLock) {
				if (this.mainReader == null) {
					Directory indexDir = FSDirectory.open(this.indexConfig.getMainDirectory());
					//如果索引存在
					if (IndexReader.indexExists(indexDir)) {
						System.out.println(new Date() + " openMainIndexReader");
						//打开只读reader
						this.mainReader = IndexReader.open(indexDir, true);
					}
				} else if (!this.mainIndexOptFlag) {//如果当前没有优化索引的任务存在
					IndexReader oldReader = this.mainReader;
					//更新当前reader
					this.mainReader = this.mainReader.reopen(true);
					//关闭之前的reader
					if (this.mainReader != oldReader) {
						oldReader.close();
					}
				}
				//克隆当前reader
				if (this.mainReader != null)
					cloneReader = this.mainReader.clone(true);
			}
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cloneReader;
	}
	/**
	 * 获取备份索引reader
	 * @return
	 */
	public IndexReader getBackupIndexReader() {
		if (!this.indexConfig.isEnableBackup()) {//如果备份索引器没有打开
			throw new UnsupportedOperationException("没有可用的备份索引器!");
		}

		IndexReader cloneReader = null;
		try {
			synchronized (this.backupReaderLock) {
				if (this.backupReader == null) {
					//打开索引目录
					Directory indexDir = FSDirectory.open(this.indexConfig.getBackupDirectory());
					//索引存在
					if (IndexReader.indexExists(indexDir)) {
						System.out.println(new Date()+ " openBackupIndexReader");
						//打开只读reader
						this.backupReader = IndexReader.open(indexDir, true);
					}
				} else if (!this.backupIndexOptFlag) {//如果当前没有优化备份索引的任务存在
					IndexReader oldReader = this.backupReader;
					//更新reader
					this.backupReader = this.backupReader.reopen(true);

					if (this.backupReader != oldReader) {
						oldReader.close();
					}
				}
				//克隆当前的reader
				if (this.backupReader != null)
					cloneReader = this.backupReader.clone(true);
			}
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cloneReader;
	}

	public boolean isMemoryIndexOptimizing() {
		return this.memoryIndexOptFlag;
	}

	public boolean isMainIndexOptimizing() {
		return this.mainIndexOptFlag;
	}

	public boolean isBackupIndexOptimizing() {
		return this.backupIndexOptFlag;
	}

	public boolean isCleaning() {
		return this.cleanupFlag;
	}
	/**
	 * 获取当前文档查询条件（词元）
	 * @param doc
	 * @return
	 */
	Term keyTerm(Document doc) {
		if (doc != null) {
			String keyFieldName = this.indexConfig.getKeyFieldName();//获取在配置文件中的主键
			String keyFieldValue = doc.get(keyFieldName);//获取对应的document里的主键
			Term keyTerm = new Term(keyFieldName, keyFieldValue);
			return keyTerm;
		}
		return null;
	}
	/**
	 * 通知/解除内存索引的优化标志
	 * @param optimize
	 */
	void notifyMemoryIndexOpt(boolean optimize) {
		synchronized (this.memoryReaderLock) {
			if (this.memoryReader != null) {
				try {
					this.memoryReader.close();//先关闭，节省内存空间
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.memoryReader = null;
			}
			this.memoryIndexOptFlag = optimize;
		}
	}
	/**
	 * 通知/关闭当前的主索引优化标志
	 * @param optimize
	 */
	void notifyMainIndexOpt(boolean optimize) {
		synchronized (this.mainReaderLock) {
			if (this.mainReader != null) {
				try {
					this.mainReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.mainReader = null;
			}
			this.mainIndexOptFlag = optimize;
		}
	}
	/**
	 * 通知/关闭备份索引优化标志
	 * @param optimize
	 */
	void notifyBackupIndexOpt(boolean optimize) {
		if (!this.indexConfig.isEnableBackup()) {
			throw new UnsupportedOperationException("没有可用的备份索引器!");
		}

		synchronized (this.backupReaderLock) {
			if (this.backupReader != null) {
				try {
					this.backupReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.backupReader = null;
			}
			this.backupIndexOptFlag = optimize;
		}
	}
	/**
	 * 获取索引搜索器
	 * @return
	 */
	private Searcher getIndexSearcher() {
		//得到内存索引reader
		IndexReader memReader = getMemoryIndexReader();
		Searcher memSearcher = null;
		if (memReader != null) {
			memSearcher = new IndexSearcher(memReader);
		}
		//得到主索引的reader
		IndexReader mainReader = getMainIndexReader();
		Searcher mainSearcher = null;
		if (mainReader != null) {
			mainSearcher = new IndexSearcher(mainReader);
		}
		/**
		 * 下面代码真正保证了查询的实时性
		 */
		Searcher theSearcher = null;
		if ((memSearcher != null) && (mainSearcher != null))//如果memSearcher和mainSearcher都存在。进行联合查询
			try {
				//联合查询ParallelMultiSearcher继承了MultiSearcher，是多线程版的MultiSearcher（对MultiSearcher里的seacher()方法进行了多线程控制）
				theSearcher = new ParallelMultiSearcher(new Searcher[] {mainSearcher, memSearcher });
			} catch (IOException e) {
				e.printStackTrace();
			}
		else if (memSearcher != null)
			theSearcher = memSearcher;//否则只在内存索引里查询
		else {
			theSearcher = mainSearcher;//否则只在主索引里查询
		}

		return theSearcher;
	}
	/**
	 * 获取备份索引搜索器
	 * @return
	 */
	private Searcher getBackupIndexSearcher() {
		if (!this.indexConfig.isEnableBackup()) {
			throw new UnsupportedOperationException("没有可用的备份索引器!");
		}
		//得到备份索引reader
		IndexReader backupReader = getBackupIndexReader();
		Searcher backupSearcher = null;
		if (backupReader != null) {
			backupSearcher = new IndexSearcher(backupReader);
		}
		return backupSearcher;
	}
	/**
	 * 递归关闭查询器（关闭查询器中的reader）
	 * @param searchable
	 */
	private void closeSearcher(Searchable searchable) {
		if (searchable != null) {
			if ((searchable instanceof MultiSearcher)) {
				Searchable[] searchables = ((MultiSearcher) searchable).getSearchables();
				for (Searchable s : searchables) {
					closeSearcher(s);
				}
			}
			if ((searchable instanceof IndexSearcher)) {
				IndexReader reader = ((IndexSearcher) searchable).getIndexReader();
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * 移除索引方法，仅在备份索引时使用
	 * 每次迁移的文档数不能超过indexConfig.getMaxMigrateDocs(),以保证主索引的容量
	 */
	public synchronized void cleanup() {
		IndexEliminatePolicy policy = this.indexConfig.getEliminatePolicy();

		if ((policy == null) || (this.cleanupFlag)) {//如果正在执行迁移操作，忽略此次调用
			return;
		}
		//设置迁移保证
		this.cleanupFlag = true;
		//得到过期文档
		List<Document> overDueDocuments = queryOverDueDocuments(policy.getEliminateCondition(this.indexConfig));
		for (Document doc : overDueDocuments) {
			//将过期文档从主索引迁移
			this.mainIndexController.sendCommand(new IndexCommand(IndexCommand.Operate.CLEAR, doc), false);
		}
		this.cleanupFlag = false;
	}
	/**
	 * 根据Query查询主索引,查找要清理的文档
	 * 查询结果最多不超过 indexConfig.getMaxMigrateDocs()
	 * @param query
	 * @return
	 */
	private List<Document> queryOverDueDocuments(Query query) {
		if (query != null) {
			IndexReader mainIndexReader = null;
			try {
				mainIndexReader = getMainIndexReader();
				if (mainIndexReader != null) {
					List<Document> docs = new ArrayList<Document>();

					Searcher searcher = new IndexSearcher(mainIndexReader);
					//搜索文档ID
					TopDocs topDocs = searcher.search(query, this.indexConfig.getMaxMigrateDocs());

					for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
						//取出文档内容
						Document doc = mainIndexReader.document(scoreDoc.doc);
						docs.add(doc);
					}
					//List localList1 = docs;
					return docs;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (mainIndexReader != null)
					try {
						mainIndexReader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			if (mainIndexReader != null) {
				try {
					mainIndexReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return new ArrayList<Document>(0);
	}
	/**
	 * 索引维护定时任务
	 * 任务包括：1.每天的索引归档 2.主索引优化3.备份索引优化
	 * @author huchangqing
	 *
	 */
	
	private class IndexMaintianTimerTask extends TimerTask {
		private IndexContext context;

		IndexMaintianTimerTask(IndexContext context) {
			this.context = context;
		}

		public void run() {
			try {
				//获取日期
				Calendar rightNow = Calendar.getInstance();
				//获取小时
				int hourOfDay = rightNow.get(11);
				//在每天的1:00到4:00出发迁移任务
				if ((hourOfDay >= 1) && (hourOfDay < 4)) {
					//如果主索引正在优化，不允许迁移
					if (!IndexContext.this.mainIndexOptFlag) {
						System.out.println(new Date() + " : "+ IndexContext.this.indexConfig.getIndexName()+ " begin migrate... ");
						if (IndexContext.this.indexConfig.isEnableBackup()) {
							//主索引过期清理
							this.context.cleanup();
						}
						//优化操作
						this.context.optimize(false);
					}
					//如果历史索引正在优化，则不再一次发起优化
					if ((IndexContext.this.indexConfig.isEnableBackup())&& (!IndexContext.this.backupIndexOptFlag)) {
						//优化历史索引
						this.context.optimizeBackupIndex(false);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
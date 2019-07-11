package com.hcq.sharplucene.core.index;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
/**
 * 全局备份索引控制器
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
class BackupIndexController implements Runnable {
	/**
	 * 控制器名称
	 */
	private String name;
	/**
	 * 索引上下文
	 */
	private IndexContext context;
	/**
	 * 等待索引的文档队列
	 */
	private IndexCommandQueue commandQueue;
	/**
	 * 待删除文档
	 */
	private List<IndexCommand> toBeDeleted;
	/**
	 * 待新增文档
	 */
	private List<IndexCommand> toBeAdded;
	/**
	 * 线程停止标志
	 */
	private boolean stopFlag;
	/**|
	 * 索引优化标志
	 */
	private boolean optimization;
	/**
	 * 
	 * @param context索引控制器上下文
	 */
	BackupIndexController(IndexContext context) {
		this.context = context;
		this.name = (getClass().getSimpleName() + " for " + context.getIndexConfig().getIndexName());
		init();
	}

	private void init() {
		this.stopFlag = false;
		this.optimization = false;
		//初始化指令队列
		this.commandQueue = new IndexCommandQueue(this.context);
		this.toBeDeleted = new LinkedList();//待删除的文档队列
		this.toBeAdded = new LinkedList();//待增加的文档队列
		//启动执行线程
		new Thread(this, this.name).start();
		System.out.println(this.name + " start.");
	}
	/**
	 * 发送索引变更指令
	 * @param command
	 * @param immediately
	 */
	void sendCommand(IndexCommand command, boolean immediately) {
		synchronized (this.commandQueue) {
			//判断是否超过容器上限
			while (this.commandQueue.size() >= this.context.getIndexConfig().getQueueHoldLimited()) {
				try {
					//超过则等待
					this.commandQueue.wait();
					if (this.stopFlag) {
						return;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//初始化命令执行状态
			command.setStatus(IndexCommand.Status.TODO);
			this.commandQueue.addCommand(command);
			/**
			 * 判断如果队列中的指令达到出发的临界值，or immediately==true。则唤醒线程
			 */
			if ((immediately)
					|| (this.commandQueue.size() >= this.context.getIndexConfig().getQueueTriggerCritical())) {
				this.commandQueue.notify();
			}
		}
	}
	/**
	 * 停止线程
	 */
	void stopService() {
		
		this.stopFlag = true;//停止标志
		this.optimization = false;
		synchronized (this.commandQueue) {
			this.commandQueue.clear();
			this.commandQueue.notifyAll();
		}
		//清空队列
		this.toBeAdded.clear();
		this.toBeDeleted.clear();
	}

	public void run() {
		while (!this.stopFlag) {
			IndexCommand[] commands = (IndexCommand[]) null;
			//同步队列
			synchronized (this.commandQueue) {
				while ((!this.stopFlag) && (this.commandQueue.isEmpty())) {//判断当前标志位停止或队列为空，等待
					try {
						this.commandQueue.wait(this.context.getIndexConfig().getQueuePollPeriod());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (this.stopFlag) {//标志位停止，停止线程
					return;
				}
				//线程被唤醒，取出所有的任务
				commands = this.commandQueue.pollALL();
				 //唤醒生产者线程
				this.commandQueue.notify();
			}

			if (commands == null)
				continue;
			try {
				//执行操作指令
				processIndexCommands(commands);
			} catch (Exception allEx) {
				allEx.printStackTrace();
			}
		}
	}
	/**
	 * 执行索引变更指令
	 * @param commands
	 */
	private void processIndexCommands(IndexCommand[] commands) {
		/**
		 * for循环构建两个指令：待增加和待删除
		 */
		for (IndexCommand command : commands) {
			switch (command.getOperate()) {
			case BUILD :
				this.toBeAdded.add(command);
				break;
			case ADD:
				this.toBeAdded.add(command);
				break;
			case MODIFY:
				this.toBeDeleted.add(command);
				this.toBeAdded.add(command);
				break;
			case DELETE:
				this.toBeDeleted.add(command);
				break;
			case OPTIMIZE:
				this.optimization = true;
				command.setStatus(IndexCommand.Status.DONE);
			}

		}
		//变更索引
		Directory dir = null;
		try {
			//得到索引目录
			dir = FSDirectory.open(this.context.getIndexConfig().getBackupDirectory());
			//判断目录是否存在
			boolean exists = IndexReader.indexExists(dir);
			
			if ((exists) && (!this.toBeDeleted.isEmpty())) {
				removeIndex(this.toBeDeleted, dir);
			}

			if (!this.toBeAdded.isEmpty()) {
				addIndex(this.toBeAdded, dir, !exists);
			}
			//如果存在优化索引的指令，且当前没有其他指令，则执行优化指令
			if ((exists) && (this.optimization)&& (this.commandQueue.isEmpty()))
				optimizeIndex(dir);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dir != null) {
				try {
					dir.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//清空任务队列
			this.toBeDeleted.clear();
			this.toBeAdded.clear();
		}
	}
	/**
	 * 执行删除索引操作
	 * @param commands
	 * @param dir
	 */
	private void removeIndex(List<IndexCommand> commands, Directory dir) {
		//构造IndexReader
		IndexReader indexReader = null;
		try {
			indexReader = IndexReader.open(dir, false);
			 //批量删除
			for (IndexCommand command : commands) {
				Term keyTerm = this.context.keyTerm(command.getDocument());
				//查找当索引中PKey对应的文档
				TermDocs termDocs = indexReader.termDocs(keyTerm);

				if (!termDocs.next())
					continue;
				//删除PKey对应的文档
				indexReader.deleteDocument(termDocs.doc());
				//变更command的操作状态
				switch (command.getOperate().ordinal()) {
				case 4:
					command.setStatus(IndexCommand.Status.DELETED);
					break;
				case 5:
					command.setStatus(IndexCommand.Status.DONE);
				}

			}

			indexReader.flush();
		} catch (IOException e) {
			e.printStackTrace();

			if (indexReader != null)
				try {
					indexReader.close();//当使用当前的IndexReader进行搜索时，即使在不关闭IndexReader的情况下，被删除的Document也不会再出现在搜索结果中
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		} finally {
			if (indexReader != null)
				try {
					indexReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void addIndex(List<IndexCommand> commands, Directory dir,boolean create) {
		IndexWriter indexWriter = null;
		try {
			indexWriter = openWriter(dir, create);

			for (IndexCommand command : commands) {
				Document doc = command.getDocument();
				switch (command.getOperate().ordinal()) {
				case 2:
					indexWriter.addDocument(doc);
					//变更command操作状态
					command.setStatus(IndexCommand.Status.DONE);
					break;
				case 3:
					indexWriter.addDocument(doc);

					command.setStatus(IndexCommand.Status.DONE);
					break;
				case 4:
					/*
					* 如果指令时修改文档，在新增文档前，需要判断  
					* IndexCommand.OPSTATUS_DELETED == command.getOpStatus() 
					* 查看文档是否在实时索引中且已经被删除              
					* 如果IndexCommand.OPSTATUS_DELETED != command.getOpStatus() 
					* 说明文档不在索引中，则不能新增                 
					*/ 
					if (IndexCommand.Status.DELETED != command.getStatus())
						continue;
					indexWriter.addDocument(doc);

					command.setStatus(IndexCommand.Status.DONE);
				}

			}

			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
			if (indexWriter != null) {
				try {
					indexWriter.rollback();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			if (indexWriter != null)
				try {
					indexWriter.close();
				} catch (CorruptIndexException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		} finally {
			if (indexWriter != null)
				try {
					indexWriter.close();
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void optimizeIndex(Directory dir) {
		IndexWriter indexWriter = null;
		try {
			indexWriter = openWriter(dir, false);
			long begin = System.currentTimeMillis();
			System.out.println(this.name + " optimization beign at "+ new Date(begin));
			//通知context，备份索引优化开始
			this.context.notifyBackupIndexOpt(true);
			indexWriter.optimize();
			System.out.println(this.name + " optimization end at "+ new Date(begin) + " cost "+ (System.currentTimeMillis() - begin) + " ms.");
		} catch (IOException e) {
			e.printStackTrace();

			this.optimization = false;
			//通知context，备份索引优化结束
			this.context.notifyBackupIndexOpt(false);
			if (indexWriter != null)
				try {
					indexWriter.close();
				} catch (CorruptIndexException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		} finally {
			this.optimization = false;
			//通知context，备份索引优化结束
			this.context.notifyBackupIndexOpt(false);
			if (indexWriter != null)
				try {
					indexWriter.close();
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	/**
	 * 打开内存索引的写入器
	 * @param dir
	 * @param create
	 * @return
	 * @throws CorruptIndexException
	 * @throws LockObtainFailedException
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	private IndexWriter openWriter(Directory dir, boolean create)throws CorruptIndexException, LockObtainFailedException,
			IOException {
		IndexWriter indexWriter = new IndexWriter(dir, this.context.getIndexConfig().getLuceneAnalyzer(), create,
				MaxFieldLength.LIMITED);
		//是否将多个segment合并
		indexWriter.setUseCompoundFile(false);
		//设置文档中Field的最大可容纳Term的数目
		indexWriter.setMaxFieldLength(this.context.getIndexConfig().getMaxFieldLength());
		//设置索引时，内存的最大缓冲文档数目
		indexWriter.setMaxBufferedDocs(this.context.getIndexConfig().getBufferedDocs());
		 //设置索引时，内存的最大缓冲
		indexWriter.setRAMBufferSizeMB(this.context.getIndexConfig().getRAMBufferSizeMB());
		//设置合并参数,History库是Main的两倍
		indexWriter.setMergeFactor(this.context.getIndexConfig().getMergeFactor() * 2);
		//设置每个index segment的最大文档数目
		indexWriter.setMaxMergeDocs(this.context.getIndexConfig().getMaxMergeDocs());
		return indexWriter;
	}
}
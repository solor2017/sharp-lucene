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
import org.apache.lucene.store.LockObtainFailedException;

/**
 * 内存索引操作控制器
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
class MemoryIndexController implements Runnable {
	/**
	 * 控制器名称
	 */
	private String name;
	/**
	 * 索引控制器上下文
	 */
	private IndexContext context;
	/**
	 * 等待索引的文档队列
	 */
	private IndexCommandQueue commandQueue;
	/**
	 * 待删除的文档
	 */
	private List<IndexCommand> toBeDeleted;
	/**
	 * 待新增的文档
	 */
	private List<IndexCommand> toBeAdded;
	/**
	 * 线程停止标志
	 */
	private boolean stopFlag;
	/**
	 * 优化标志
	 */
	private boolean optimization;
	/**
	 * 索引变更计数器
	 */
	private int updateCount;

	MemoryIndexController(IndexContext context) {
		this.context = context;
		this.name = (getClass().getSimpleName() + " for " + context.getIndexConfig().getIndexName());
		init();
	}
	/**
	 * 初始化索引控制器
	 */
	private void init() {
		this.stopFlag = false;
		this.optimization = false;
		this.updateCount = 0;
		//初始化指令队列
		this.commandQueue = new IndexCommandQueue(this.context);
		this.toBeDeleted = new LinkedList();
		this.toBeAdded = new LinkedList();
		//启动线程
		new Thread(this, this.name).start();
		System.out.println(this.name + " start.");
	}
	/**
	 * 发送索引变更指令
	 * @param command
	 */
	void sendCommand(IndexCommand command) {
		synchronized (this.commandQueue) {
			//如果超过容量上限，当前线程等待
			while (this.commandQueue.size() >= this.context.getIndexConfig().getQueueHoldLimited()) {
				try {
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
			//立即唤醒消费者（处理）线程
			this.commandQueue.notifyAll();
		}
	}
	/**
	 * 发送清楚内存索引的指令（来至MainIndexController的回送）
	 * @param command
	 */
	void sendClear(IndexCommand command) {
		if (IndexCommand.Operate.CLEAR != command.getOperate()) {
			return;
		}
		//初始化指令执行状态
		command.setStatus(IndexCommand.Status.TODO);
		synchronized (this.commandQueue) {
			//放入操作队列
			this.commandQueue.addCommand(command);
			//唤醒线程
			this.commandQueue.notifyAll();
		}
	}
	/**
	 * 停止线程服务
	 */
	void stopService() {
		this.stopFlag = true;
		synchronized (this.commandQueue) {
			this.commandQueue.clear();
			this.commandQueue.notifyAll();
		}
		this.toBeAdded.clear();
		this.toBeDeleted.clear();
	}

	public void run() {
		while (!this.stopFlag) {
			IndexCommand[] commands = (IndexCommand[]) null;
			//同步队列
			synchronized (this.commandQueue) {
				//如果当前是停止标志且队列为空，等待
				while ((!this.stopFlag) && (this.commandQueue.isEmpty())) {
					try {
						this.commandQueue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (this.stopFlag) {
					return;
				}
				//取出所有操作指令
				commands = this.commandQueue.pollALL();
				//唤醒等待的线程
				this.commandQueue.notifyAll();
			}

			if (commands == null)
				continue;
			try {
				//执行索引操作指令
				processIndexCommands(commands);
			} catch (Exception allEx) {
				allEx.printStackTrace();
			}
		}
	}
	/**
	 * 执行索引变更指令
	 * @param commands（索引变更指令数组）
	 */
	private void processIndexCommands(IndexCommand[] commands){
    	/*
    	 * * 分离指令，构造删除、新增两个指令队列
    	 * * 对于MemoryIndexController，处理ADD ,MOD ,DEL ,CLR
    	 * */
    	for(IndexCommand command : commands){
    		switch(command.getOperate()){
    		case ADD :
    			this.toBeAdded.add(command);
    			break;
    		case MODIFY :
    			this.toBeDeleted.add(command);
    			this.toBeAdded.add(command);
    			break;
    		case DELETE :
    			this.toBeDeleted.add(command);
    			break;
    		case CLEAR :
    			this.toBeDeleted.add(command);
    			break;
    			}
    		}
    	/*
    	 * 进行索引变更
    	 *  1.先执行删除操作
    	 *  2.在执行新增操作
    	 *  3.在没有更多指令等待的情况下，且有优化指令，则执行优化索引操作
    	 *  */
    	Directory dir = null;
    	try {
    		//获取内存索引目录
    		dir = this.context.getMemIndexDir();
    		//判断索引是否已经建立
    		boolean exists = IndexReader.indexExists(dir);
    		//执行删除索引任务
    		if(exists && !toBeDeleted.isEmpty()){
    			this.removeIndex(toBeDeleted , dir);
    			}
    		//执行新增索引指令 
    		if(!toBeAdded.isEmpty()){
    			this.addIndex(toBeAdded, dir, !exists);
    			}
    		/*
    		 *将指令发送到主索引 
    		 * 内存索引的所有指令都必须复制到主索引 
    		 * */ 
    		for(IndexCommand command : commands){
    			//OPERATE_CLR指令不发送主索引
    			if(IndexCommand.Operate.CLEAR == command.getOperate()){
    				continue;
    				}
    			this.context.getMainIndexController().sendCommand(command, false);
    			}
    		//索引变更到一定数量,触发内存索引优化
    		if(this.updateCount >= 4096){
    			this.optimization = true;
    			this.updateCount = 0;
    			}
    		//如果存在索引优化指令，且指令队列中没有其他指令，则执行优化
    		if(exists && this.optimization && this.commandQueue.isEmpty()){
    			this.optimizeIndex(dir);
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    			} finally{
    				//这里不关闭dir索引目录
    				//清空索引任务队列
    				this.toBeDeleted.clear();
    				this.toBeAdded.clear();
    				}
    			} 
//	private void processIndexCommands(IndexCommand[] commands) {
//		for (IndexCommand command : commands) {
//			//枚举做数组下标，可避免数组越界！队列存放操作指令
//			switch (command.getOperate().ordinal()) {
//			case 3://对应Operate.ADD，执行增加操作
//				this.toBeAdded.add(command);
//				break;
//			case 4://对应Operate.MODIFY,即执行更新操作，（先删除再增加）
//				this.toBeDeleted.add(command);
//				this.toBeAdded.add(command);
//				break;
//			case 5:
//				this.toBeDeleted.add(command);
//				break;
//			case 7:
//				this.toBeDeleted.add(command);
//			case 6:
//			}
//
//		}
//
//		Directory dir = null;
//		try {
//			dir = this.context.getMemIndexDir();
//
//			boolean exists = IndexReader.indexExists(dir);
//
//			if ((exists) && (!this.toBeDeleted.isEmpty())) {
//				removeIndex(this.toBeDeleted, dir);
//			}
//
//			if (!this.toBeAdded.isEmpty()) {
//				addIndex(this.toBeAdded, dir, !exists);
//			}
//
//			for (IndexCommand command : commands) {
//				if (IndexCommand.Operate.CLEAR == command.getOperate()) {
//					continue;
//				}
//				if (IndexCommand.Operate.DELETE == command.getOperate())
//					this.context.getMainIndexController().sendCommand(command,true);
//				else {
//					this.context.getMainIndexController().sendCommand(command,false);
//				}
//			}
//
//			if (this.updateCount >= 4096) {
//				this.optimization = true;
//				this.updateCount = 0;
//			}
//
//			if ((exists) && (this.optimization)&& (this.commandQueue.isEmpty()))
//				optimizeIndex(dir);
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			this.toBeDeleted.clear();
//			this.toBeAdded.clear();
//		}
//	}
	/**
	 * 执行删除索引操作
	 * @param commands 操作指令集
	 * @param dir
	 */
	private void removeIndex(List<IndexCommand> commands, Directory dir) {
		// 构造IndexReader
		IndexReader indexReader = null;
		try {
			indexReader = IndexReader.open(dir, false);
			// 批量删除
			for (IndexCommand command : commands) {
				Term keyTerm = this.context.keyTerm(command.getDocument());
				// 查找当索引中PKey对应的文档
				TermDocs termDocs = indexReader.termDocs(keyTerm);
				/*
				 * PKey是唯一的，则该termDocs.next()只执行一次
				 */
				if (termDocs.next()) {
					// 删除PKey对应的文档
					indexReader.deleteDocument(termDocs.doc());
					this.updateCount++;
					// 变更command操作状态
					switch (command.getOperate()) {
					case MODIFY:
						command.setStatus(IndexCommand.Status.DELETED);
						break;
					case DELETE:
						command.setStatus(IndexCommand.Status.DONE);
						break;
					case CLEAR:
						command.setStatus(IndexCommand.Status.DONE);
						break;
					}
				}
			}
			indexReader.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭reader 提交删除的文档
			if (indexReader != null) {
				try {
					indexReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
//	private void removeIndex(List<IndexCommand> commands, Directory dir) {
//		IndexReader indexReader = null;
//		try {
//			indexReader = IndexReader.open(dir, false);
//
//			for (IndexCommand command : commands) {
//				Term keyTerm = this.context.keyTerm(command.getDocument());
//
//				TermDocs termDocs = indexReader.termDocs(keyTerm);
//
//				if (!termDocs.next())
//					continue;
//				indexReader.deleteDocument(termDocs.doc());
//				this.updateCount += 1;
//
//				switch (command.getOperate().ordinal()) {
//				case 4:
//					command.setStatus(IndexCommand.Status.DELETED);
//					break;
//				case 5:
//					command.setStatus(IndexCommand.Status.DONE);
//					break;
//				case 7:
//					command.setStatus(IndexCommand.Status.DONE);
//				case 6:
//				}
//			}
//
//			indexReader.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//
//			if (indexReader != null)
//				try {
//					indexReader.close();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//		} finally {
//			if (indexReader != null)
//				try {
//					indexReader.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//		}
//	}
	/**
	 * 新增索引
	 * @param commands
	 * @param dir
	 * @param create
	 */
	private void addIndex(List<IndexCommand> commands , Directory dir , boolean create){
		//构造IndexWriter
		IndexWriter indexWriter = null;
		try{
			indexWriter = this.openWriter(dir , create);
			//批量添加文档 
			for(IndexCommand command : commands){
				Document doc = command.getDocument();
				switch(command.getOperate()){
				case ADD :
					indexWriter.addDocument(doc);
					//变更command操作状态 
					command.setStatus(IndexCommand.Status.DONE);
					break;
				case MODIFY :
					/*
					 *  如果指令时修改文档，在新增文档前，需要判断
					 *   IndexCommand.OPSTATUS_DELETED == command.getOpStatus()
					 *   查看文档是否在实时索引中且已经被删除 
					 *   如果IndexCommand.OPSTATUS_DELETED != command.getOpStatus()
					 *   说明文档不在索引中，则不能新增
					 *    */
					if(IndexCommand.Status.DELETED == command.getStatus()){
						indexWriter.addDocument(doc);
						this.updateCount++;
						//变更command操作状态 
						command.setStatus(IndexCommand.Status.DONE);
						}
					break;
					}
				}
			//提交事务
			indexWriter.commit();
			} catch (IOException e) {
				e.printStackTrace();
				if(indexWriter != null){
					try {
						indexWriter.rollback();
						} catch (IOException e1) {
							e1.printStackTrace();
							}
						}
				}finally{
					if(indexWriter != null){
						try {
							indexWriter.close();
							} catch (CorruptIndexException e) {
								e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
									}
								}
					}
				}
//	private void addIndex(List<IndexCommand> commands, Directory dir,boolean create) {
//		IndexWriter indexWriter = null;
//		try {
//			indexWriter = openWriter(dir, create);
//
//			for (IndexCommand command : commands) {
//				Document doc = command.getDocument();
//				switch (command.getOperate().ordinal()) {
//				case 3:
//					indexWriter.addDocument(doc);
//
//					command.setStatus(IndexCommand.Status.DONE);
//					break;
//				case 4:
//					if (IndexCommand.Status.DELETED != command.getStatus())
//						continue;
//					indexWriter.addDocument(doc);
//					this.updateCount += 1;
//
//					command.setStatus(IndexCommand.Status.DONE);
//				}
//
//			}
//
//			indexWriter.commit();//保证文件的实时性
//		} catch (IOException e) {
//			e.printStackTrace();
//			if (indexWriter != null) {
//				try {
//					indexWriter.rollback();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//			}
//
//			if (indexWriter != null)
//				try {
//					indexWriter.close();
//				} catch (CorruptIndexException e1) {
//					e1.printStackTrace();
//				} catch (IOException e2) {
//					e2.printStackTrace();
//				}
//		} finally {
//			if (indexWriter != null)
//				try {
//					indexWriter.close();
//				} catch (CorruptIndexException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//		}
//	}

	// private int[] $SWITCH_TABLE$org$wltea$luci$index$IndexCommand$Operate() {
	// // TODO Auto-generated method stub
	// return null;
	// }

	/**
	 * 优化索引
	 * @param dir
	 */
	private void optimizeIndex(Directory dir) {
		IndexWriter indexWriter = null;
		try {
			indexWriter = this.openWriter(dir, false);
			long begin = System.currentTimeMillis();
			System.out.println(this.name + " optimization beign at "+ new Date(begin));
			//通知context，主索引开始优化
			this.context.notifyMemoryIndexOpt(true);
			indexWriter.optimize();
			System.out.println(this.name + " optimization cost "+ (System.currentTimeMillis() - begin) + " ms.");
		} catch (IOException e) {
			e.printStackTrace();

			this.optimization = false;
			//通知context，主索引优化结束
			this.context.notifyMemoryIndexOpt(false);
			if (indexWriter != null)
				try {
					indexWriter.close();
				} catch (CorruptIndexException e1) {
					e1.printStackTrace();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
		} finally {
			this.optimization = false;

			this.context.notifyMemoryIndexOpt(false);
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
	 * @param dir 索引目录
	 * @param create 是否重建索引
	 * @return IndexWriter
	 */
	@SuppressWarnings("deprecation")
	private IndexWriter openWriter(Directory dir, boolean create)throws CorruptIndexException, LockObtainFailedException,
			IOException {
		IndexWriter indexWriter = new IndexWriter(dir, this.context.getIndexConfig().getLuceneAnalyzer(),
				create, MaxFieldLength.LIMITED);
		//是否将多个segment合并
		indexWriter.setUseCompoundFile(false);
		//设置文档中Field的最大可容纳Term的数目
		indexWriter.setMaxFieldLength(this.context.getIndexConfig().getMaxFieldLength());
		//设置合并参数
		indexWriter.setMergeFactor(this.context.getIndexConfig().getMergeFactor());
		//设置每个index segment的最大文档数目
		indexWriter.setMaxMergeDocs(2048);
		return indexWriter;
	}
}
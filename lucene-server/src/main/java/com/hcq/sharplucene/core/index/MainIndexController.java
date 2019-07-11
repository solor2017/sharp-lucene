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

import com.hcq.sharplucene.core.index.IndexCommand.Operate;
import com.hcq.sharplucene.core.index.IndexCommand.Status;

/**
 * 主索引控制器，磁盘索引 支持三种触发条件 1.队列中，任务数达到触发点（例如：500条）
 * 2.索引间隔时间到达触发点（例如：60秒） 3.手动触发
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
class MainIndexController implements Runnable {
	/**
	 * 控制器名称
	 */
	private String name;
	/**
	 * 索引控制器上下文
	 */
	private IndexContext context;
	/**
	 * 等待索引的文档
	 */
	private IndexCommandQueue commandQueue;
	/**
	 * 待删除
	 */
	private List<IndexCommand> toBeDeleted;
	/**
	 * 待新增
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

	MainIndexController(IndexContext context) {
		this.context = context;
		this.name = (getClass().getSimpleName() + " for " + context
				.getIndexConfig().getIndexName());
		init();
	}

	/**
	 * 初始化索引控制器
	 */
	private void init() {
		this.stopFlag = false;
		this.optimization = false;
		// 初始化指令队列
		this.commandQueue = new IndexCommandQueue(this.context);
		this.toBeDeleted = new LinkedList();
		this.toBeAdded = new LinkedList();
		// 启动执行线程
		new Thread(this, this.name).start();
		System.out.println(this.name + " start.");
	}

	/**
	 * 发送索引变更指令
	 * 
	 * @param command
	 * @param immediately
	 */
	void sendCommand(IndexCommand command, boolean immediately) {
		synchronized (this.commandQueue) {
			// 如果队列里的指令超过了上限，则等待执行
			while (this.commandQueue.size() >= this.context.getIndexConfig()
					.getQueueHoldLimited()) {
				try {
					this.commandQueue.wait();
					if (this.stopFlag) {
						return;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
			// 初始化命令执行状态
			command.setStatus(IndexCommand.Status.TODO);
			this.commandQueue.addCommand(command);
			// 如果队列中等待的值达到临界值or immediately==true,则立即唤醒
			if ((immediately)
					|| (this.commandQueue.size() >= this.context
							.getIndexConfig().getQueueTriggerCritical())) {
				this.commandQueue.notify();
			}
		}
	}

	/**
	 * 停止线程
	 */
	void stopService() {
		this.stopFlag = true;
		this.optimization = false;
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
			// 同步队列
			synchronized (this.commandQueue) {
				while ((!this.stopFlag) && (this.commandQueue.isEmpty())) {
					try {
						this.commandQueue.wait(this.context.getIndexConfig()
								.getQueuePollPeriod());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (this.stopFlag) {
					// 终止当前线程的处理
					return;
				}
				// 取出所有线程
				commands = this.commandQueue.pollALL();
				// 唤醒
				this.commandQueue.notify();
			}

			if (commands == null)
				continue;
			try {
				// 执行操作指令
				processIndexCommands(commands);
			} catch (Exception allEx) {// 捕获所以异常，保证线程的run可以继续循环执行
				allEx.printStackTrace();
			}
		}
	}

	// /**
	// * 执行索引变更指令
	// * @param commands
	// */
	// private void processIndexCommands(IndexCommand[] commands) {
	// for (IndexCommand command : commands) {
	// switch (command.getOperate().ordinal()) {
	// case 2:
	// this.toBeAdded.add(command);
	// break;
	// case 3:
	// this.toBeAdded.add(command);
	// break;
	// case 4:
	// this.toBeDeleted.add(command);
	// this.toBeAdded.add(command);
	// break;
	// case 5:
	// this.toBeDeleted.add(command);
	// break;
	// case 7:
	// this.toBeDeleted.add(command);
	// break;
	// case 6:
	// this.optimization = true;
	// command.setStatus(IndexCommand.Status.DONE);
	// }
	//
	// }
	//
	// Directory dir = null;
	// try {
	// //得到索引目录
	// dir = FSDirectory.open(this.context.getIndexConfig().getMainDirectory());
	// //判断目录是否存在
	// boolean exists = IndexReader.indexExists(dir);
	// //删除任务
	// if ((exists) && (!this.toBeDeleted.isEmpty())) {
	// removeIndex(this.toBeDeleted, dir);
	// }
	// //新增任务
	// if (!this.toBeAdded.isEmpty()) {
	// addIndex(this.toBeAdded, dir, !exists);
	// }
	// /**
	// * 将指令发送到备份索引，除CLEAR和OPTIMIZE外，都必须传递到历史索引
	// */
	// if (this.context.getIndexConfig().isEnableBackup()) {
	// for (IndexCommand command : commands) {
	// if ((IndexCommand.Operate.CLEAR == command.getOperate())
	// || (IndexCommand.Operate.OPTIMIZE == command.getOperate())) {
	// continue;
	// }
	// this.context.getBackupIndexController().sendCommand(command, false);
	// }
	//
	// }
	// //如果存在索引优化指令，且指令队列中没有其他指令，则执行优化
	// if ((exists) && (this.optimization)&& (this.commandQueue.isEmpty()))
	// optimizeIndex(dir);
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// if (dir != null) {
	// try {
	// dir.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// //清空索引任务队列
	// this.toBeDeleted.clear();
	// this.toBeAdded.clear();
	// }
	// }
	private void processIndexCommands(IndexCommand[] commands) {
		/*
		 * * 分离指令，构造删除、新增两个指令队列 * 对于MainIndexController, 要负责处理BLD , ADD , MOD ,
		 * DEL , CLR , OPT
		 */
		for (IndexCommand command : commands) {
			switch (command.getOperate()) {
			case BUILD:
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
			case CLEAR:
				this.toBeDeleted.add(command);
				break;
			case OPTIMIZE:
				this.optimization = true;
				command.setStatus(IndexCommand.Status.DONE);
				break;
			}
		}
		// 变更索引
		Directory dir = null;
		try {
			// 获取索引目录
			dir = FSDirectory.open(this.context.getIndexConfig()
					.getMainDirectory());
			// 判断索引是否已经存在
			boolean exists = IndexReader.indexExists(dir);
			// 执行删除索引任务
			if (exists && !toBeDeleted.isEmpty()) {
				this.removeIndex(toBeDeleted, dir);
			}
			// 执行新增索引任务
			if (!toBeAdded.isEmpty()) {
				this.addIndex(toBeAdded, dir, !exists);
			}
			/*
			 * 
			 * * 将指令发送到备份索引 主索引的指令除CLEAR，OPTIMIZE外，都必须传递到历史索引 *
			 * 在OPTIMIZE执行前完成向备份索引的指令传递
			 */
			if (this.context.getIndexConfig().isEnableBackup()) {
				for (IndexCommand command : commands) {
					if (Operate.CLEAR == command.getOperate()
							|| Operate.OPTIMIZE == command.getOperate()) {
						continue;
					}
					this.context.getBackupIndexController().sendCommand(
							command, false);
				}
			}
			// 如果存在索引优化指令，且指令队列中没有其他指令，则执行优化
			if (exists && this.optimization && this.commandQueue.isEmpty()) {
				this.optimizeIndex(dir);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭当前索引目录
			if (dir != null) {
				try {
					dir.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 清空索引任务队列
			this.toBeDeleted.clear();
			this.toBeAdded.clear();
		}
	}

	/**
	 * 执行删除索引操作
	 * 
	 * @param commands
	 * @param dir
	 */
	// private void removeIndex(List<IndexCommand> commands, Directory dir) {
	// IndexReader indexReader = null;
	// try {
	// indexReader = IndexReader.open(dir, false);
	//
	// for (IndexCommand command : commands) {
	// Term keyTerm = this.context.keyTerm(command.getDocument());
	// //查找索引中pkey对应的文档
	// TermDocs termDocs = indexReader.termDocs(keyTerm);
	//
	// if (!termDocs.next())
	// continue;
	// //删除pkey对应的文档
	// indexReader.deleteDocument(termDocs.doc());
	// //变更command状态
	// switch (command.getOperate().ordinal()) {
	// case 4:
	// command.setStatus(IndexCommand.Status.DELETED);
	// break;
	// case 5:
	// command.setStatus(IndexCommand.Status.DONE);
	// break;
	// case 7:
	// command.setStatus(IndexCommand.Status.DONE);
	// case 6:
	// }
	// }
	//
	// indexReader.flush();
	// } catch (IOException e) {
	// e.printStackTrace();
	//
	// if (indexReader != null)
	// try {
	// indexReader.close();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// } finally {
	// if (indexReader != null)
	// try {
	// indexReader.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
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
				 * * PKey是唯一的，则该termDocs.next()只执行一次
				 */
				if (termDocs.next()) {
					// 删除PKey对应的文档
					indexReader.deleteDocument(termDocs.doc());
					// 变更command操作状态
					switch (command.getOperate()) {
					case MODIFY:
						command.setStatus(Status.DELETED);
						break;
					case DELETE:
						command.setStatus(Status.DONE);
						break;
					case CLEAR:
						command.setStatus(Status.DONE);
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

	/**
	 * 优化索引操作
	 * @param dir
	 */
	private void optimizeIndex(Directory dir) {
		IndexWriter indexWriter = null;
		try {
			indexWriter = openWriter(dir, false);
			long begin = System.currentTimeMillis();
			System.out.println(this.name + " optimization beign at "
					+ new Date(begin));
			// 通知context，主索引开始优化
			this.context.notifyMainIndexOpt(true);
			indexWriter.optimize();
			System.out.println(this.name + " optimization end at "
					+ new Date(begin) + " cost "
					+ (System.currentTimeMillis() - begin) + " ms.");
		} catch (IOException e) {
			e.printStackTrace();

			this.optimization = false;

			this.context.notifyMainIndexOpt(false);
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
			// 通知context，主索引优化结束
			this.context.notifyMainIndexOpt(false);
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
	 * 新增索引
	 * @param commands
	 * @param dir
	 * @param create
	 */
	private void addIndex(List<IndexCommand> commands, Directory dir,
			boolean create) {
		// 构造IndexWriter
		IndexWriter indexWriter = null;
		try {
			indexWriter = this.openWriter(dir, create);
			// 批量添加文档
			for (IndexCommand command : commands) {
				Document doc = command.getDocument();
				switch (command.getOperate()) {
				case BUILD:
					indexWriter.addDocument(doc);
					// 变更command操作状态
					command.setStatus(Status.DONE);
					break;
				case ADD:
					indexWriter.addDocument(doc);
					// 变更command操作状态
					command.setStatus(Status.DONE);
					// 删除内存索引中的文档
					IndexCommand clearCommand = new IndexCommand(Operate.CLEAR,
							doc);
					this.context.getMemoryIndexController().sendClear(
							clearCommand);
					break;
				case MODIFY:
					/*
					 * 如果指令时修改文档，在新增文档前，需要判断 * IndexCommand.OPSTATUS_DELETED==
					 * command.getOpStatus() 查看文档是否在实时索引中且已经被删除 *
					 * 如果IndexCommand.OPSTATUS_DELETED != command.getOpStatus()
					 * * 说明文档不在索引中，则不能新增
					 */
					if (Status.DELETED == command.getStatus()) {
						indexWriter.addDocument(doc);
						// 变更command操作状态
						command.setStatus(Status.DONE);
					}
					break;
				}
			}
			// 提交事务
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
		} finally {
			if (indexWriter != null) {
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

	// private void addIndex(List<IndexCommand> commands, Directory dir,boolean
	// create) {
	// IndexWriter indexWriter = null;
	// try {
	// indexWriter = openWriter(dir, create);
	//
	// for (IndexCommand command : commands) {
	// Document doc = command.getDocument();
	// switch (command.getOperate().ordinal()) {
	// case 2:
	// indexWriter.addDocument(doc);
	//
	// command.setStatus(IndexCommand.Status.DONE);
	// break;
	// case 3:
	// indexWriter.addDocument(doc);
	//
	// command.setStatus(IndexCommand.Status.DONE);
	//
	// IndexCommand clearCommand = new IndexCommand(IndexCommand.Operate.CLEAR,
	// doc);
	// this.context.getMemoryIndexController().sendClear(clearCommand);
	// break;
	// case 4:
	// if (IndexCommand.Status.DELETED != command.getStatus())
	// continue;
	// indexWriter.addDocument(doc);
	//
	// command.setStatus(IndexCommand.Status.DONE);
	// }
	//
	// }
	//
	// indexWriter.commit();
	// } catch (IOException e) {
	// e.printStackTrace();
	// if (indexWriter != null) {
	// try {
	// indexWriter.rollback();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// }
	//
	// if (indexWriter != null)
	// try {
	// indexWriter.close();
	// } catch (CorruptIndexException e1) {
	// e1.printStackTrace();
	// } catch (IOException e2) {
	// e2.printStackTrace();
	// }
	// } finally {
	// if (indexWriter != null)
	// try {
	// indexWriter.close();
	// } catch (CorruptIndexException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }

	/**
	 * 打开内存索引的写入器
	 * @param dir 索引目录
	 * @param create 是否重建索引
	 * @return IndexWriter
	 */
	@SuppressWarnings("deprecation")
	private IndexWriter openWriter(Directory dir, boolean create)throws CorruptIndexException,
					LockObtainFailedException,IOException {
		IndexWriter indexWriter = new IndexWriter(dir, this.context.getIndexConfig().getLuceneAnalyzer(), create,
				MaxFieldLength.LIMITED);
		//是否将多个segment合并
		indexWriter.setUseCompoundFile(false);
		//设置文档中Field的最大可容纳Term的数目
		indexWriter.setMaxFieldLength(this.context.getIndexConfig().getMaxFieldLength());
		//设置索引时，内存的最大缓冲文档数目(关闭内存容量Buffer参数)
		indexWriter.setMaxBufferedDocs(this.context.getIndexConfig().getBufferedDocs());
		indexWriter.setRAMBufferSizeMB(-1.0D);
		//设置合并参数
		indexWriter.setMergeFactor(this.context.getIndexConfig().getMergeFactor());
		//设置每个index segment的最大文档数目
		indexWriter.setMaxMergeDocs(this.context.getIndexConfig().getMaxMergeDocs());
		return indexWriter;
	}
}
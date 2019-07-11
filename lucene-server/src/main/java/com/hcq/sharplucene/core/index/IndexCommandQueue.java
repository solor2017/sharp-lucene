package com.hcq.sharplucene.core.index;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.lucene.index.Term;
/**
 * 索引操作指令队列，创建索引线程冲中取等待指令
 * 对同意document的操作指令在后台合并
 * @Author: solor
 * @Since: 1.0
 * @Description:
 */
class IndexCommandQueue {
	/**
	 * innerMap<KEY,VALUE>
	 * 对ADD ， MOD ，DEL 指令的映射表
	 * Key ：  Document 的主键（可能没有）
	 * Value ： 包含有对应Document的IndexTask
	 * 存放添加索引的指令
	 * 一条IndexCommand包含指令和要操作的文档 ，
	 * inerMap,存放操作缓存，对已操作过的文档（对单词的更新，记录被更新过的词语），innerQueue：缓存队列
	 * result：true 接受指令 false:拒绝指令
	 */
	private Map<Term, IndexCommand> innerMap;
	/**
	 * 索引变更任务列表
	 */
	private LinkedList<IndexCommand> innerQueue;
	/**
	 * 索引上下文
	 */
	private IndexContext context;

	IndexCommandQueue(IndexContext context) {
		this.context = context;
		this.innerMap = new HashMap();
		this.innerQueue = new LinkedList();
	}
	/**          
	 * 添加索引指令          
	 * 线程同步操作          
	 * @param indexCommand          
	 * @return true ： 接收新任务 ； false 拒绝新任务         
	 *  */         
	public synchronized boolean addCommand(IndexCommand indexCommand){                 
		boolean result = false;                 
		if(indexCommand == null){                         
			return result;                 
			}                          
		if(IndexCommand.Operate.BUILD == indexCommand.getOperate()                                  
				|| IndexCommand.Operate.CLEAR == indexCommand.getOperate()                                 
				|| IndexCommand.Operate.OPTIMIZE == indexCommand.getOperate()                                 
				){                         
			//直接向队列添加指令                         
			this.innerQueue.add(indexCommand);                  
			}else if(indexCommand.getDocument() != null){                         
				//一下是对ADD ，MOD ，DEL指令的状态机处理                                                  
				//0.从Docment中获取主键值                         
				Term keyTerm = this.context.keyTerm(indexCommand.getDocument());                                                  
				//1.判断队列中是否已存在对同一Document的变更任务，                         
				IndexCommand taskInMap = this.innerMap.get(keyTerm);                                             
				if(taskInMap == null){                                 
					//1-1如果不存在，则将任务插入队列                                 
					this.innerMap.put(keyTerm, indexCommand);                                 
					this.innerQueue.add(indexCommand);                                                          
					}else{                                 
						//1-2如果存在需要进行任务状态变更                         
						                                                                  
						IndexCommand.Operate oldIst = taskInMap.getOperate();                                 
						IndexCommand.Operate newIst = indexCommand.getOperate();                                 
						if(IndexCommand.Operate.ADD == oldIst){                                         
							switch(newIst){                                         
							case ADD :                                                 
								//忽略后来的任务                                                 
								break;                                         
								case MODIFY :                                                 
									//更新任务中DOC内容                                                 
									taskInMap.setDocument(indexCommand.getDocument());                                                 
									result = true;                                                 
									break;                                         
									case DELETE :                                                 
										//更改指令为无效操作指令，等效于清空当前Task，这样处理的目的是避免LinkList的Remove操作耗时                                                 
										taskInMap.setOperate(IndexCommand.Operate.NULL);                                                 
										result = true;                                                 
										break;                                                                           
										}                                                                          
							}else if(IndexCommand.Operate.MODIFY == oldIst){                                                                              
								switch(newIst){                                         
								case ADD :                                                 
									//忽略后来的任务                                                
									break;                                         
									case MODIFY :                                                 
										//更新任务中DOC内容                                                 
										taskInMap.setDocument(indexCommand.getDocument());                                                 
										result = true;                                                 
										break;                                         
										case DELETE :                                                 
											//更改指令为删除指令                                                 
											taskInMap.setOperate(IndexCommand.Operate.DELETE);                                                 
											//更新任务中DOC内容                                                 
											//taskInMap.setDocument(indexTask.getDocument());                                                 
											result = true;                                                 
											break;                                                                           
											}                                                                          
								}else if(IndexCommand.Operate.DELETE == oldIst){                                         
									switch(newIst){                                         
									case ADD :                                                 
										//更改指令为修改指令                                                 
										taskInMap.setOperate(IndexCommand.Operate.MODIFY);                                                 
										//更新任务中DOC内容                                                 
										taskInMap.setDocument(indexCommand.getDocument());                                                 
										result = true;                                                 
										break;                                         
										case MODIFY :                                                 
											//忽略后来的任务                                                 
											break;                                         
											case DELETE :                                                 
												//忽略后来的任务                                                 
											break;                                                                           
											}                                                                        
									}                                                        
						}                                
				}                 
		return result;         
		} 
 
// /**
//  * 添加索引的操作指令，线程同步操作。
//  * result true:接受指令 false:拒绝指令
//  * @param indexCommand
//  * @return
//  */
//	public synchronized boolean addCommand(IndexCommand indexCommand) {
//		boolean result = false;
//		if (indexCommand == null) {
//			return result;
//		}
//		//如果存在指令，放入队列
//		if ((IndexCommand.Operate.BUILD == indexCommand.getOperate())
//				|| (IndexCommand.Operate.CLEAR == indexCommand.getOperate())
//				|| (IndexCommand.Operate.OPTIMIZE == indexCommand.getOperate())) {
//			this.innerQueue.add(indexCommand);//直接向队列添加指令
//			
//		} else if (indexCommand.getDocument() != null) {
//			//以下是对ADD ，MODIFY ，DELETE指令的状态机制处理
//			Term keyTerm = this.context.keyTerm(indexCommand.getDocument());//从document获取主键值
//			//1.判断队列中是否已存在对同一Document的变更任务，
//			//innerMap 存放数据格式，key--操作过的文档  value-- 该文档的操作指令
//			IndexCommand taskInMap = (IndexCommand) this.innerMap.get(keyTerm);//通过文档找相应存在该文档的操作指令
//			if (taskInMap == null) {
//				//1-1如果不存在，则将任务插入队列
//				this.innerMap.put(keyTerm, indexCommand);//放入文档和操作命令
//				this.innerQueue.add(indexCommand);//放入操作命令
//			} else {
//				//如果存在需要进行任务状态变更
//				IndexCommand.Operate oldIst = taskInMap.getOperate();
//				IndexCommand.Operate newIst = indexCommand.getOperate();
//				if (IndexCommand.Operate.ADD == oldIst)
//					switch (newIst) {
//					case CLEAR:
//						break;
//					case DELETE://删除
//						taskInMap.setDocument(indexCommand.getDocument());
//						result = true;
//						break;
//					case MODIFY://更新
//						//更新任务中DOC内容
//						//更改指令为无效操作指令，等效于清空当前Task，这样处理的目的是避免LinkList的Remove操作耗时
//						taskInMap.setOperate(IndexCommand.Operate.NULL);
//						result = true;
//					default:
//						break;
//					}
//				else if (IndexCommand.Operate.MODIFY == oldIst)
//					switch (newIst) {
//					case CLEAR:
//						break;
//					case DELETE:
//						taskInMap.setDocument(indexCommand.getDocument());
//						result = true;
//						break;
//					case MODIFY:
//						taskInMap.setOperate(IndexCommand.Operate.DELETE);
//
//						result = true;
//					default:
//						break;
//					}
//				else if (IndexCommand.Operate.DELETE == oldIst) {
//					switch (newIst) {
//					case CLEAR:
//						taskInMap.setOperate(IndexCommand.Operate.MODIFY);
//
//						taskInMap.setDocument(indexCommand.getDocument());
//						result = true;
//						break;
//					case DELETE:
//						break;
//					case MODIFY:
//					}
//				}
//			}
//
//		}
//
//		return result;
//	}
	/**
	 * 取出队列中的第一条指令
	 * @return 第一条指令
	 */
	public synchronized IndexCommand pollFirst() {
		IndexCommand first = (IndexCommand) this.innerQueue.pollFirst();
		if ((first != null)
				&& ((IndexCommand.Operate.ADD == first.getOperate())
						|| (IndexCommand.Operate.MODIFY == first.getOperate())
						|| (IndexCommand.Operate.DELETE == first.getOperate()))) {
			Term firstKeyTerm = this.context.keyTerm(first.getDocument());
			this.innerMap.remove(firstKeyTerm);
		}

		return first;
	}
	/**
	 * 取出最后一条指令
	 * @return 最后一条指令
	 */
	public synchronized IndexCommand pollLast() {
		IndexCommand last = (IndexCommand) this.innerQueue.pollLast();
		if ((last != null)
				&& ((IndexCommand.Operate.ADD == last.getOperate())
						|| (IndexCommand.Operate.MODIFY == last.getOperate()) 
						|| (IndexCommand.Operate.DELETE == last.getOperate()))) {
			Term lastKeyTerm = this.context.keyTerm(last.getDocument());
			this.innerMap.remove(lastKeyTerm);
		}

		return last;
	}
	/**
	 * 取出所有指令，并清空队列
	 * @return IndexCommand[]
	 */
	public synchronized IndexCommand[] pollALL() {
		IndexCommand[] tasks = new IndexCommand[size()];
		tasks = (IndexCommand[]) this.innerQueue.toArray(tasks);
		clear();
		return tasks;
	}
	/**
	 * 清空队列
	 */
	public synchronized void clear() {
		this.innerQueue.clear();
		this.innerMap.clear();
	}
	/**
	 * 得到队列大小
	 * @return
	 */
	public int size() {
		return this.innerQueue.size();
	}

	public boolean isEmpty() {
		return this.innerQueue.isEmpty();
	}
}
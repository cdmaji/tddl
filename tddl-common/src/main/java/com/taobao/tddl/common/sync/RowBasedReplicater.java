/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.sync.BucketSwitcher.BucketTaker;
import com.taobao.tddl.common.util.TDDLMBeanServer;
import com.taobao.tddl.interact.rule.bean.SqlType;

/**
 * TODO ɾ����־ʱ������־�⡢��־���������飬ÿ��ÿ��һ��BucketSwitcherִ�С�
 * 
 * @author linxuan
 *
 */
public class RowBasedReplicater implements ReplicationTaskListener, RowBasedReplicaterMBean {
	private static final Log logger = LogFactory.getLog(RowBasedReplicater.class);
	private static final int DEFAULT_THREAD_POOL_SIZE = 16;
	private static final int DEFAULT_WORK_QUEUE_SIZE = 4096;
	public static final int DEFAULT_BATCH_DELETE_SIZE = 1280;
	public static final int DEFAULT_BATCH_UPDATE_SIZE = 512;

	/**
	 * ����μ�SyncServer����ͬ�ֶ�
	 */
	//private long temporaryExtraPlusTime;
	/**
	 * �Ƿ�ָ�next_sync_time, ��temporaryExtraPlusTime���ý�С�����´δ���ɽ��ܵķ�Χ��ʱ��
	 * ������Ϊ�������Ϊfalse���Խ�ʡ������־�ĳɱ�
	 */
	//private boolean isRevertNextSyncTime = false;
	private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
	private int workQueueSize = DEFAULT_WORK_QUEUE_SIZE;

	private ThreadPoolExecutor replicationExecutor; //���ݸ����̳߳�
	protected ThreadPoolExecutor deleteSyncLogExecutor; //ɾ����־�̳߳�
	protected ThreadPoolExecutor updateSyncLogExecutor; //������־�̳߳�
	private NoStrictBucketSwitcher<RowBasedReplicationContext> deleteBucketSwitcher;
	private NoStrictBucketSwitcher<RowBasedReplicationContext> updateBucketSwitcher;

	public RowBasedReplicater() {

	}

	public void init() {
		/**
		 * �����̳߳أ�CallerRunsPolicy: ��������execute����
		 */
		replicationExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(workQueueSize), new ThreadPoolExecutor.CallerRunsPolicy());

		/**
		 * ɾ���͸����̳߳أ�����Log��DiscardPolicy
		 */
		deleteSyncLogExecutor = new ThreadPoolExecutor(1, 2, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(10), new RejectedExecutionHandler() {
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						logger.warn("A DeleteSyncLogTask discarded");
					}
				});
		updateSyncLogExecutor = new ThreadPoolExecutor(1, 2, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(10), new RejectedExecutionHandler() {
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						logger.warn("A UpdateSyncLogTask discarded");
					}
				});

		/**
		 * ɾ���͸�����־�Ļ�����������ˮͰ�л���
		 */
		final BucketTaker<RowBasedReplicationContext> deleteBucketTaker = new BucketTaker<RowBasedReplicationContext>(deleteSyncLogExecutor) {
			@Override
			public Runnable createTakeAwayTask(Collection<RowBasedReplicationContext> list) {
				return new DeleteSyncLogTask(list);
			}

		};
		final BucketTaker<RowBasedReplicationContext> updateBucketTaker = new BucketTaker<RowBasedReplicationContext>(updateSyncLogExecutor) {

			@Override
			public Runnable createTakeAwayTask(Collection<RowBasedReplicationContext> list) {
				return new UpdateSyncLogTask(list);
			}
			
		};
		deleteBucketSwitcher = new NoStrictBucketSwitcher<RowBasedReplicationContext>(deleteBucketTaker,
				DEFAULT_BATCH_DELETE_SIZE);
		updateBucketSwitcher = new NoStrictBucketSwitcher<RowBasedReplicationContext>(updateBucketTaker,
				DEFAULT_BATCH_UPDATE_SIZE);

		TDDLMBeanServer.registerMBean(this, "Replicater"); //ע��JMX
	}

	public static class DeleteSyncLogTask implements Runnable {
		private final Collection<RowBasedReplicationContext> contexts;

		public DeleteSyncLogTask(Collection<RowBasedReplicationContext> contexts) {
			this.contexts = contexts;
		}

		public void run() {
			/**
			 * ��������ɾ����־�߼��ĵط�
			 */
			RowBasedReplicationExecutor.batchDeleteSyncLog(contexts);
		}
	}

	static class UpdateSyncLogTask implements Runnable {
		private final Collection<RowBasedReplicationContext> contexts;

		public UpdateSyncLogTask(Collection<RowBasedReplicationContext> contexts) {
			this.contexts = contexts;
		}

		public void run() {
			/**
			 * �������ø�����־�߼��ĵط�
			 * ������ɺ���û�гɹ�������������next_sync_time
			 */
			//RowBasedReplicationExecutor.batchUpdateSyncLog(contexts, -temporaryExtraPlusTime);
			RowBasedReplicationExecutor.batchUpdateSyncLog(contexts, 0);
		}
	}
	
	public static class InDeleteSyncLogTask implements Runnable {
		private final Collection<RowBasedReplicationContext> contexts;
		private final int onceSize;

		public InDeleteSyncLogTask(Collection<RowBasedReplicationContext> contexts, int size) {
			this.contexts = contexts;
			this.onceSize = size;
		}

		public void run() {
			/**
			 * ��������ɾ����־�߼��ĵط�
			 */
			RowBasedReplicationExecutor.inDeleteSyncLog(contexts, onceSize);
		}
	}

	static class InUpdateSyncLogTask implements Runnable {
		private final Collection<RowBasedReplicationContext> contexts;
		private final int onceSize;

		public InUpdateSyncLogTask(Collection<RowBasedReplicationContext> contexts, int size) {
			this.contexts = contexts;
			this.onceSize = size;
		}

		public void run() {
			/**
			 * �������ø�����־�߼��ĵط�
			 * ������ɺ���û�гɹ�������������next_sync_time
			 */
			//RowBasedReplicationExecutor.batchUpdateSyncLog(contexts, -temporaryExtraPlusTime);
			RowBasedReplicationExecutor.inUpdateSyncLog(contexts, 0, onceSize);
		}
	}


	/**
	 * �����̳߳�ִ�С��̳߳�����ǰ�߳�ִ��(ThreadPoolExecutor.CallerRunsPolicy)
	 * ���뱾������Ϊÿ����������ɵ�ReplicationTaskListener
	 * @see onTaskCompleted
	 */
	/*public void replicate(RowBasedReplicationContext context) {
		replicationExecutor.execute(new RowBasedReplicationTask(context, this));
	}*/

	/**
	 * ���׳��κ��쳣
	 * �����̳߳�ִ�С��̳߳�����ǰ�߳�ִ��(ThreadPoolExecutor.CallerRunsPolicy)
	 * ���뱾������Ϊÿ����������ɵ�ReplicationTaskListener
	 * @see onTaskCompleted
	 */
	public void replicate(Collection<RowBasedReplicationContext> contexts) {
		contexts = mergeAndReduce(contexts);
		long timeused, time0 = System.currentTimeMillis();
		for (RowBasedReplicationContext context : contexts) {
			try {
				//replicater.replicate(context);
				replicationExecutor.execute(new RowBasedReplicationTask(context, this));
			} catch (Throwable t) {
				logger.warn("[SyncServer]replicate failed", t);
			}
		}
		timeused = System.currentTimeMillis() - time0;
		logger.warn(contexts.size() + " replication logs processe tasks accepted, time used:" + timeused);
		Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_ReplicationTasksAccepted, contexts.size(), timeused);
	}

	/**
	 * ��־�ϲ����ԣ������ظ�����־��ֻ����һ��ȥ�������������ֱ��ɾ��
	 * 1. ֻ��update��־���ϲ�
	 * 2. �����߼��������������������ֵ��ͬ�ĸ�����־����Ϊ��ͬһ�����ݲ�������־
	 * 3. ��gmt_create������־Ϊ׼��������־����������������ֱ�ӵ����ɹ�����
	 * 4. failedTargets����Ϊ�ϲ������Ƕ�����ѡһ��������������������ʧ���б�ֻ��ע��������
	 *    ��־�ϲ��Ĵ����ʧ���б�Ĵ�����ȫ������������ϡ�
	 */
	private Collection<RowBasedReplicationContext> mergeAndReduce(Collection<RowBasedReplicationContext> contexts) {
		Map<String, RowBasedReplicationContext> sortMap = new HashMap<String, RowBasedReplicationContext>(contexts.size());
		List<RowBasedReplicationContext> noMergeList = new ArrayList<RowBasedReplicationContext>(contexts.size());
		for (RowBasedReplicationContext context : contexts) {
			if (SqlType.INSERT.equals(context.getSqlType())) {
				noMergeList.add(context); //��insert�����ϲ�
			} else {
				String key = new StringBuilder(context.getMasterLogicTableName()).append("#").append(
						context.getPrimaryKeyValue()).append("#").append(context.getPrimaryKeyColumn()).toString();
				RowBasedReplicationContext last = sortMap.get(key);
				if (last == null) {
					sortMap.put(key, context);
				} else if (context.getCreateTime().equals(last.getCreateTime())) {
					noMergeList.add(context); //����ʱ����ͬ�������ϲ�����ֹ����syncServer����ɾ���Է��ļ�¼
				} else if (context.getCreateTime().after(last.getCreateTime())) {
					sortMap.put(key, context); //�������µ�
				} else {
					logger.warn(new StringBuilder("Dropping a log:id=").append(context.getSyncLogId()).append(
							",LogicTableName=").append(context.getMasterLogicTableName()).append(",").append(
							context.getPrimaryKeyColumn()).append("=").append(context.getPrimaryKeyValue()));
					this.deleteBucketSwitcher.pourin(context);
				}
			}
		}
		noMergeList.addAll(sortMap.values());
		return noMergeList;
	}

	public void onTaskCompleted(RowBasedReplicationContext context, boolean success) {
		if (success) {
			//������Ƴɹ�������deleteBucketSwitcher�ȴ�����ɾ��
			this.deleteBucketSwitcher.pourin(context);
		} else {
			//������Ʋ��ɹ�������updateBucketSwitcher�ȴ���������next_sync_time
			this.updateBucketSwitcher.pourin(context);
		}
	}

	public void destroy() {
		/**
		 * ��˵ĳ��ҵ����JBoss�»�ܹ����һ�������Ⱥ����start��destroy
		 */
		/*if (replicationExecutor != null) {
			replicationExecutor.shutdown();
			try {
				replicationExecutor.awaitTermination(8L, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// ignore
			}
		}*/
	}

	/**
	 * JMX Exporting
	 * executorService.getTaskCount(); //�������+����ִ����+�ȴ�ִ����
	 * executorService.getCompletedTaskCount(); //�������
	 * executorService.getQueue().size(); //�ȴ�ִ����
	 */
	public int getReplicationQueueSize() {
		return replicationExecutor.getQueue().size();
	}

	public int getDeleteSyncLogQueueSize() {
		return deleteSyncLogExecutor.getQueue().size();
	}

	public int getUpdateSyncLogQueueSize() {
		return updateSyncLogExecutor.getQueue().size();
	}

	public long getCompletedReplicationCount() {
		return replicationExecutor.getCompletedTaskCount();
	}

	public long getCompletedDeleteSyncLogCount() {
		return deleteSyncLogExecutor.getCompletedTaskCount();
	}

	public long getCompletedUpdateSyncLogCount() {
		return updateSyncLogExecutor.getCompletedTaskCount();
	}

	public int getDeleteBatchSize() {
		return this.deleteBucketSwitcher.getBucketSize();
	}

	public void setDeleteBatchSize(int bucketSize) {
		this.deleteBucketSwitcher.setBucketSize(bucketSize);
	}

	public int getUpdateBatchSize() {
		return this.updateBucketSwitcher.getBucketSize();
	}

	public void setUpdateBatchSize(int bucketSize) {
		this.updateBucketSwitcher.setBucketSize(bucketSize);
	}

	/**
	 * ���߼���getter/setter
	 */
	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public void setWorkQueueSize(int workQueueSize) {
		this.workQueueSize = workQueueSize;
	}

}

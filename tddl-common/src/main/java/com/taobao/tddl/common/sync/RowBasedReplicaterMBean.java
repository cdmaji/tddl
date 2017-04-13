/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sync;


/**
 * RowBasedReplicater JMX MBean�ӿ�
 * 
 * @author linxuan
 */
public interface RowBasedReplicaterMBean {
	/**
	 * �������еĴ�С
	 */
	int getReplicationQueueSize();

	int getDeleteSyncLogQueueSize();

	int getUpdateSyncLogQueueSize();


	/**
	 * �����̳߳ص����������
	 */
	long getCompletedReplicationCount();

	/**
	 * ɾ����־�̳߳������������ÿ������������ɾ���� 
	 */
	long getCompletedDeleteSyncLogCount();

	/**
	 * ������־�̳߳������������ÿ���������������µ� 
	 */
	long getCompletedUpdateSyncLogCount();

	
	/**
	 * ��̬��غ͵���bucketSize
	 */
	int getDeleteBatchSize();

	void setDeleteBatchSize(int bucketSize);

	int getUpdateBatchSize();

	void setUpdateBatchSize(int bucketSize);
}

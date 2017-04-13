/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sync;

import java.util.Map;

/**
 * ������ݸ��ƣ�ͬ�����ӿڡ��ص��ӿ�
 * 
 * @author linxuan
 * 
 */
public interface SlaveReplicater {
	
	/**
	 * ���������ɹ������
	 * @param masterRow ���������һ�����ݡ�key��������value����ֵ
	 * @param slave ��ӦTDataSource.replicationConfigFileָ��ĸ��������ļ�(����tddl-replication.xml)�е�slaveInfo������Ϣ
	 */
	void insertSlaveRow(Map<String, Object> masterRow, SlaveInfo slave);

	/**
	 * ��������³ɹ������
	 * @param masterRow ���������һ�����ݡ�key��������value����ֵ
	 * @param slave ��ӦTDataSource.replicationConfigFileָ��ĸ��������ļ�(����tddl-replication.xml)�е�slaveInfo������Ϣ
	 */
	void updateSlaveRow(Map<String, Object> masterRow, SlaveInfo slave);
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.config;

/**
 * @author shenxun
 * @author <a href="zylicfc@gmail.com">junyu</a> 
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc ������Ϣ�Ļص��ӿ�
 */
public interface ConfigDataListener {
	/**
	 * �������Ŀͻ����յ�����ʱ����ע��ļ�����������
	 * �����յ������ݴ��ݵ��˷�����
	 * @param dataId         ��������������ע���id
	 * @param data           �ַ�������
	 */
    void onDataRecieved(String dataId,String data);
}

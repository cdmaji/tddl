/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.atom.config;

import com.taobao.tddl.common.config.ConfigDataListener;

/**
 * TAtom����Դȫ�ֺ�Ӧ�õ����ù���ӿڶ���
 * 
 * @author qihao
 *
 */
public interface DbConfManager {
	/**��ȡȫ������
	 * 
	 * @return
	 */
	public String getGlobalDbConf();

	/**��ȡӦ������
	 * 
	 * @return
	 */
	public String getAppDbDbConf();

	/**
	 * ע��ȫ�����ü���
	 * 
	 * @param Listener
	 */
	public void registerGlobaDbConfListener(ConfigDataListener Listener);

	/**ע��Ӧ�����ü���
	 * 
	 * @param Listener
	 */
	public void registerAppDbConfListener(ConfigDataListener Listener);

	/**
	 * ֹͣDbConfManager
	 */
	public void stopDbConfManager();
}

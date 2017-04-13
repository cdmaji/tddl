/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	//Copyrigh(c) Taobao.com
package com.taobao.tddl.common.config;

import java.util.List;import java.util.Map;import java.util.concurrent.Executor;
/**
 * @author <a href="zylicfc@gmail.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc ��ȡ���õĴ�����
 */
public interface ConfigDataHandler {
	public static final String FIRST_SERVER_STRATEGY = "firstServer";
	public static final String FIRST_CACHE_THEN_SERVER_STRATEGY="firstCache";

	/**
	 * DefaultConfigDataHandler���� ʵ���������Handler֮����ô˷��� ����Handler�����Ϣ
	 * @param dataId             ����������ƽ̨��ע���id
	 * @param listenerList       ���ݼ������б�
	 * @param prop               ȫ�����ú�����ʱ
	 */
	void init(String dataId, List<ConfigDataListener> listenerList,
			Map<String, Object> prop);

	/**
	 * ������������ȡ����
	 * @param timeout    ��ȡ������Ϣ��ʱʱ��
	 * @param strategy   ��ȡ���ò���
	 * @return 
	 */
	String getData(long timeout, String strategy);

	/**
	 * Ϊ���͹���������ע�ᴦ��ļ�����
	 * @param configDataListener    ������
	 * @param executor              ִ�е�executor
	 */
	void addListener(ConfigDataListener configDataListener, Executor executor);

	/**
	 * Ϊ���͹���������ע�������������
	 * @param configDataListenerList  �������б�
	 * @param executor                ִ�е�executor
	 */
	void addListeners(List<ConfigDataListener> configDataListenerList,
			Executor executor);

	/**
	 * ֹͣ�ײ����ù�����
	 */
	void closeUnderManager();
}

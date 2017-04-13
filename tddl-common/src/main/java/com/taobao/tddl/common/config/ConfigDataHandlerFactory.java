/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author whisper
 * @author <a href="zylicfc@gmail.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc �õ���������ô�����ʵ��
 */
public interface ConfigDataHandlerFactory {
	/**
	 * ��ĳһ��dataId���м���
	 * @param dataId   ��������������ע���id
	 * @return         �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandler(String dataId);
	
	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص�������
	 * @param dataId                ������p��ֵ����ע���id
	 * @param configDataListener    ���ݻص�������
	 * @return                      �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandler(String dataId,
			ConfigDataListener configDataListener);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص��������б��������յ�������Ϣʱ
	 * ��������ü������Ļص�����
	 * @param dataId                 ��������������ע���id
	 * @param configDataListenerList ���ݻص��������б�
	 * @return                       �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerWithListenerList(String dataId,
			List<ConfigDataListener> configDataListenerList);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص��������������ṩ�ڲ�һЩ����(���ܱ�handler����)
	 * @param dataId              ��������������ע���id
   	 * @param configDataListener  ���ݻص�������
   	 * @param config              TDDL�ڲ���handler�ṩ��һЩ����
	 * @return                    �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerC(String dataId,
			ConfigDataListener configDataListener,
			Map<String, String> config);

	/**
	 * ��ĳһ��dataId���м���,ʹ�����ṩ�ص��������б������ṩ�ڲ�һЩ����(���ܱ�handler����)
	 * @param dataId                  ��������������ע���id
	 * @param configDataListenerList  ���ݻص��������б�
	 * @param config                  TDDL�ڲ���handler�ṩ��һЩ����
	 * @return                        �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerWithListenerListC(String dataId,
			List<ConfigDataListener> configDataListenerList,
			Map<String, String> config);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص��������������ṩִ���̳߳�
	 * @param dataId                  ��������������ע���id
	 * @param configDataListener      ���ݻص�������
	 * @param executor                ���ݽ��մ����̳߳�
	 * @return                        �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerE(String dataId,
			ConfigDataListener configDataListener, Executor executor);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص��������б������ṩִ���̳߳�
	 * @param dataId                  ��������������ע���id
	 * @param configDataListenerList  ���ݻص��������б�
	 * @param executor                ���ݽ��մ����̳߳�
	 * @return                        �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerWithListenerListE(String dataId,
			List<ConfigDataListener> configDataListenerList, Executor executor);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص���������
	 * �����ṩִ���̳߳غ��ڲ�һЩ����(���ܱ�handler����)
	 * @param dataId                ��������������ע���id
	 * @param configDataListener    ���ݻص�������
	 * @param executor              ���ݽ��մ����̳߳�
	 * @param config                TDDL�ڲ���handler�ṩ��һЩ����
	 * @return                      �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerCE(String dataId,
			ConfigDataListener configDataListener, Executor executor,
			Map<String, String> config);

	/**
	 * ��ĳһ��dataId���м�����ʹ�����ṩ�ص��������б�
	 * �����ṩִ���̳߳غ��ڲ�һЩ����(���ܱ�handler����)
	 * @param dataId                  ��������������ע���id
	 * @param configDataListenerList  ���ݻص��������б�
	 * @param executor                ���ݽ��մ����̳߳�
	 * @param config                  TDDL�ڲ���handler�ṩ��һЩ����
	 * @return                        �����������ݴ�����ʵ��
	 */
	ConfigDataHandler getConfigDataHandlerWithListenerListCE(String dataId,
			List<ConfigDataListener> configDataListenerList, Executor executor,
			Map<String, String> config);
}

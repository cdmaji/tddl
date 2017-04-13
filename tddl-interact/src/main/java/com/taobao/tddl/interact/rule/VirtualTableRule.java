/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.interact.rule.virtualnode.DBTableMap;
import com.taobao.tddl.interact.rule.virtualnode.TableSlotMap;

/**
 * TDataSource������������������ö��������
 * tddl-client���ݽ���/Ԥ�������ȡ���������
 * �����������ȡ�ö�Ӧ��VirtualTableRule����
 * 
 * @author linxuan
 *
 */
public interface VirtualTableRule<D, T> {

	/**
	 * �������
	 */
	List<Rule<String>> getDbShardRules();

	/**
	 * �������
	 */
	List<Rule<String>> getTbShardRules();

	/**
	 * ��������������Ľ����ת��Ϊ���ս��
	 * @param value
	 * @param dynamicExtraContext
	 */
	//String mapDbKey(D value);

	/**
	 * ��������������Ľ����ת��Ϊ���ս��
	 * @param value
	 * @param dynamicExtraContext
	 * @return
	 */
	//String mapTbKey(T value);

	/**
	 * @return ���������û�б����ʱ��Ĭ�ϱ���
	 */
	//String getVirtualTbName();
	
	/**
	 * @return ��������ڵ����������û�п����ʱ��Ĭ�Ͽ���
	 */
	//String getVirtualDbName();

	/**
	 * ���ر�����ʵ�ʶ�Ӧ��ȫ��������˽ṹ
	 * @return key:dbIndex; value:ʵ����������ļ���
	 */
	Map<String, Set<String>> getActualTopology();

	Object getOuterContext();
	
	public TableSlotMap getTableSlotMap();
	
	public DBTableMap getDbTableMap();

	//=========================================================================
	// ������������Եķָ���
	//=========================================================================

	DBType getDbType();

	boolean isAllowReverseOutput();

	boolean isAllowFullTableScan();

	boolean isNeedRowCopy();

	List<String> getUniqueKeys();
	
	public String getTbNamePattern();
}

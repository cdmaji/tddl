/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ŀ�����ݿ����� ������дĿ��ds��id �Լ���ds�з���Ҫ��ı����б�
 * 
 * @author shenxun
 * 
 */
public class TargetDB{
	/**
	 * �������TDatasource�����е�����
	 */
	private String dbIndex;

	/**
	 * ��������µķ��ϲ�ѯ�����ı����б�
	 */
	private Map<String, Field> tableNames;
	/**
	 * ���������sql,���reverseOutput��Ϊfalse,�����ﲻ��Ϊnull. ����Ȼ����Ϊһ��empty list
	 */
	private List<ReverseOutput> outputSQL;

	/**
	 * ���ر����Ľ����
	 * 
	 * @return ��Set if û�б� ���������
	 */
	public Set<String> getTableNames() {
		if (tableNames == null) {
			return null;
		}
		return tableNames.keySet();
	}

	public void setTableNames(Map<String, Field> tableNames) {
		this.tableNames = tableNames;
	}

	public List<ReverseOutput> getOutputSQL() {
		return outputSQL;
	}

	public Map<String, Field> getTableNameMap() {
		return tableNames;
	}

	public void setOutputSQL(List<ReverseOutput> outputSQL) {
		this.outputSQL = outputSQL;
	}

	public void addOneTable(String table) {
		if (tableNames == null) {
			tableNames = new HashMap<String, Field>();
		}
		tableNames.put(table, Field.EMPTY_FIELD);
	}

	public void addOneTable(String table, Field field) {
		if (tableNames == null) {
			tableNames = new HashMap<String, Field>();
		}
		tableNames.put(table, field);
	}

	public void addOneTableWithSameTable(String table, Field field) {
		if (tableNames == null) {
			tableNames = new HashMap<String, Field>();
			tableNames.put(table, field);
		} else {
			Field inField = tableNames.get(table);
			if (inField == null) {
				tableNames.put(table, field);
			} else {
				if (field.sourceKeys != null) {
					for (Map.Entry<String, Set<Object>> entry : field.sourceKeys
							.entrySet()) {
						inField.sourceKeys.get(entry.getKey()).addAll(
								entry.getValue());
					}
				}
			}
		}
	}

	public String getDbIndex() {
		return dbIndex;
	}

	public void setDbIndex(String dbIndex) {
		this.dbIndex = dbIndex;
	}

	@Override
	public String toString() {
		return "TargetDB [dbIndex=" + dbIndex + ", outputSQL=" + outputSQL
				+ ", tableNames=" + tableNames + "]";
	}

}

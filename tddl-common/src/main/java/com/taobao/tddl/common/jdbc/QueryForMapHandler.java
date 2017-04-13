/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.jdbc;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;

/**
 * ��һ����ȡһ������
 * 1. ����ÿ����select *
 * 2. ȡ��SQLType����Ҫ��Ԫ��Ϣ
 * 
 * ����������⣺
 * 1. ����select *
 * 2. ͨ��setObjectʱ��ʽָ��sqlType�Ա���Oracle�����������ִ�мƻ�
 * 
 * @author linxuan
 *
 */
public interface QueryForMapHandler {
	//����������static��
	static class ColumnMetaData {
		public final int sqlType;
		public final int scale;
		public final String className;

		public ColumnMetaData(int sqlType, int scale, String className) {
			this.sqlType = sqlType;
			this.scale = scale;
			this.className = className;
		}
	}

	//����������static��
	static class TableMetaData {
		private final Map<String/*Сд����*/, ColumnMetaData/*�е�sqlType��*/> columnMetaDataMap;
		public final String[] columnNames; //Сд����
		public final ColumnMetaData[] columns; //columnNames��Ӧ��ColumnMetaData
		public final String commaColumnNames; //���ŷָ���Сд����

		public TableMetaData(String[] columnNames, ColumnMetaData[] columns) {
			if (columnNames == null || columnNames.length == 0 || columns == null || columns.length == 0
					|| columnNames.length != columns.length) {
				throw new IllegalArgumentException("columnNames or columns is null or empty or not match");
			}

			//�����Ա�֤���ڲ����������
			this.columnNames = new String[columnNames.length];
			this.columns = new ColumnMetaData[columns.length];
			System.arraycopy(columnNames, 0, this.columnNames, 0, columnNames.length);
			System.arraycopy(columns, 0, this.columns, 0, columns.length);

			StringBuilder sb = new StringBuilder();
			columnMetaDataMap = new HashMap<String, ColumnMetaData>(this.columnNames.length);
			for (int i = 0; i < this.columnNames.length; i++) {
				sb.append(",").append(this.columnNames[i]);
				columnMetaDataMap.put(this.columnNames[i], this.columns[i]);
			}
			this.commaColumnNames = sb.substring(1);
		}

		public ColumnMetaData getColumnMetaData(String columnName) {
			return this.columnMetaDataMap.get(columnName);
		}
	}

	/*
	public static class UseCachedMetaDataSetter implements PreparedStatementSetter {
		private final ColumnMetaData[] columns;
		private final Object[] args;

		public UseCachedMetaDataSetter(ColumnMetaData[] columns, Object[] args) {
			if (args != null && columns == null) {
				throw new IllegalArgumentException("ColumnMetaData is null");
			}
			if (args != null && columns.length != args.length) {
				throw new IllegalArgumentException("Parameters length can't match the cached colums length.");
			}
			this.columns = columns;
			this.args = args;
		}

		public void setValues(PreparedStatement ps) throws SQLException {
			if (args != null) {
				for (int i = 0; i < columns.length; i++) {
					//ps.setObject(parameterIndex, x, targetSqlType, scale);
					if (columns[i] != null) {
						ps.setObject(i + 1, args[i], columns[i].sqlType, columns[i].scale);
					} else {
						ps.setObject(i + 1, args[i]);
					}
				}
			}
		}
	}
	*/

	public static class UseCachedMetaDataSetter extends ArgTypePreparedStatementSetter {
		private static int[] getArgTypes(ColumnMetaData[] columns) {
			int[] argTypes = new int[columns.length];
			for (int i = 0; i < columns.length; i++) {
				argTypes[i] = columns[i] == null ? SqlTypeValue.TYPE_UNKNOWN : columns[i].sqlType;
			}
			return argTypes;
		}

		public UseCachedMetaDataSetter(ColumnMetaData[] columns, Object[] args) {
			super(args, getArgTypes(columns));
		}
	}
	
	
	/**
	 * ��һ����ȡ��һ������
	 * @param tableName ����
	 * @param selectColumns null����select * ��������ָ���ģ�ֻ�ڵ�һ�β�ѯʱʹ�á�֮���õ�һ�β鵽��ʵ������
	 * @param whereSql select xxx from xxx where xxx �д�where��ʼ֮���sql
	 * @param args 
	 * @return
	 */
	Map<String, Object> queryForMap(JdbcTemplate jt, String tableName, String selectColumns, String whereSql,
			Object[] args);

	/**
	 * ��queryForMap֮����á�����ÿ���е������Ͷ�Ӧ��sqlType
	 * @param tableName Сд����
	 * @return ��Ӧtable��TableMetaData
	 */
	TableMetaData getTableMetaData(String tableName);
}

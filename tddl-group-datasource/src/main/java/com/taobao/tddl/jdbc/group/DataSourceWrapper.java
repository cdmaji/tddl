/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.jdbc.group.config.Weight;

/**
 * һ���̰߳�ȫ��DataSource��װ��
 * 
 * DataSource��װ�࣬��Ϊһ��GroupDataSource�ɶ��AtomDataSource��ɣ���ÿ��AtomDataSource���ж�Ӧ�Ķ�дȨ�ص���Ϣ�����Խ�ÿһ��AtomDataSource��װ������---add by mazhidan.pt
 * 
 * @author yangzhu
 * @author linxuan refactor as immutable class; dataSourceIndex extends
 *
 */
public class DataSourceWrapper implements DataSource {
	private final String dataSourceKey;  //���DataSource��Ӧ��dbKey
	private final String weightStr;//Ȩ����Ϣ�ַ���
	private final Weight weight;  //Ȩ����Ϣ
	private final DataSource wrappedDataSource; //����װ��Ŀ��DataSource
	private final DBType dbType;//���ݿ�����
	private final int dataSourceIndex;//DataSourceIndex��ָ���DataSource��Group�е�λ��

	public DataSourceWrapper(String dataSourceKey, String weightStr, DataSource wrappedDataSource, DBType dbType,
			int dataSourceIndex) {
		this.dataSourceKey = dataSourceKey;
		this.weight = new Weight(weightStr);
		this.weightStr = weightStr;
		this.wrappedDataSource = wrappedDataSource;
		this.dbType = dbType;

		this.dataSourceIndex = dataSourceIndex;
	}

	public DataSourceWrapper(String dataSourceKey, String weightStr, DataSource wrappedDataSource, DBType dbType) {
		this(dataSourceKey, weightStr, wrappedDataSource, dbType, -1);
	}

	/**
	 * ��֤��DataSource��·��index��Ϣ�У��Ƿ����ָ����index--add by mazhidan.pt
	 */
	public boolean isMatchDataSourceIndex(int specifiedIndex) {
		if (weight.indexes != null && !weight.indexes.isEmpty()) {
			return weight.indexes.contains(specifiedIndex);
		} else {
			return this.dataSourceIndex == specifiedIndex;
		}
	}

	/**
	 * �Ƿ��ж�Ȩ�ء�r0��Ż�false
	 */
	public boolean hasReadWeight() {
		return weight.r != 0;
	}

	/**
	 * �Ƿ���дȨ�ء�w0��Ż�false
	 */
	public boolean hasWriteWeight() {
		return weight.w != 0;
	}

	public String toString() {
		return new StringBuilder("DataSourceWrapper{dataSourceKey=").append(dataSourceKey).append(", dataSourceIndex=")
				.append(dataSourceIndex).append(",weight=").append(weight).append("}").toString();
	}

	public String getDataSourceKey() {
		return dataSourceKey;
	}

	public String getWeightStr() {
		return weightStr;
	}

	/*public synchronized void setWeightStr(String weightStr) {
		if ((this.weightStr == weightStr) || (this.weightStr != null && this.weightStr.equals(weightStr)))
			return;
		this.weight = new Weight(weightStr);
		this.weightStr = weightStr;
	}*/

	public Weight getWeight() {
		return weight;
	}

	/*public int getDataSourceIndex() {
		return dataSourceIndex;
	}*/

	/*public void setDataSourceIndex(int dataSourceIndex) {
		this.dataSourceIndex = dataSourceIndex;
	}*/

	public DBType getDBType() {
		return dbType;
	}

	public DataSource getWrappedDataSource() {
		return wrappedDataSource;
	}

	//������javax.sql.DataSource��APIʵ��
	////////////////////////////////////////////////////////////////////////////
	public Connection getConnection() throws SQLException {
		return wrappedDataSource.getConnection();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return wrappedDataSource.getConnection(username, password);
	}

	public PrintWriter getLogWriter() throws SQLException {
		return wrappedDataSource.getLogWriter();
	}

	public int getLoginTimeout() throws SQLException {
		return wrappedDataSource.getLoginTimeout();
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		wrappedDataSource.setLogWriter(out);
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		wrappedDataSource.setLoginTimeout(seconds);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.getClass().isAssignableFrom(iface);
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		try {
			return (T) this;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/*
	//Since: 1.6

	//java.sql.Wrapper
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}
	*/
}

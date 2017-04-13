/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.mockdatasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.taobao.tddl.common.exception.runtime.NotSupportException;
import com.taobao.tddl.interact.rule.bean.DBType;

public class MockDataSource implements DataSource, Cloneable {

	private int timeToObtainConnection = 0;
	private int getConnectionInvokingTimes = 0;
	private String name;
	private String dbIndex;
	private boolean isClosed;
	private DBType dbType = DBType.MYSQL;

	public MockDataSource() {
	}

	public MockDataSource(String dbIndex, String name) {
		this.dbIndex = dbIndex;
		this.name = name;
	}

	public static class ExecuteInfo {
		public ExecuteInfo(MockDataSource dataSource, String method, String sql, Object[] args) {
			this.ds = dataSource;
			this.method = method;
			this.sql = sql;
			this.args = args;
		}

		public MockDataSource ds;
		public String method;
		public String sql;
		public Object[] args;

		@Override
		public String toString() {
			return new StringBuilder("ExecuteInfo:{ds:").append(ds).append(",method:").append(method).append(",sql:")
					.append(sql).append(",args:").append(Arrays.toString(args)).append("}").toString();
		}
	}

	public static class QueryResult {
		public QueryResult(Map<String, Integer> columns, List<Object[]> values) {
			this.columns = columns;
			this.rows = values;
		}

		/**
		 * ֻ֧�ַ�һ�����ݣ���������ֻ֧����ֵlong��String,���磺
		 * sku_id:0,item_id:65,seller_id:63
		 * sku_id:0,item_id:65,name:'aaa'
		 */
		public QueryResult(String row) {
			String[] cols = row.split(",");
			this.columns = new HashMap<String, Integer>(cols.length);
			List<Object> colvalues = new ArrayList<Object>(cols.length);
			for (int i = 0; i < cols.length; i++) {
				String col = cols[i];
				String[] nv = col.split("\\:");
				this.columns.put(nv[0], i);
				if (nv[1].startsWith("'") && nv[1].endsWith("'")) {
					colvalues.add(nv[1].substring(1, nv[1].length() - 1));//�ַ���
				} else if (nv[1].endsWith("NULL")) {
					colvalues.add(null);
				} else {
					colvalues.add(Long.parseLong(nv[1]));//����
				}
			}
			this.rows = new ArrayList<Object[]>(1);
			this.rows.add(colvalues.toArray(new Object[colvalues.size()]));
		}

		public final Map<String, Integer> columns;
		public final List<Object[]> rows;
	}

	public void checkState() throws SQLException {
		if (isClosed) {
			throw genFatalSQLException();
		}
	}

	public SQLException genFatalSQLException() throws SQLException {
		if (DBType.MYSQL.equals(dbType)) {
			return new SQLException("dsClosed", "08001");//����MySQLExceptionSorter
		} else if (DBType.ORACLE.equals(dbType)) {
			return new SQLException("dsClosed", "28");//����OracleExceptionSorter //28 session has been killed
		} else {
			throw new RuntimeException("�����µ�dbType������û�и���");
		}
	}

	/**
	 * ���ÿ��ִ�еĽ����Ϣ��ʵ�ʵ�sql������������Դ����
	 */
	private static ThreadLocal<ExecuteInfo> RESULT = new ThreadLocal<ExecuteInfo>(); //TODO ����TRACE����Ҫ����� 
	private static ThreadLocal<List<ExecuteInfo>> TRACE = new ThreadLocal<List<ExecuteInfo>>();
	private static ThreadLocal<List<QueryResult>> PREDATA = new ThreadLocal<List<QueryResult>>();
	private static ThreadLocal<List<Integer>> PREAffectedRow = new ThreadLocal<List<Integer>>();
	/**
	 * map��key��ȡֵ"getConnection"��"prepareStatement"��"executeQuery"��"executeUpdate"��"" ... 
	 */
	private static ThreadLocal<Map<String, List<SQLException>>> PREException = new ThreadLocal<Map<String, List<SQLException>>>(){
		@Override
	    protected Map<String, List<SQLException>> initialValue() {
			Map<String, List<SQLException>> exceptions = new HashMap<String, List<SQLException>>(4);
			exceptions.put(m_getConnection, new ArrayList<SQLException>(0));
			exceptions.put(m_prepareStatement, new ArrayList<SQLException>(0));
			exceptions.put(m_createStatement, new ArrayList<SQLException>(0));
			exceptions.put(m_executeQuery, new ArrayList<SQLException>(0));
			exceptions.put(m_executeUpdate, new ArrayList<SQLException>(0));
			return exceptions;
	    }
	};

	//TODO ������Щ�����ĳ�һ��enum��
	public static final String m_getConnection = "getConnection";
	public static final String m_prepareStatement = "prepareStatement";
	public static final String m_createStatement = "createStatement";
	public static final String m_executeQuery = "executeQuery";
	public static final String m_executeUpdate = "executeUpdate";

	/**
	 * ��Ҫ��ÿ��testcase��afterclass�е����������
	 */
	public static void reset() {
		RESULT.set(null);
		TRACE.set(null);
		PREDATA.set(null);
		PREAffectedRow.set(null);
		for (Map.Entry<String, List<SQLException>> e : PREException.get().entrySet()) {
			e.getValue().clear();
		}
	}

	public static void clearTrace() {
		List<ExecuteInfo> trace = TRACE.get();
		if (trace != null)
			trace.clear();
		PREDATA.set(null);
	}

	public static void showTrace() {
		showTrace("");
	}

	public static void showTrace(String msg) {
		List<ExecuteInfo> trace = TRACE.get();
		if (trace == null) {
			return;
		}
		System.out.println("Invoke trace on MockDataSource:" + msg);
		for (ExecuteInfo info : trace) {
			System.out.println(info.toString());
		}
	}

	public static ExecuteInfo getResult() {
		return RESULT.get();
	}

	public static List<ExecuteInfo> getTrace() {
		return TRACE.get();
	}

	//�Ƿ���ָ����dbIndex��ִ�й�sqlHead��ͷ��sql
	public static boolean hasTrace(String dbIndex, String sqlHead) {
		List<ExecuteInfo> trace = TRACE.get();
		if (trace != null) {
			for (ExecuteInfo info : trace) {
				if (info.sql != null && dbIndex.equals(info.ds.dbIndex) && sqlHead.length() <= info.sql.length()
						&& sqlHead.equalsIgnoreCase(info.sql.substring(0, sqlHead.length()))) {
					return true;
				}
			}
		}
		return false;
	}

	//�Ƿ���ָ����dbIndex��dsName��ִ�й�sqlHead��ͷ��sql
	public static boolean hasTrace(String dbIndex, String dsName, String sqlHead) {
		List<ExecuteInfo> trace = TRACE.get();
		if (trace != null) {
			for (ExecuteInfo info : trace) {
				if (info.sql != null && dbIndex.equals(info.ds.dbIndex) && info.ds.name.equals(dsName)
						&& sqlHead.length() <= info.sql.length()
						&& sqlHead.equalsIgnoreCase(info.sql.substring(0, sqlHead.length()))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasMethod(String dbIndex, String method) {
		List<ExecuteInfo> trace = TRACE.get();
		if (trace != null) {
			for (ExecuteInfo info : trace) {
				if (dbIndex.equals(info.ds.dbIndex) && method.equals(info.method)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasMethod(String dbIndex, String dsName, String method) {
		List<ExecuteInfo> trace = TRACE.get();
		if (trace != null) {
			for (ExecuteInfo info : trace) {
				if (dbIndex.equals(info.ds.dbIndex) && method.equals(info.method) && info.ds.name.equals(dsName)) {
					return true;
				}
			}
		}
		return false;
	}

	/*public static List<QueryResult> getPreData(){
		return PREDATA.get();
	}*/
	/**
	 * ��¼һ��Datasource��Connection��Statement�ϵ�ִ�ж���
	 */
	public static void record(ExecuteInfo info) {
		RESULT.set(info);
		if (TRACE.get() == null) {
			TRACE.set(new ArrayList<ExecuteInfo>());
		}
		TRACE.get().add(info);
	}

	/**
	 * ����һ��Ԥ�õĲ�ѯ���������ResultSetʱ���ᰴ˳����ȡԤ�õ����ݹ���ResultSet
	 * @param arow ��ʽ��sku_id:0,item_id:65,seller_id:63,name:'����'
	 */
	public static void addPreData(String arow) {
		addPreData(new QueryResult(arow));
	}

	public static void addPreData(QueryResult queryResult) {
		if (PREDATA.get() == null) {
			PREDATA.set(new ArrayList<QueryResult>(5));
		}
		PREDATA.get().add(queryResult);
	}

	public static void addPreAffectedRow(int preAffectedRow) {
		if (PREAffectedRow.get() == null) {
			PREAffectedRow.set(new ArrayList<Integer>(1));
		}
		PREAffectedRow.get().add(preAffectedRow);
	}

	/**
	 * ��Ȩ�ޣ�����ResultSetʱ�������������ȡԤ������
	 */
	static QueryResult popPreData() {
		List<QueryResult> preData = PREDATA.get();
		if (preData == null || preData.isEmpty()) {
			return null; //û��Ԥ������Ҳ�ǿ��Ե�,������ЩTest��care��������
		}
		return PREDATA.get().remove(0);
	}

	/**
	 * ��Ȩ�ޣ�����ResultSetʱ�������������ȡԤ������
	 */
	static int popPreAffectedRow() {
		List<Integer> preAffectedRow = PREAffectedRow.get();
		if (preAffectedRow == null || preAffectedRow.isEmpty()) {
			return 1; //û��Ԥ��AffectedRowĬ�Ϸ���1
		}
		return PREAffectedRow.get().remove(0);
	}

	public static void addPreException(String key, SQLException e) {
		PREException.get().get(key).add(e);
	}

	public static SQLException popPreException(String key) {
		List<SQLException> pre = PREException.get().get(key);
		return pre.size() == 0 ? null : pre.remove(0);
	}

	/**==============================================================
	 * ����Ϊjdbc�ӿ�ʵ��
	 * ==============================================================*/

	public Connection getConnection() throws SQLException {
		try {
			Thread.sleep(timeToObtainConnection);
		} catch (Exception e) {
		}
		getConnectionInvokingTimes++;
		return new MockConnection(m_getConnection, this);
	}

	public Connection getConnection(String username, String password) throws SQLException {
		try {
			Thread.sleep(timeToObtainConnection);
		} catch (Exception e) {
		}
		getConnectionInvokingTimes++;
		return new MockConnection("getConnection#username_password", this);
	}

	public PrintWriter getLogWriter() throws SQLException {
		throw new NotSupportException("");
	}

	public int getLoginTimeout() throws SQLException {
		throw new NotSupportException("");
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new NotSupportException("");
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		throw new NotSupportException("");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDbIndex() {
		return dbIndex;
	}

	public void setDbIndex(String dbIndex) {
		this.dbIndex = dbIndex;
	}

	@Override
	public MockDataSource clone() throws CloneNotSupportedException {
		return (MockDataSource) super.clone();
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	@Override
	public String toString() {
		return new StringBuilder(super.toString().substring(getClass().getPackage().getName().length() + 1)).append(
				"{dbIndex:").append(dbIndex).append(",name:").append(name).append("}").toString();
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.jdbc.group.dbselector.DBSelector;
import com.taobao.tddl.jdbc.group.dbselector.DBSelector.AbstractDataSourceTryer;
import com.taobao.tddl.jdbc.group.dbselector.DBSelector.DataSourceTryer;
import com.taobao.tddl.jdbc.group.util.ExceptionUtils;
import com.taobao.tddl.jdbc.group.util.GroupHintParser;

/**
 *��ص�JDBC�淶��
 * 
 * 1. Connection�رգ������ϴ򿪵�statement�Զ��رա����Ҫ��Connection�������ϴ򿪵�����statement������
 * 2. 
 * 
 *���Եĳ���1���ڵ�һ��statement��ִ�в�ѯ��·�ɵ�db1�ɹ����ٴ���һ��statement��ѯ��db1��ʧ�ܣ�
 * stmt1 = TGroupConnection.createStatement
 * rs1 = stmt1.executeQuery --create connection on db1 and execute success
 * stmt2 = conn..createStatement
 * rs2 = stmt2..executeQuery --db1 failed then...
 * ��ʱ������Ե�db2�⣬db1��connectionҪ��Ҫ�أ�
 * a������أ����ϵ�ʵ��stmt��rs�Ͷ���ص�������db2�ɹ���
 *    �û��ῴ����exception�����û���˵��stms1��rs1���������ġ���ʵ�����Ѿ��ǻ������ˡ�
 * b: ������أ�Ҳ����TGroupConnection���ж��baseConnection��������
 * 
 * �����ϳ����Ŀ��ǣ�������һ��ԭ��
 * 
 *���Ե�ԭ��
 * һ��TGroupConnection�У�ֻ�ڵ�һ�������������ݿ⽻��ʱ��Ҳ���ǲ��ò�����db������û�ʱ������DBGroup�Ͻ������ԡ�
 * һ����ĳ���������Գɹ������������TGroupConnection��ִ�е����в�������ֻ��������ϣ��������ԣ�����ֱ���׳��쳣��
 * ��һ�ν����������ӵ����Թ����У�baseConnection�п��ܻᷢ���仯���滻��һ�����Գɹ���baseConnection�򱣳ֲ��ٸı䡣
 * �������Լ򻯺ܶ����飬��ͬʱ����Թ�����ɱ���Ӱ�졣ͬʱ�����˶�״̬�����������ܻ���û���ɵĹ�������
 * 
 * @author linxuan
 * @author yangzhu
 *
 */
public class TGroupConnection implements Connection {
	private static final Log log = LogFactory.getLog(TGroupConnection.class);

	private TGroupDataSource tGroupDataSource;

	// ��ȻDataSource.getConnection(String username, String password)�����ã�
	// ��Ϊ�˾�����ѭjdbc�淶�����Ǳ����á�
	private String username;
	private String password;

	public TGroupConnection(TGroupDataSource tGroupDataSource) {
		this.tGroupDataSource = tGroupDataSource;
	}

	public TGroupConnection(TGroupDataSource tGroupDataSource, String username, String password) {
		this(tGroupDataSource);
		this.username = username;
		this.password = password;
	}

	/* ========================================================================
	 * �²�connection�ĳ��У�getter/setter��Ȩ��
	 * ======================================================================*/
	private Connection rBaseConnection;
	private Connection wBaseConnection;
	//private String rBaseDsKey; // rBaseConnection��Ӧ������Դkey
	//private String wBaseDsKey; // wBaseConnection��Ӧ������Դkey
	//private int rBaseDataSourceIndex = -2; // rBaseConnection��Ӧ������ԴIndex
	//private int wBaseDataSourceIndex = -2; // wBaseConnection��Ӧ������ԴIndex
	private DataSourceWrapper rBaseDsWrapper;
	private DataSourceWrapper wBaseDsWrapper;

	Connection getBaseConnection(String sql,boolean isRead) throws SQLException {
		int dataSourceIndex=DBSelector.NOT_EXIST_USER_SPECIFIED_INDEX;
		if(sql==null){
			//�����ǰ������Դ��������һ�ε�����Դ������һ����˵����һ�λ����Connection�Ѿ������ˣ���Ҫ�رպ��ؽ���
		    dataSourceIndex = ThreadLocalDataSourceIndex.getIndex();
		}else{
			dataSourceIndex=GroupHintParser.convertHint2Index(sql);
			if(dataSourceIndex<0){
				dataSourceIndex = ThreadLocalDataSourceIndex.getIndex();
			}
		}

		if (dataSourceIndex != DBSelector.NOT_EXIST_USER_SPECIFIED_INDEX) {
			if (log.isDebugEnabled()) {
				log.debug("dataSourceIndex=" + dataSourceIndex);
			}
			//������״̬�£����ò�ͬ������Դ�����ᵼ���쳣��
			if (!isAutoCommit)
			{
			    if (wBaseDsWrapper != null && !wBaseDsWrapper.isMatchDataSourceIndex(dataSourceIndex))
			        throw new SQLException("Transaction in another dataSourceIndex: " + dataSourceIndex);
			}
			if (isRead) {
				if (rBaseDsWrapper != null && !rBaseDsWrapper.isMatchDataSourceIndex(dataSourceIndex))
					closeReadConnection();
			} else {
				if (wBaseDsWrapper != null && !wBaseDsWrapper.isMatchDataSourceIndex(dataSourceIndex))
					closeWriteConnection();
			}
		}

        //Ϊ�˱�֤������ȷ�رգ�������״̬��ֻ��ȡ��д����
		if (isRead && isAutoCommit) {
			//ֻҪ��д���ӣ����Ҷ�Ӧ�Ŀ�ɶ������á����򷵻ض�����
			return wBaseConnection != null && wBaseDsWrapper.hasReadWeight() ? wBaseConnection : rBaseConnection;
			//��д���������д���Ӷ���rBaseConnection��Ȼ��null
		} else {
			if (wBaseConnection != null){
				this.tGroupDataSource.setWriteTarget(wBaseDsWrapper);
				return wBaseConnection;
			}
			else if (rBaseConnection != null && rBaseDsWrapper.hasWriteWeight()) {
				//��д����null������£�����������Ѿ��������Ҷ�Ӧ�Ŀ��д������
				wBaseConnection = rBaseConnection; //wBaseConnection��ֵ����ȷ�������ܹ���ȷ�ύ�ع�
				//��д��������������
				if (wBaseConnection.getAutoCommit() != isAutoCommit)
				    wBaseConnection.setAutoCommit(isAutoCommit);
				//wBaseDsKey = rBaseDsKey;
				wBaseDsWrapper = rBaseDsWrapper;
				this.tGroupDataSource.setWriteTarget(wBaseDsWrapper);
				return wBaseConnection;
			} else {
				return null;
			}
		}
	}

	/**
	 * ��ʵ�ʵ�DataSource���һ���²㣨�п��ܲ�����ʵ�ģ�Connection
	 * ��Ȩ�ޣ��˷���ֻ��TGroupStatement��TGroupPreparedStatement��ʹ��
	 */
	Connection createNewConnection(DataSourceWrapper dsw, boolean isRead) throws SQLException {
		//�������ֻ�����ڵ�һ�ν�����/д���ӵ�ʱ���Ժ��Ǹ�����
		Connection conn;
		if (username != null)
			conn = dsw.getConnection(username, password);
		else
			conn = dsw.getConnection();

		//Ϊ�˱�֤������ȷ�رգ�������״̬��ֻ����д����
		setBaseConnection(conn, dsw, isRead && isAutoCommit);

		//ֻ��д�����ϵ���  setAutoCommit, ��  TGroupConnection#setAutoCommit �Ĵ��뱣��һ��
		if (!isRead || !isAutoCommit)
		        conn.setAutoCommit(isAutoCommit); //�½����ӵ�AutoCommitҪ�뵱ǰisAutoCommit��״̬ͬ��

		return conn;
	}

	private void setBaseConnection(Connection baseConnection, DataSourceWrapper dsw, boolean isRead) {
		if (baseConnection == null) {
			log.warn("setBaseConnection to null !!");
		}

		if (isRead)
			closeReadConnection();
		else
			closeWriteConnection();

		if (isRead) {
			rBaseConnection = baseConnection;
			//this.rBaseDsKey = dsw.getDataSourceKey();
			//this.rBaseDataSourceIndex = dsw.getDataSourceIndex();
			this.rBaseDsWrapper = dsw;
		} else {
			wBaseConnection = baseConnection;
			//this.wBaseDsKey = dsw.getDataSourceKey();
			//this.wBaseDataSourceIndex = dsw.getDataSourceIndex();
			this.wBaseDsWrapper = dsw;
			this.tGroupDataSource.setWriteTarget(dsw);
		}
	}

	private void closeReadConnection() {
		//r|wBaseConnection����ָ��ͬһ�����������һ���������ã��Ͳ�ȥ�ر�
		if (rBaseConnection != null && rBaseConnection != wBaseConnection) {
			try {
				rBaseConnection.close(); // �ɵ�baseConnectionҪ�ر�
			} catch (SQLException e) {
				log.error("close rBaseConnection failed.", e);
			}
			rBaseDsWrapper = null;
			rBaseConnection = null;
		}
	}

	private void closeWriteConnection() {
		//r|wBaseConnection����ָ��ͬһ�����������һ���������ã��Ͳ�ȥ�ر�
		if (wBaseConnection != null && rBaseConnection != wBaseConnection) {
			try {
				wBaseConnection.close(); // �ɵ�baseConnectionҪ�ر�
			} catch (SQLException e) {
				log.error("close wBaseConnection failed.", e);
			}
			wBaseDsWrapper = null;
			wBaseConnection = null;
		}
	}
 
	private Set<TGroupStatement> openedStatements = new HashSet<TGroupStatement>(2);

	void removeOpenedStatements(Statement statement) {
		if (!openedStatements.remove(statement)) {
			log.warn("current statmenet ��" + statement + " doesn't exist!");
		}
	}

	/* ========================================================================
	 * �ر��߼�
	 * ======================================================================*/
	private boolean closed;

	private void checkClosed() throws SQLException {
		if (closed) {
			throw new SQLException("No operations allowed after connection closed.");
		}
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	@SuppressWarnings("unchecked")
	public void close() throws SQLException {
		if (closed) {
			return;
		}
		closed = true;

		List<SQLException> exceptions = new LinkedList<SQLException>();
		try {
			// �ر�statement
			for (TGroupStatement stmt : openedStatements) {
				try {
					stmt.close(false);
				} catch (SQLException e) {
					exceptions.add(e);
				}
			}

			try {
				if (rBaseConnection != null && !rBaseConnection.isClosed()) {
					rBaseConnection.close();
				}
			} catch (SQLException e) {
				exceptions.add(e);
			}
			try {
				if (wBaseConnection != null && !wBaseConnection.isClosed()) {
					wBaseConnection.close();
				}
			} catch (SQLException e) {
				exceptions.add(e);
			}
		} finally {
			openedStatements.clear();
			// openedStatements = null; //�߼�������
			rBaseConnection = null;
			wBaseConnection = null;

			ThreadLocalDataSourceIndex.clearIndex();
		}
		ExceptionUtils.throwSQLException(exceptions, "close tconnection", Collections.EMPTY_LIST);
	}

	/* ========================================================================
	 * ����Statement�߼�
	 * ======================================================================*/
	public TGroupStatement createStatement() throws SQLException {
		checkClosed();
		TGroupStatement stmt = new TGroupStatement(tGroupDataSource, this);
		openedStatements.add(stmt);
		return stmt;
	}

	public TGroupStatement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		TGroupStatement stmt = createStatement();
		stmt.setResultSetType(resultSetType);
		stmt.setResultSetConcurrency(resultSetConcurrency);
		return stmt;
	}

	public TGroupStatement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		TGroupStatement stmt = createStatement(resultSetType, resultSetConcurrency);
		stmt.setResultSetHoldability(resultSetHoldability);
		return stmt;
	}

	/* ========================================================================
	 * ����PreparedStatement�߼�
	 * ======================================================================*/
	public TGroupPreparedStatement prepareStatement(String sql) throws SQLException {
		checkClosed();
		TGroupPreparedStatement stmt = new TGroupPreparedStatement(tGroupDataSource, this, sql);
		openedStatements.add(stmt);
		return stmt;
	}

	public TGroupPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		TGroupPreparedStatement stmt = prepareStatement(sql);
		stmt.setResultSetType(resultSetType);
		stmt.setResultSetConcurrency(resultSetConcurrency);
		return stmt;
	}

	public TGroupPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		TGroupPreparedStatement stmt = prepareStatement(sql, resultSetType, resultSetConcurrency);
		stmt.setResultSetHoldability(resultSetHoldability);
		return stmt;
	}

	public TGroupPreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		TGroupPreparedStatement stmt = prepareStatement(sql);
		stmt.setAutoGeneratedKeys(autoGeneratedKeys);
		return stmt;
	}

	public TGroupPreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		TGroupPreparedStatement stmt = prepareStatement(sql);
		stmt.setColumnIndexes(columnIndexes);
		return stmt;
	}

	public TGroupPreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		TGroupPreparedStatement stmt = prepareStatement(sql);
		stmt.setColumnNames(columnNames);
		return stmt;
	}

	/* ========================================================================
	 * ����CallableStatement�߼����洢����CallableStatement֧��
	 * ======================================================================*/
	private DataSourceTryer<CallableStatement> getCallableStatementTryer = new AbstractDataSourceTryer<CallableStatement>() {
		public CallableStatement tryOnDataSource(DataSourceWrapper dsw, Object... args) throws SQLException {
			String sql = (String) args[0];
			int resultSetType = (Integer) args[1];
			int resultSetConcurrency = (Integer) args[2];
			int resultSetHoldability = (Integer) args[3];
			Connection conn = TGroupConnection.this.createNewConnection(dsw, false);
			return getCallableStatement(conn, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
	};

	private CallableStatement getCallableStatement(Connection conn, String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		if (resultSetType == Integer.MIN_VALUE) {
			return conn.prepareCall(sql);
		} else if (resultSetHoldability == Integer.MIN_VALUE) {
			return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
		} else {
			return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
	}

	public TGroupCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		checkClosed();
		CallableStatement target;

		Connection conn = this.getBaseConnection(sql,false); //�洢����Ĭ����д��
		if (conn != null) {
			sql=GroupHintParser.removeTddlGroupHint(sql);
			target = getCallableStatement(conn, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		} else {
			// hint����
			Integer dataSourceIndex = GroupHintParser
						.convertHint2Index(sql);
			sql=GroupHintParser.removeTddlGroupHint(sql);
			if (dataSourceIndex < 0) {
				dataSourceIndex = ThreadLocalDataSourceIndex.getIndex();
			}
			target = tGroupDataSource.getDBSelector(false).tryExecute(null, getCallableStatementTryer,
					this.tGroupDataSource.getRetryingTimes(), sql, resultSetType, resultSetConcurrency,
					resultSetHoldability,dataSourceIndex);
		}
		TGroupCallableStatement stmt = new TGroupCallableStatement(tGroupDataSource, this, target, sql);
		if (resultSetType != Integer.MIN_VALUE) {
			stmt.setResultSetType(resultSetType);
			stmt.setResultSetConcurrency(resultSetConcurrency);
		}
		if (resultSetHoldability != Integer.MIN_VALUE) {
			stmt.setResultSetHoldability(resultSetHoldability);
		}
		openedStatements.add(stmt);
		return stmt;
	}

	public TGroupCallableStatement prepareCall(String sql) throws SQLException {
		return prepareCall(sql, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
	}

	public TGroupCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return prepareCall(sql, resultSetType, resultSetConcurrency, Integer.MIN_VALUE);
	}

	/* ========================================================================
	 * JDBC������ص�autoCommit���á�commit/rollback��TransactionIsolation��
	 * ======================================================================*/
	private boolean isAutoCommit = true; // jdbc�淶��������Ϊtrue

	public void setAutoCommit(boolean autoCommit0) throws SQLException {
		checkClosed();
		if (this.isAutoCommit == autoCommit0) {
			// ���ų����������״̬,true==true ��false == false: ʲôҲ����
			return;
		}
		this.isAutoCommit = autoCommit0;
		/*/////////////////////////////////////ֻ�������������
		if (this.rBaseConnection != null) {
			this.rBaseConnection.setAutoCommit(autoCommit0);
		}
		*/
		if (this.wBaseConnection != null) {
			this.wBaseConnection.setAutoCommit(autoCommit0);
		}
	}

	public boolean getAutoCommit() throws SQLException {
		checkClosed();
		return isAutoCommit;
	}

	public void commit() throws SQLException {
		checkClosed();
		if (isAutoCommit) {
			return;
		}

		/*/////////////////////////////////////ֻ�������������
		if (rBaseConnection != null) {
			try {
				rBaseConnection.commit();
			} catch (SQLException e) {
				log.error("Commit failed on " + this.rBaseDsKey + ":" + e.getMessage());
				throw e;
			}
		}
		*/
		if (wBaseConnection != null) {
			try {
				wBaseConnection.commit();
			} catch (SQLException e) {
				log.error("Commit failed on " + this.wBaseDsWrapper.getDataSourceKey() + ":" + e.getMessage());
				throw e;
			}
		}
	}

	public void rollback() throws SQLException {
		checkClosed();
		if (isAutoCommit) {
			return;
		}

		/*/////////////////////////////////////ֻ�������������
		if (rBaseConnection != null) {
			try {
				rBaseConnection.rollback();
			} catch (SQLException e) {
				log.error("Rollback failed on " + this.rBaseDsKey + ":" + e.getMessage());
				throw e;
			}
		}
		*/

		if (wBaseConnection != null) {
			try {
				wBaseConnection.rollback();
			} catch (SQLException e) {
				log.error("Rollback failed on " + this.wBaseDsWrapper.getDataSourceKey() + ":" + e.getMessage());
				throw e;
			}
		}
	}

	// TODO: �Ժ������ֵ������������
	private int transactionIsolation = -1;

	public int getTransactionIsolation() throws SQLException {
		checkClosed();
		return transactionIsolation;
	}

	public void setTransactionIsolation(int transactionIsolation) throws SQLException {
		checkClosed();
		this.transactionIsolation = transactionIsolation;
	}

	/* ========================================================================
	 * SQLWarning �� DatabaseMetaData
	 * ======================================================================*/
	public SQLWarning getWarnings() throws SQLException {
		checkClosed();
		if (rBaseConnection != null)
			return rBaseConnection.getWarnings();
		else if (wBaseConnection != null)
			return wBaseConnection.getWarnings();
		else
			return null;
	}

	public void clearWarnings() throws SQLException {
		checkClosed();
		if (rBaseConnection != null)
			rBaseConnection.clearWarnings();
		if (wBaseConnection != null)
			wBaseConnection.clearWarnings();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		checkClosed();
		if (rBaseConnection != null)
			return rBaseConnection.getMetaData();
		else if (wBaseConnection != null)
			return wBaseConnection.getMetaData();
		else
			return new TGroupDatabaseMetaData();
	}

	/* ========================================================================
	 * ������δʵ�ֵķ���
	 * ======================================================================*/
	public void rollback(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("rollback");
	}

	public Savepoint setSavepoint() throws SQLException {
		throw new UnsupportedOperationException("setSavepoint");
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		throw new UnsupportedOperationException("setSavepoint");
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("releaseSavepoint");
	}

	public String getCatalog() throws SQLException {
		throw new UnsupportedOperationException("getCatalog");
	}

	public void setCatalog(String catalog) throws SQLException {
		throw new UnsupportedOperationException("setCatalog");
	}

	public int getHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	public void setHoldability(int holdability) throws SQLException {
		/*
		 * ����㿴�������ô��ϲ������ mysqlĬ����5.x��jdbc driver����Ҳû��ʵ��holdability ��
		 * ����Ĭ�϶���.CLOSE_CURSORS_AT_COMMIT Ϊ�˼����������Ҳ��ֻʵ��close����
		 */

		// mysql 5.x��jdbc driverֻ֧��ResultSet.HOLD_CURSORS_OVER_COMMIT
		throw new UnsupportedOperationException("setHoldability");
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new UnsupportedOperationException("getTypeMap");
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException("setTypeMap");
	}

	public String nativeSQL(String sql) throws SQLException {
		throw new UnsupportedOperationException("nativeSQL");
	}

	/**
	 * ���ֿɶ���д
	 * @author junyu
	 */
	public boolean isReadOnly() throws SQLException {
		return false;
	}

	/**
	 * �����κ�����
	 * @author junyu
	 */
	public void setReadOnly(boolean readOnly) throws SQLException {

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

	public Clob createClob() throws SQLException {
		throw new SQLException("not support exception");
	}

	public Blob createBlob() throws SQLException {
		throw new SQLException("not support exception");
	}

	public NClob createNClob() throws SQLException {
		throw new SQLException("not support exception");
	}

	public SQLXML createSQLXML() throws SQLException {
		throw new SQLException("not support exception");
	}

	public boolean isValid(int timeout) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		throw new RuntimeException("not support exception");
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		throw new RuntimeException("not support exception");
	}

	public String getClientInfo(String name) throws SQLException {
		throw new SQLException("not support exception");
	}

	public Properties getClientInfo() throws SQLException {
		throw new SQLException("not support exception");
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		throw new SQLException("not support exception");
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw new SQLException("not support exception");
	}

}

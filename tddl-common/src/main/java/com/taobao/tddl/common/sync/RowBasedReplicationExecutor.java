/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sync;

import static com.taobao.tddl.common.Monitor.KEY3_COPY_2_SLAVE_EXCEPTION;
import static com.taobao.tddl.common.Monitor.KEY3_COPY_2_SLAVE_EXCEPTION_TIME_CONSUMING_IN_THREADPOOL;
import static com.taobao.tddl.common.Monitor.KEY3_COPY_2_SLAVE_SUCCESS;
import static com.taobao.tddl.common.Monitor.KEY3_COPY_2_SLAVE_SUCCESS_TIME_CONSUMING_IN_THREADPOOL;
import static com.taobao.tddl.common.Monitor.KEY3_COPY_2_SLAVE_TIMEOUT;
import static com.taobao.tddl.common.Monitor.KEY3_COPY_2_SLAVE_TIMEOUT_TIME_CONSUMING_IN_THREADPOOL;
import static com.taobao.tddl.common.Monitor.add;
import static com.taobao.tddl.common.Monitor.buildReplicationSqlKey2;
import static com.taobao.tddl.common.Monitor.buildTableKey1;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.jdbc.ArgPreparedStatementSetter;
import com.taobao.tddl.common.jdbc.MetaDataQueryForMapHandler;
import com.taobao.tddl.common.jdbc.QueryForMapHandler;
import com.taobao.tddl.common.jdbc.QueryForMapHandler.ColumnMetaData;
import com.taobao.tddl.common.jdbc.QueryForMapHandler.TableMetaData;
import com.taobao.tddl.common.jdbc.QueryForMapHandler.UseCachedMetaDataSetter;
import com.taobao.tddl.common.sync.SyncUtils.SQLExceptionInfo;
import com.taobao.tddl.interact.rule.bean.DBType;

public class RowBasedReplicationExecutor {
	private static long timeoutThreshold = 200;
	private static final Log log = LogFactory.getLog(RowBasedReplicationExecutor.class);
	private static final long masterAbsentReserveTime = 72*60*60*1000; //�����¼�����ڵ���־�����ٱ���ʱ�䣺72Сʱ

	private static final QueryForMapHandler queryForMapHandler = new MetaDataQueryForMapHandler(); 

	static class SqlArgs {
		public final String sql;
		public final Object[] args;
		public final String[] argNames; //Сд������Ҳ���Բ���������������һ���������֣�,��args��Ӧ

		public SqlArgs(String sql, Object[] args) {
			this.sql = sql;
			this.args = args;
			this.argNames = null;
		}

		public SqlArgs(String sql, List<Object> args, List<String> argnames) {
			this.sql = sql;
			this.args = args.toArray(new Object[args.size()]);
			this.argNames = argnames.toArray(new String[argnames.size()]);
		}
	}

	private static class InsensitiveColumnMapRowMapper extends ColumnMapRowMapper {
		protected String getColumnKey(String columnName) {
			return columnName.toLowerCase();
		}
		protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
			//int columType = rs.getMetaData().getColumnType(index); //TODO
			return super.getColumnValue(rs, index);
		}
	}

	/**
	 * ʵʱ�и��ƺ��º󲹳����������ø÷�����
	 * @return 
	 *   true��ʵʱ�£���ʾ���Ƴɹ�������log��¼�ɹ�ɾ���������£���ʾ���Ƴɹ�����ʧ�ܲ��ֲ�Ҫ������־������ɾ����־
	 *   flase��ʵʱ�£���ʾ����ʧ�ܡ�log��¼��Ȼ������־���У������£�Ҫ������־��¼
	 * ʵʱ�и��Ƶ��ø÷�������false��ɾ����־��¼��������������������
	 * �������������ø÷�������false���������ɾ����־��¼
	 */
	public static boolean execute(RowBasedReplicationContext context, boolean isDeleteSyncLog) {
		try {
			Map<String, Object> masterRow = getMasterRow(context);
			if (masterRow != null) {
				//SlaveInfo[] slaves = context.getReplicationMap().get(context.getMasterLogicTableName());
				SlaveInfo[] slaves = context.getSlaveInfos();
				boolean[] isReserveSyncLog = new boolean[1];
				isReserveSyncLog[0] = false;
				long timeConsumingInWritingDatabase = 0;
				for (SlaveInfo slave : slaves) {
					//���ҵ��ע����slaveDataHandler����ô������������masterRow
					if(slave.getSlaveDataHandler()!=null){
						masterRow=slave.getSlaveDataHandler().handle(masterRow, slave);
					}
					if (!slave.isAllowSync()) {
						continue;
					}
					try {
						long slavetimeConsumingInWritingDatabase = sync(context, masterRow, slave);
						profile(null, context, timeoutThreshold, slavetimeConsumingInWritingDatabase, slave.getIdentity());
						timeConsumingInWritingDatabase += slavetimeConsumingInWritingDatabase;
					} catch (Exception e) {
						log.error("execute exception", e);
						if (slave.isBreakOnFail()) {
							return false;
						} else if (slave.isRetryOnFail()) {
							isReserveSyncLog[0] = true;
						}
					}
				}

				profile(null, context, timeoutThreshold, timeConsumingInWritingDatabase, "");
				// ɾ��ͬ����־
				if (isDeleteSyncLog && !isReserveSyncLog[0]) {
					deleteSyncLog(context);
				}
				return !isReserveSyncLog[0]; //����false������־��trueɾ����־
			} else {
				log.warn(messageNotFoundRow("�����¼������", context));
				if (new Date().getTime() - context.getCreateTime().getTime() > masterAbsentReserveTime) {
					//�����¼�����ڣ�ͬʱ��־����ʱ����72Сʱ֮ǰ��ֱ��ɾ��
					if (isDeleteSyncLog) {
						//��ʵ��������������ʵʱ�и��Ʋ������ӳ�72Сʱ��Ϊ���߼������ԣ��������ж�
						deleteSyncLog(context); 
					}
					return true;
				}
				return false;
			}
		} catch (Exception e) {
			log.error("execute exception", e);
			return false;
		}
	}

	public static Map<String, Object> getMasterRow(RowBasedReplicationContext context) {
		List<Object> args = new ArrayList<Object>();
		//sql.append("select ").append(masterColumns).append(" from ").append(context.getMasterLogicTableName());
		StringBuilder whereSql = new StringBuilder("where ").append(context.getPrimaryKeyColumn()).append(" = ?");
		args.add(context.getPrimaryKeyValue());
		//��where��������ӷֿ��
		if (context.getMasterDatabaseShardColumn() != null && context.getMasterDatabaseShardValue() != null
				&& !context.getMasterDatabaseShardColumn().equalsIgnoreCase(context.getPrimaryKeyColumn())) {
			//��ͬ����־��¼�зֿ���Ϣ�����ҷֿ��в�����������ʱ������ֿ��в�ѯ����
			whereSql.append(" and ").append(context.getMasterDatabaseShardColumn()).append(" = ?");
			args.add(context.getMasterDatabaseShardValue());
		}
		//��where��������ӷֱ��
		if (context.getMasterTableShardColumn() != null && context.getMasterTableShardValue() != null
				&& !context.getMasterTableShardColumn().equalsIgnoreCase(context.getPrimaryKeyColumn())
				&& !context.getMasterTableShardColumn().equalsIgnoreCase(context.getMasterDatabaseShardColumn())) {
			//��ͬ����־��¼�зֱ���Ϣ�����ҷֱ��в����������кͷֿ���ʱ������ֱ��в�ѯ����
			whereSql.append(" and ").append(context.getMasterTableShardColumn()).append(" = ?");
			args.add(context.getMasterTableShardValue());
		}

		try {
			ThreadLocalMap.put(ThreadLocalString.DATASOURCE_INDEX, 0);
			return queryForMapHandler.queryForMap(context.getMasterJdbcTemplate(), context.getMasterLogicTableName(),
					context.getMasterColumns(), whereSql.toString(), args.toArray());
		} finally {
			ThreadLocalMap.remove(ThreadLocalString.DATASOURCE_INDEX);
		}
	}

	protected static long sync(RowBasedReplicationContext context, Map<String, Object> masterRow, SlaveInfo slave) {
		switch (context.getSqlType()) {
		case INSERT:
			if (slave.getSlaveReplicater() != null) {
				long beforeSlaveTime = System.currentTimeMillis();
				slave.getSlaveReplicater().insertSlaveRow(masterRow, slave);
				return System.currentTimeMillis() - beforeSlaveTime;
			}
			return insertSlaveRow(context, masterRow, slave, false);
		case UPDATE:
			if (slave.getSlaveReplicater() != null) {
				long beforeSlaveTime = System.currentTimeMillis();
				slave.getSlaveReplicater().updateSlaveRow(masterRow, slave);
				return System.currentTimeMillis() - beforeSlaveTime;
			}
			return updateSlaveRow(context, masterRow, slave);
		default:
			throw new RuntimeException("��������SQL����: " + context.getSqlType());
		}
	}

	private static String messageNotFoundRow(String title, RowBasedReplicationContext context) {
		StringBuilder message = new StringBuilder(title);
		message.append(", �߼�����: [").append(context.getMasterLogicTableName());
		message.append("], ����: [").append(context.getPrimaryKeyColumn()).append(" = ").append(
				context.getPrimaryKeyValue());
		message.append("], �ֿ��: [").append(context.getMasterDatabaseShardColumn()).append(" = ").append(
				context.getMasterDatabaseShardValue());
		message.append("], �ֱ��: [").append(context.getMasterTableShardColumn()).append(" = ").append(
				context.getMasterTableShardValue());
		message.append("]");

		return message.toString();
	}

	/**
	 * @param masterRow �����һ����¼��key��������value����ֵ
	 */
	protected static long insertSlaveRow(RowBasedReplicationContext context, Map<String, Object> masterRow,
			SlaveInfo slave, boolean throwOnExist) {
		if(slave.isDisableInsert()){
			return 0;
		}
		SqlArgs sqlInfo = buildSlaveInsertSql(masterRow, slave);

		if (log.isDebugEnabled()) {
			log.debug("sql = [" + sqlInfo.sql + "], args = " + Arrays.asList(sqlInfo.args));
		}
		long beforeInsertSlaveDBTime = System.currentTimeMillis();
		try {
			//context.getSlaveJdbcTemplates().get(slave.getDataSourceName()).update(sqlInfo.getSql(), sqlInfo.getArgs());
			slave.getJdbcTemplate().update(sqlInfo.sql, sqlInfo.args);
			
			return System.currentTimeMillis()-beforeInsertSlaveDBTime; 
		} catch (DataAccessException e) {
			if (!throwOnExist && (e instanceof DataIntegrityViolationException)) {
				if (e.getCause() instanceof SQLException) {
                    SQLExceptionInfo expInfo = SyncUtils.getSqlState((SQLException) e.getCause());
                    if (DBType.MYSQL.equals(slave.getDbType())
                            && SyncConstants.ERROR_CODE_DUPLICATE_PRIMARY_KEY_MYSQL == expInfo.getErrorCode()) {
                        return System.currentTimeMillis()-beforeInsertSlaveDBTime;
                    } else if(DBType.ORACLE.equals(slave.getDbType())
                            && SyncConstants.ERROR_CODE_DUPLICATE_PRIMARY_KEY_ORACLE == expInfo.getErrorCode()) {
                        return System.currentTimeMillis()-beforeInsertSlaveDBTime;
                    }
				}
			}
			profile(e, context, timeoutThreshold, System.currentTimeMillis()-beforeInsertSlaveDBTime, slave.getIdentity());
			throw e;
		}
	}

	protected static void profile(Throwable e, RowBasedReplicationContext context, long timeoutThreshold,
			long write2DBConsumingTime, String suffix) {
		long elapsedTime = System.currentTimeMillis() - context.getAfterMainDBSqlExecuteTime();
		long timeConsumingInThreadPool = context.getReplicationStartTime() - context.getAfterMainDBSqlExecuteTime();
		if (e == null) {
			successProfile(context, timeoutThreshold, elapsedTime, write2DBConsumingTime, timeConsumingInThreadPool, suffix);
		} else {
			exceptionProfile(context, timeoutThreshold, elapsedTime, write2DBConsumingTime, timeConsumingInThreadPool, suffix);
		}
	}

	private static void exceptionProfile(RowBasedReplicationContext context, long timeoutThreshold, long elapsedTime,
			long write2DBConsumingTime, long timeConsumingInThreadPool, String suffix) {
		// exception
		add(buildTableKey1(context.getMasterLogicTableName()), buildReplicationSqlKey2(context.getSql()),
				KEY3_COPY_2_SLAVE_EXCEPTION + suffix, elapsedTime, 1);
		add(buildTableKey1(context.getMasterLogicTableName()), buildReplicationSqlKey2(context.getSql()),
				KEY3_COPY_2_SLAVE_EXCEPTION_TIME_CONSUMING_IN_THREADPOOL + suffix, timeConsumingInThreadPool,1);
	}

	private static void successProfile(RowBasedReplicationContext context, long timeoutThreshold, long elapsedTime,
			long write2DBConsumingTime, long timeConsumingInThreadPool, String suffix) {
		if (elapsedTime > timeoutThreshold) {
			// timeout
			add(buildTableKey1(context.getMasterLogicTableName()), buildReplicationSqlKey2(context.getSql()),
					KEY3_COPY_2_SLAVE_TIMEOUT + suffix,  elapsedTime , 1);
			add(buildTableKey1(context.getMasterLogicTableName()), buildReplicationSqlKey2(context.getSql()),
					KEY3_COPY_2_SLAVE_TIMEOUT_TIME_CONSUMING_IN_THREADPOOL + suffix, timeConsumingInThreadPool,1);
		} else {
			// normal
			add(buildTableKey1(context.getMasterLogicTableName()), buildReplicationSqlKey2(context.getSql()),
					KEY3_COPY_2_SLAVE_SUCCESS + suffix, elapsedTime,1);
			add(buildTableKey1(context.getMasterLogicTableName()), buildReplicationSqlKey2(context.getSql()),
					KEY3_COPY_2_SLAVE_SUCCESS_TIME_CONSUMING_IN_THREADPOOL + suffix, timeConsumingInThreadPool,1);
		}
	}
	
	private static PreparedStatementSetter getPss(String tableName, Object[] args, String[] argNames) {
		TableMetaData tmd = queryForMapHandler.getTableMetaData(tableName);
		if (tmd == null) {
			log.error("Can't find cached MetaData for table " + tableName);
			return new ArgPreparedStatementSetter(args);
		}
		if (args != null && argNames != null && argNames.length != args.length) {
			log.error("Parameters length can't match the parameter name length. table:" + tableName + ",args.length:"
					+ args.length + ",argNames.length:" + argNames.length);
			return new ArgPreparedStatementSetter(args);
		}

		ColumnMetaData[] argMetas = new ColumnMetaData[argNames.length];
		for (int i = 0; i < argNames.length; i++) {
			argMetas[i] = tmd.getColumnMetaData(argNames[i]);
		}
		
		return new UseCachedMetaDataSetter(argMetas, args);
	}

	private static long updateSlaveRow(RowBasedReplicationContext context, Map<String, Object> masterRow,
			SlaveInfo slave) {
		if (slave.isDisableUpdate()) {
			if (slave.isAutoInsert()) {
				//��AutoInsert������£���ʹupdate�ر��ˣ�������ַֿ��¼�����ڣ�Ҳֱ�Ӳ��롣����׼���ڼ�ʹ��
				//ʹAutoInsert���ض�����update���ء�
				Map<String, Object> slaveRow = getSlaveRow(context, masterRow, slave);
				if (slaveRow == null) {
					// �ֿ��¼������
					log.warn(messageNotFoundRow(slave.getName()+"�ֿ��¼�������Զ�����", context));
					//���ǵ��ǹر�update��������ʷֿ��¼�������Զ��������������ͻʱ�������ɹ���
					//������������־,�����ٴδ���ʱ���ֿ��¼�Ѵ��ڣ����ڹر���update����ʹ��ʱ����汾�£�Ҳ�ǲ���update��
					return insertSlaveRow(context, masterRow, slave, false);
				}
			}
			return 0;
		}
		SqlArgs sqlInfo = buildSlaveUpdateSql(context, masterRow, slave);

		if (log.isDebugEnabled()) {
			log.debug("sql = [" + sqlInfo.sql + "], args = " + Arrays.asList(sqlInfo.args));
		}
		long beforeUpdateSlaveDBTime = System.currentTimeMillis();

		if(DBType.ORACLE.equals(slave.getDbType())){ //TODO �����и��ٶ������ⶼ��Oracle
		PreparedStatementSetter pss = getPss(context.getMasterLogicTableName(), sqlInfo.args, sqlInfo.argNames);
		if (slave.getJdbcTemplate().update(sqlInfo.sql, pss) != 0) {
			Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_UpdateSlaveRow_dup_all, 0, 1); //�ظ�ͬ������
			return System.currentTimeMillis() - beforeUpdateSlaveDBTime;
		}
		}else{//MYSQL���������ݿ⡣��������Oracle�ֿ���Mysqlʱ����Oracle��meta���÷ֿ��setObject3��������
			if (slave.getJdbcTemplate().update(sqlInfo.sql, sqlInfo.args) != 0) {
				Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_UpdateSlaveRow_dup_all, 0, 1); //�ظ�ͬ������
				return System.currentTimeMillis() - beforeUpdateSlaveDBTime;
			}
		}

		// ��ȡ�÷ֿ��¼���Ա��֪Ϊʲô����²��ɹ�
		Map<String, Object> slaveRow = getSlaveRow(context, masterRow, slave);
		RuntimeException exc = null;
		if (slaveRow == null) {
			// �ֿ��¼������
			if (slave.isAutoInsert()) {
				/**
				 * ��������ͻҪ�״�throwOnExist��true�����Ա�����־������update�¼�������insert�¼�����������£�
				 * ���update�ȴ�����ߵ���������Զ����룬��ʱ���insert�¼�����ִ�гɹ������Զ������������ͻ��
				 * �������������׳����Ļ���insertSlaveRow�ᵱ���ɹ���ɾ����־���Ӷ����»ᶪʧ��
				 */
				if (log.isInfoEnabled()) {
					log.info(messageNotFoundRow(slave.getName()+"�ֿ��¼�������Զ�����", context));
				}
				return insertSlaveRow(context, masterRow, slave, true);
			} else {
				exc = new RuntimeException(messageNotFoundRow(slave.getName()+"�ֿ��¼������", context));
			}
			profile(exc, context, timeoutThreshold, System.currentTimeMillis() - beforeUpdateSlaveDBTime, slave.getIdentity());
			throw exc;
		} else if (slave.isNoSyncVersion()) {
			//û�а汾�ſɱȽϣ������ɹ�(insert update˳��ߵ���update����insert��ִ�У���getSlaveRowǰ��insert��ɣ����ߵ�����)
			return System.currentTimeMillis()-beforeUpdateSlaveDBTime;
		} else {
			// �ֿ��¼���ڣ�������Ϊ�����¼�汾�űȷֿ��¼�汾�žɣ����Ը��²��ɹ�
			long masterVersion = getSyncVersion(masterRow.get(SyncConstants.SYNC_VERSION_COLUMN_NAME));
			long slaveVersion = getSyncVersion(slaveRow.get(SyncConstants.SYNC_VERSION_COLUMN_NAME));

			if (slaveVersion >= masterVersion) {
				// �ֿ��¼�汾�Ŵ��ڵ��������¼�汾�ţ��ֿ��¼�Ѿ������£�����ͬ���ɹ�
				log.warn("�ֿ��¼�汾�Ŵ��ڵ��������¼�汾��. masterVersion=" + masterVersion + ",slaveVersion=" + slaveVersion
						+ ",PrimaryKeyValue=" + context.getPrimaryKeyValue());
				Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_UpdateSlaveRow_dup_all, 1, 1); //�ظ�ͬ������
				return System.currentTimeMillis()-beforeUpdateSlaveDBTime;
			} else {
				exc = new RuntimeException("�ֿ��¼�汾��С�������¼�汾��");
				profile(exc, context, timeoutThreshold, System.currentTimeMillis()-beforeUpdateSlaveDBTime, slave.getIdentity());
				// �ֿ��¼�汾��С�������¼�汾�ţ����Ǹղ���û�и���
				// ���ܵ�ԭ���Ǹ��µ�ʱ���¼�����ڣ�Ȼ�������update�󣬲�����ִ����insert
				// ���Ҳ�������ϵļ�¼���������Ϊ����ͬ��ʧ��
				throw exc;
			}
		}
	}

	private static SqlArgs buildSlaveInsertSql(Map<String, Object> masterRow, SlaveInfo slave) {
		StringBuilder sql = new StringBuilder();
		

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		Object[] args = null;
		if (slave.getColumns() == null || slave.getColumns().length == 0) {
			 args = new Object[masterRow.size()];
			int count = 0;
			for (Map.Entry<String, Object> entry : masterRow.entrySet()) {
				String columnName = entry.getKey();

				if (count > 0) {
					columns.append(", ");
					values.append(", ");
				}
				columns.append(columnName);
				values.append("?");
				if (slave.getColumRanges().containsKey(columnName) && entry.getValue() != null
						&& entry.getValue() instanceof Number) {
					Long value = ((Number) entry.getValue()).longValue();
					Long[] range = slave.getColumRanges().get(columnName);
					Long min = range[0], max = range[1];
					if (max != null && value > max) {
						args[count++] = max;
					} else if (min != null && value < min) {
						args[count++] = min;
					} else {
						args[count++] = slave.changeToDefaultOnNull(columnName, entry.getValue());
					}
				} else {
					args[count++] = slave.changeToDefaultOnNull(columnName, entry.getValue());
				}
			}
		} else {
			args = new Object[slave.getColumns().length];
			int count = 0;
			for (String columnName : slave.getColumns()) {
				if (count > 0) {
					columns.append(", ");
					values.append(", ");
				}
				columns.append(columnName);
				values.append("?");

				args[count++] = slave.changeToDefaultOnNull(columnName, masterRow.get(columnName));
			}
		}

		sql.append("insert into ").append(slave.getName()).append(" (");
		sql.append(columns).append(") values (").append(values).append(")");

		return new SqlArgs(sql.toString(), args);
	}

	private static SqlArgs buildSlaveUpdateSql(RowBasedReplicationContext context, Map<String, Object> masterRow,
			SlaveInfo slave) {
		StringBuilder sql = new StringBuilder("update ").append(slave.getName()).append(" set ");
		List<Object> args = new ArrayList<Object>();
		List<String> argnames = new ArrayList<String>();

		String[] cols = slave.getColumns();
		Collection<String> colomnNames = cols != null && cols.length != 0 ? Arrays.asList(cols) : masterRow.keySet();

		for (String columnName : colomnNames) {
			sql.append(columnName).append("=? ,");
			args.add(slave.changeToDefaultOnNull(columnName, masterRow.get(columnName)));
			argnames.add(columnName);
		}
		sql.deleteCharAt(sql.length()-1);

		sql.append(" where ").append(context.getPrimaryKeyColumn()).append(" = ?");
		args.add(masterRow.get(context.getPrimaryKeyColumn()));
		argnames.add(context.getPrimaryKeyColumn());

		if (slave.getDatabaseShardColumn() != null
				&& !slave.getDatabaseShardColumn().equalsIgnoreCase(context.getPrimaryKeyColumn())) {
			sql.append(" and ").append(slave.getDatabaseShardColumn()).append(" = ?");
			args.add(masterRow.get(slave.getDatabaseShardColumn()));
			argnames.add(slave.getDatabaseShardColumn());
		}

		if (slave.getTableShardColumn() != null
				&& !slave.getTableShardColumn().equalsIgnoreCase(context.getPrimaryKeyColumn())
				&& !slave.getTableShardColumn().equalsIgnoreCase(slave.getDatabaseShardColumn())) {
			sql.append(" and ").append(slave.getTableShardColumn()).append(" = ?");
			args.add(masterRow.get(slave.getTableShardColumn()));
			argnames.add(slave.getTableShardColumn());
		}

		if (!slave.isNoSyncVersion()) {
			switch (slave.getDbType()) {
			case MYSQL:
				sql.append(" and ifnull(");
				break;
			case ORACLE:
				sql.append(" and nvl(");
				break;
			default:
				throw new RuntimeException("�����µ�dbType������û�и���");
			}
			sql.append(SyncConstants.SYNC_VERSION_COLUMN_NAME).append(", ?)").append(" < ?");
			args.add(SyncConstants.SYNC_VERSION_DEFAULT_VALUE);
			argnames.add(SyncConstants.SYNC_VERSION_COLUMN_NAME);
			args.add(masterRow.get(SyncConstants.SYNC_VERSION_COLUMN_NAME));
			argnames.add(SyncConstants.SYNC_VERSION_COLUMN_NAME);
		}

		return new SqlArgs(sql.toString(), args, argnames);
	}

	private static SqlArgs buildSlaveSelectSql(RowBasedReplicationContext context, Map<String, Object> masterRow,
			SlaveInfo slave) {
		StringBuilder sql = new StringBuilder();
		List<Object> args = new ArrayList<Object>();

		//sql.append("select * from ").append(slave.getName());
		//Ŀǰ����ҵ��ķֿ�ֻ����sync_version�͹�����
		//TODO Ŀǰ��������select 1 from auction_id_route; count(1) ��Ȼ���Խ����������岻һ�£�������ʱ����*
		String selectCols = slave.isNoSyncVersion() ? "1" : SyncConstants.SYNC_VERSION_COLUMN_NAME;
		sql.append("select ").append(selectCols).append(" from ").append(slave.getName());
		sql.append(" where ").append(context.getPrimaryKeyColumn()).append(" = ?");
		args.add(context.getPrimaryKeyValue());

		if (slave.getDatabaseShardColumn() != null
				&& !slave.getDatabaseShardColumn().equalsIgnoreCase(context.getPrimaryKeyColumn())) {
			sql.append(" and ").append(slave.getDatabaseShardColumn()).append(" = ?");
			args.add(masterRow.get(slave.getDatabaseShardColumn()));
		}

		if (slave.getTableShardColumn() != null
				&& !slave.getTableShardColumn().equalsIgnoreCase(context.getPrimaryKeyColumn())
				&& !slave.getTableShardColumn().equalsIgnoreCase(slave.getDatabaseShardColumn())) {
			sql.append(" and ").append(slave.getTableShardColumn()).append(" = ?");
			args.add(masterRow.get(slave.getTableShardColumn()));
		}

		return new SqlArgs(sql.toString(), args.toArray(new Object[args.size()]));
	}

	/**
	 * ֻ�õ�sync_version�ֶ� 
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getSlaveRow(RowBasedReplicationContext context, Map<String, Object> masterRow,
			SlaveInfo slave) {
		SqlArgs sqlInfo = buildSlaveSelectSql(context, masterRow, slave);

		if (log.isDebugEnabled()) {
			log.debug("sql = [" + sqlInfo.sql + "], args = " + Arrays.asList(sqlInfo.args));
		}

		try {
			return (Map<String, Object>) slave.getJdbcTemplate().queryForObject(sqlInfo.sql, sqlInfo.args,
					new InsensitiveColumnMapRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public static void deleteSyncLog(RowBasedReplicationContext context) {
		if (context.getSyncLogId() == null) {
			return; //ѡ���ͬ��������־��Ĳ���ʱ�������������
		}
		StringBuilder sql = new StringBuilder();
		sql.append("delete from sync_log_").append(SyncUtils.getSyncLogTableSuffix(context.getSyncLogId()));
		sql.append(" where id = ?");

		if (log.isDebugEnabled()) {
			log.debug("deleteSyncLog, sql = [" + sql.toString() + "], args = [" + context.getSyncLogId() + "]");
		}

		context.getSyncLogJdbcTemplate().update(sql.toString(), new Object[] { context.getSyncLogId() });
	}

	/**
	 * ʹ����jdbc�������¹��ܵ�delete
	 */
	public static void batchDeleteSyncLog(Collection<RowBasedReplicationContext> contexts) {
		long timeused, time0 = System.currentTimeMillis();
		String sqlpattern = "delete from sync_log_{0} where id = ?";

		/**
		 * ������RowBasedReplicationContext������־�⡢ÿ����ÿ����־���Ӧ��updateSql���࣬������
		 */
		Map<JdbcTemplate, Map<String/*����log���SQL*/, List<RowBasedReplicationContext>>> sortedContexts = 
				buildSortedContexts(contexts, sqlpattern.toString());

		for (Map.Entry<JdbcTemplate, Map<String, List<RowBasedReplicationContext>>> e0 : sortedContexts.entrySet()) {
			JdbcTemplate jt = e0.getKey();
			for (Map.Entry<String, List<RowBasedReplicationContext>> e : e0.getValue().entrySet()) {
				final List<RowBasedReplicationContext> endContexts = e.getValue();
				BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
					public int getBatchSize() {
						return endContexts.size();
					}
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						RowBasedReplicationContext context = endContexts.get(i);
						ps.setString(1, context.getSyncLogId());
					}
				};
				jt.batchUpdate(e.getKey(), setter);
				if (log.isDebugEnabled()) {
					log.debug("[batchDeleteSyncLog], sql = [" + e.getKey() + "], batch size="+endContexts.size());
				}
			}
		}
		timeused = System.currentTimeMillis() - time0;
		log.warn(contexts.size() + " replication logs deleted, time used:" + timeused);
		Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_BatchDeleteSyncLog, contexts.size(), timeused);
	}

	private final static int extraListSizePlus = 5;
	
	/**
	 * ʹ����jdbc�������¹��ܵ�delete
	 */
	public static void inDeleteSyncLog(Collection<RowBasedReplicationContext> contexts, int onceSize) {
		onceSize += extraListSizePlus;
		long timeused, time0 = System.currentTimeMillis();
		StringBuilder sqlpattern = new StringBuilder("delete from sync_log_{0} where id in (?");
		for(int i = 0; i < onceSize - 1; i++) {
			sqlpattern.append(", ?");
		}
		sqlpattern.append(")");

		/**
		 * ������RowBasedReplicationContext������־�⡢ÿ����ÿ����־���Ӧ��updateSql���࣬������
		 */
		Map<JdbcTemplate, Map<String/*����log���SQL*/, List<RowBasedReplicationContext>>> sortedContexts = 
				buildSortedContexts(contexts, sqlpattern.toString());

		for (Map.Entry<JdbcTemplate, Map<String, List<RowBasedReplicationContext>>> e0 : sortedContexts.entrySet()) {
			JdbcTemplate jt = e0.getKey();
			for (Map.Entry<String, List<RowBasedReplicationContext>> e : e0.getValue().entrySet()) {
				final List<RowBasedReplicationContext> endContexts = e.getValue();
				List<String> ids = new ArrayList<String>(endContexts.size());
				for(int i = 0; i < endContexts.size(); i++) {
					ids.add(endContexts.get(i).getSyncLogId());
				}
				updateIn(jt, e.getKey(), ids, onceSize);
				if (log.isDebugEnabled()) {
					log.debug("[inDeleteSyncLog], sql = [" + e.getKey() + "], batch size="+endContexts.size());
				}
			}
		}
		timeused = System.currentTimeMillis() - time0;
		log.warn(contexts.size() + " replication logs inDeleted, time used:" + timeused);
		Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_BatchDeleteSyncLog, contexts.size(), timeused);
	}
	
	/**
	 * delete ... where WW=? and UU=? and id in(...)
	 * update set XX=?, YY=? where WW=? and UU=? and id in(...)
	 */
	public static <T> void updateIn(JdbcTemplate template, String sql, List<T> ids, int onceSize, Object... externArgs) {
		int index = 0;
		while(index + onceSize <= ids.size()) {
			Object[] param = new Object[externArgs.length + onceSize];
			int i = 0;
			for(; i < externArgs.length; i++) {
				param[i] = externArgs[i];
			}
			for(int j = 0; j < onceSize; j++) {
				param[i++] = ids.get(index++);
			}
			template.update(sql, param);
		}
		if(index == ids.size()) {
			return;
		}
		Object[] param = new Object[onceSize];
		int i = 0;
		for(; i < externArgs.length; i++) {
			param[i] = externArgs[i];
		}
		for(; i < onceSize && index < ids.size(); i++) {
			param[i] = ids.get(index++);
		}
		for(; i < onceSize; i++) {
			param[i] = ids.get(0);
		}
		template.update(sql, param);
	}
	
	/**
	 *  next_sync_timeÿ���Ӻ��ʱ�� = next_sync_time - gmt_create
	 *  1. next_sync_time = next_sync_time + (next_sync_time - gmt_create)  
	 *  2. next_sync_time = ��ǰʱ�� + (next_sync_time - gmt_create)         --ʵ�ʲ���
	 *  3. next_sync_time = ��ǰʱ�� + (��ǰʱ�� - gmt_create)  
	 *  2��ָ����������
	 */
	public static void updateSyncLog(RowBasedReplicationContext context, final long extraPlusTime) {
		StringBuilder sql = new StringBuilder();
		sql.append("update sync_log_").append(SyncUtils.getSyncLogTableSuffix(context.getSyncLogId()));
		sql.append(" set next_sync_time=? where id = ?");
		//TODO �ӳ�ʱ����� 

		Object[] params = new Object[] { getNextSyncTime(context, extraPlusTime), context.getSyncLogId() };

		if (log.isDebugEnabled()) {
			log.debug("updateSyncLog, sql = [" + sql.toString() + "], args = [" + params[0] + "," + params[1] + "]");
		}
		context.getSyncLogJdbcTemplate().update(sql.toString(), params);
	}

	/**
	 * ʹ����jdbc�������¹��ܵ�update
	 */
	public static void batchUpdateSyncLog(Collection<RowBasedReplicationContext> contexts, final long extraPlusTime) {
		long timeused, time0 = System.currentTimeMillis();
		String sqlpattern = "update sync_log_{0} set next_sync_time=? where id = ?";

		/**
		 * ������RowBasedReplicationContext������־�⡢ÿ����ÿ����־���Ӧ��updateSql���࣬������
		 */
		Map<JdbcTemplate, Map<String/*����log���SQL*/, List<RowBasedReplicationContext>>> sortedContexts = 
				buildSortedContexts(contexts, sqlpattern.toString());

		for (Map.Entry<JdbcTemplate, Map<String, List<RowBasedReplicationContext>>> e0 : sortedContexts.entrySet()) {
			JdbcTemplate jt = e0.getKey();
			for (Map.Entry<String, List<RowBasedReplicationContext>> e : e0.getValue().entrySet()) {
				final List<RowBasedReplicationContext> endContexts = e.getValue();
				BatchPreparedStatementSetter setter = new BatchPreparedStatementSetter() {
					public int getBatchSize() {
						return endContexts.size();
					}
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						RowBasedReplicationContext context = endContexts.get(i);
						ps.setTimestamp(1, getNextSyncTime(context, extraPlusTime));
						ps.setString(2, context.getSyncLogId());
					}
				};
				jt.batchUpdate(e.getKey(), setter);
				if (log.isDebugEnabled()) {
					log.debug("[batchUpdateSyncLog], sql = [" + e.getKey() + "], batch size="+endContexts.size());
				}
			}
		}

		timeused = System.currentTimeMillis() - time0;
		log.warn(contexts.size() + " replication logs updated, time used:" + timeused);
		Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_BatchUpdateSyncLog, contexts.size(), timeused);
	}
	
	public static void inUpdateSyncLog(Collection<RowBasedReplicationContext> contexts, final long extraPlusTime, int onceSize) {
		onceSize += extraListSizePlus;
		long timeused, time0 = System.currentTimeMillis();
		StringBuilder sqlpattern = new StringBuilder("update sync_log_{0} set next_sync_time=? where id in (?");
		for(int i = 0; i < onceSize - 1; i++) {
			sqlpattern.append(", ?");
		}
		sqlpattern.append(")");

		/**
		 * ������RowBasedReplicationContext������־�⡢ÿ����ÿ����־���Ӧ��updateSql���࣬������
		 */
		Map<JdbcTemplate, Map<String/*����log���SQL*/, List<RowBasedReplicationContext>>> sortedContexts = 
			buildSortedContexts(contexts, sqlpattern.toString());

		for (Map.Entry<JdbcTemplate, Map<String, List<RowBasedReplicationContext>>> e0 : sortedContexts.entrySet()) {
			JdbcTemplate jt = e0.getKey();
			for (Map.Entry<String, List<RowBasedReplicationContext>> e : e0.getValue().entrySet()) {
				final List<RowBasedReplicationContext> endContexts = e.getValue();
				List<String> ids = new ArrayList<String>(endContexts.size());
				for(int i = 0; i < endContexts.size(); i++) {
					ids.add(endContexts.get(i).getSyncLogId());
				}
				updateIn(jt, e.getKey(), ids, onceSize, getNextSyncTime(endContexts.get(0), extraPlusTime));
				if (log.isDebugEnabled()) {
					log.debug("[inUpdateSyncLog], sql = [" + e.getKey() + "], batch size="+endContexts.size());
				}
			}
		}

		timeused = System.currentTimeMillis() - time0;
		log.warn(contexts.size() + " replication logs inUpdated, time used:" + timeused);
		Monitor.add(Monitor.KEY1, Monitor.KEY2_SYNC, Monitor.KEY3_BatchUpdateSyncLog, contexts.size(), timeused);
	}
	
	private static Map<JdbcTemplate, Map<String/*����log���SQL*/, List<RowBasedReplicationContext>>> buildSortedContexts(
			Collection<RowBasedReplicationContext> contexts, String sqlpattern) {
		Map<JdbcTemplate, Map<String/*����log���SQL*/, List<RowBasedReplicationContext>>> sortedContexts = new HashMap<JdbcTemplate, Map<String, List<RowBasedReplicationContext>>>();
		for (RowBasedReplicationContext context : contexts) {
			Map<String, List<RowBasedReplicationContext>> sql2contexts = getHashMap(sortedContexts, context.getSyncLogJdbcTemplate());
			String sql = MessageFormat.format(sqlpattern, SyncUtils.getSyncLogTableSuffix(context.getSyncLogId()));
			List<RowBasedReplicationContext> endContexts = getArrayList(sql2contexts, sql);
			endContexts.add(context);
		}
		return sortedContexts;
	}

	private static Timestamp getNextSyncTime(RowBasedReplicationContext context, long extraPlusTime) {
		long delay = context.getNextSyncTime().getTime() - context.getCreateTime().getTime();
		return new Timestamp(System.currentTimeMillis() + delay + extraPlusTime);
	}

	private static long getSyncVersion(Object value) {
		long version = SyncConstants.SYNC_VERSION_DEFAULT_VALUE;

		if (value != null) {
			if (value instanceof Integer) {
				version = ((Integer) value).longValue();
			} else if (value instanceof Long) {
				version = ((Long) value).longValue();
			} else if (value instanceof BigInteger) {
				version = ((BigInteger) value).longValue();
			} else if (value instanceof BigDecimal) {
				version = ((BigDecimal) value).longValue();
			} else if (value instanceof Short) {
				version = ((Short) value).longValue();
			} else if (value instanceof Byte) {
				version = ((Byte) value).longValue();
			} else {
				throw new RuntimeException("Unsupported data type [" + value.getClass() + "] of sync_version");
			}
		}

		return version;
	}

	/**
	 * ������key�Ѵ�����map�У��򷵻��Ѵ���ֵ������Ϊkey����һ���µ�HashMap������
	 * @return ����Ϊ�ա�����ֵ���´��������ѷ���m�е�HashMap
	 */
	@SuppressWarnings("unchecked")
	private static <K, V> V getHashMap(Map<K, V> m, K key) {
		V value = m.get(key);
		if (value == null) {
			value = (V) new HashMap();
			m.put(key, value);
		}
		return value;
	}

	/**
	 * ������key�Ѵ�����map�У��򷵻��Ѵ���ֵ������Ϊkey����һ���µ�ArrayList������
	 * @return ����Ϊ�ա�����ֵ���´��������ѷ���m�е�ArrayList
	 */
	@SuppressWarnings("unchecked")
	private static <K, V> V getArrayList(Map<K, V> m, K key) {
		V value = m.get(key);
		if (value == null) {
			value = (V) new ArrayList();
			m.put(key, value);
		}
		return value;
	}
	
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sync;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import com.taobao.tddl.interact.rule.bean.SqlType;

/**
 * һ�������飬���Զ�Ӧ���������
 * һ���������Ӧһ��tddl-rule��ÿ���������Ӧ���Ե�tddl-rule
 * <master name="feed_receive">
 *	<slaves>
 *		<slave name="feed_send" type="oracle">
 *			<data-source-name>REP_SLAVE_ORACLE</data-source-name>
 *			<table-shard-column>RATER_UID</table-shard-column>
 *		</slave>
 *	</slaves>
 * </master>
 * @author nianbing
 */
public class RowBasedReplicationContext {
	public RowBasedReplicationContext() {/* ��Ĭ�Ϲ��캯������������new�ĵط� */
	}

	private String syncLogDsKey;
	private JdbcTemplate syncLogJdbcTemplate; //��־��ԭʼ����Դ
	private JdbcTemplate masterJdbcTemplate; //�������TDataSource��Ӧ��JdbcTemplate
	private SlaveInfo[] slaveInfos; //������Ϣ
	private SqlType sqlType;
	private String primaryKeyColumn; //��������,Setter������֤��Сд
	private Object primaryKeyValue; //����ֵ
	private String masterLogicTableName; //�����߼�����,Setter������֤��Сд
	private String masterDatabaseShardColumn; //������ֿ�����,Setter������֤��Сд
	private Object masterDatabaseShardValue; //������ֿ���ֵ
	private String masterTableShardColumn; //������ֱ�����,Setter������֤��Сд
	private Object masterTableShardValue; //������ֱ���ֵ
	private String syncLogId;
	private Timestamp createTime; //��־����ʱ��
	private Timestamp nextSyncTime; //��־�´�ͬ��ʱ��

	private long afterMainDBSqlExecuteTime; //ִ���������ɹ����ʱ���
	private String sql; //��ǰ��ʱ������
	private long replicationStartTime; //�����ĸ�������ʼִ��ʱ��ʱ��㡣

	private String masterColumns;

	public JdbcTemplate getMasterJdbcTemplate() {
		return masterJdbcTemplate;
	}

	public void setMasterJdbcTemplate(JdbcTemplate masterJdbcTemplate) {
		this.masterJdbcTemplate = masterJdbcTemplate;
	}

	public SqlType getSqlType() {
		return sqlType;
	}

	public void setSqlType(SqlType sqlType) {
		this.sqlType = sqlType;
	}

	public String getPrimaryKeyColumn() {
		return primaryKeyColumn;
	}

	public void setPrimaryKeyColumn(String primaryKeyColumn) {
		this.primaryKeyColumn = primaryKeyColumn == null ? null : primaryKeyColumn.toLowerCase();
	}

	public Object getPrimaryKeyValue() {
		return primaryKeyValue;
	}

	public void setPrimaryKeyValue(Object primaryKeyValue) {
		this.primaryKeyValue = primaryKeyValue;
	}

	public String getMasterLogicTableName() {
		return masterLogicTableName;
	}

	public void setMasterLogicTableName(String masterLogicTableName) {
		this.masterLogicTableName = masterLogicTableName == null ? null : masterLogicTableName.toLowerCase();
	}

	public String getMasterDatabaseShardColumn() {
		return masterDatabaseShardColumn;
	}

	public void setMasterDatabaseShardColumn(String masterDatabaseShardColumn) {
		this.masterDatabaseShardColumn = masterDatabaseShardColumn == null ? null : masterDatabaseShardColumn.toLowerCase();
	}

	public Object getMasterDatabaseShardValue() {
		return masterDatabaseShardValue;
	}

	public void setMasterDatabaseShardValue(Object masterDatabaseShardValue) {
		this.masterDatabaseShardValue = masterDatabaseShardValue;
	}

	public String getMasterTableShardColumn() {
		return masterTableShardColumn;
	}

	public void setMasterTableShardColumn(String masterTableShardColumn) {
		this.masterTableShardColumn = masterTableShardColumn == null ? null : masterTableShardColumn.toLowerCase();
	}

	public Object getMasterTableShardValue() {
		return masterTableShardValue;
	}

	public void setMasterTableShardValue(Object masterTableShardValue) {
		this.masterTableShardValue = masterTableShardValue;
	}

	public JdbcTemplate getSyncLogJdbcTemplate() {
		return syncLogJdbcTemplate;
	}

	public void setSyncLogJdbcTemplate(JdbcTemplate syncLogJdbcTemplate) {
		this.syncLogJdbcTemplate = syncLogJdbcTemplate;
	}

	public String getSyncLogId() {
		return syncLogId;
	}

	public void setSyncLogId(String syncLogId) {
		this.syncLogId = syncLogId;
	}

	public SlaveInfo[] getSlaveInfos() {
		return slaveInfos;
	}

	public void setSlaveInfos(SlaveInfo[] slaveInfos) {
		this.slaveInfos = slaveInfos;
	}

	public long getAfterMainDBSqlExecuteTime() {
		return afterMainDBSqlExecuteTime;
	}

	public void setAfterMainDBSqlExecuteTime(long afterMainDBSqlExecuteTime) {
		this.afterMainDBSqlExecuteTime = afterMainDBSqlExecuteTime;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public long getReplicationStartTime() {
		return replicationStartTime;
	}

	public void setReplicationStartTime(long replicationStartTime) {
		this.replicationStartTime = replicationStartTime;
	}

	public Timestamp getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public Timestamp getNextSyncTime() {
		return nextSyncTime;
	}

	public void setNextSyncTime(Timestamp nextSyncTime) {
		this.nextSyncTime = nextSyncTime;
	}

	public String getMasterColumns() {
		return masterColumns;
	}

	public void setMasterColumns(String masterColumns) {
		this.masterColumns = masterColumns;
	}

	public String getSyncLogDsKey() {
		return syncLogDsKey;
	}

	public void setSyncLogDsKey(String syncLogDsKey) {
		this.syncLogDsKey = syncLogDsKey;
	}
}

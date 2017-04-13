/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sync;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import com.taobao.tddl.interact.rule.bean.DBType;

/**
 * <master name="feed_receive">
 * <master-column></master-column>
 *	<slaves>
 *		<slave name="feed_send" type="oracle">
 *			<data-source-name>REP_SLAVE_ORACLE</data-source-name>     <!-- mandatory -->
 *          <database-shard-column>xxx</database-shard-column>        <!-- optional -->
 *			<table-shard-column>RATER_UID</table-shard-column>        <!-- optional -->
 *          <columns>                                                 <!-- optional -->
 *             <column>id</column>
 *             <column>name</column>
 *          </columns>
 *		</slave>
 *	</slaves>
 * </master>
 * 
 * setter�б�֤databaseShardColumn��tableShardColumn��name��type��columns��ΪСд
 * 
 */
public class SlaveInfo {
	private String name; // �����߼�������Setter������֤��Сд
	private DBType dbType; // �������ݿ����ͣ�oracle/mysql��SyncConstants.DATABASE_TYPE_MYSQL/DATABASE_TYPE_ORACLE��
	private String databaseShardColumn; // ����ֿ�Сд������Setter������֤��Сд
	private String tableShardColumn; // ����ֱ�Сд������Setter������֤��Сд
	private String[] columns; // ��Ҫ���Ƶ��е�Сд������ȱʡΪ�����ȫ���У�Setter������֤��Сд
	private JdbcTemplate jdbcTemplate; // ����TDataSource��Ӧ��JdbcTemplate������dataSourceName�ֹ�����
	/**
	 * ����TDataSource����
	 * 2.4.1֮��SlaveInfo�� DataSourceName���Ա�������
	 * �����������͵ķ�ʽ�£���Ĭ�ϸ��Ƶ��ֿ�ʱ������ָ����TDS�е�dbIndex���Ը��Ƶ���Ӧ�����ݿ���
	 */
	private String dataSourceName;
	private SlaveReplicater slaveReplicater; // ͬ��(����)Ŀ�겻�����ݿ����������øýӿ�ʵ�ֶ��ƹ���
	private String slaveReplicaterName; //slaveReplicater�ĸ�spring�е�beanId
    /**
     * application may want do something special
     * thing like change column value,add column
     * and so on ,they could inject this interface
     * implementation to make their logic work
     * 
     * default,this attribute is null
     */
	private SlaveDataHandler slaveDataHandler=null;
	
	public String getIdentity() {
		return "_" + dataSourceName + "_" + name;
	}
	
	private volatile boolean allowSync = true;

	/**
	 * �������ԣ�Ĭ��false
	 */
	private boolean isDisableUpdate; // �Ƿ�ر�update
	private boolean isDisableInsert; // �Ƿ�ر�insert
	private boolean isNoSyncVersion; // �Ƿ񲻹���sync_version(����򱸿�û��sync_version�ֶε�ʱ����Ϊtrue)
	/**
	 * true ����ʱ���������м�¼���ֿ��¼�����ڣ��Զ�����ֿ⣬���سɹ� 
	 * false ����ʱ���������м�¼���ֿ��¼�����ڣ��׳��쳣����־�ᱣ����Ĭ��false
	 */
	private boolean isAutoInsert; // ����ʱ���������м�¼���ֿ��¼�����ڣ��Ƿ��Զ�����ֿ⡣���򱨴�

	/**
	 * Ĭ��true����slaveʧ��ֱ���жϣ����ٽ��к�������slave�ĸ��ơ�������־���¼�������������������ԣ���ʱ����isRetryOnFail��
	 * �������Ϊfalse����ô��ǰslaveʧ�ܣ���������slave��������ơ��Ƿ�Ҫ������־����isRetryOnFail����
	 */
	private boolean isBreakOnFail = true; //��ǰslaveĿ��ͬ��ʧ�ܣ��Ƿ�ֱ���жϣ����ٽ��к�������slave�ĸ��ơ�

	/**
	 * Ĭ��true����ǰslaveĿ��ͬ��ʧ�ܺ󣬱�����־���¼��������������������
	 * �������Ϊfalse����ô����slave���ɹ����߶���Ҫ������־����־��ֱ��ɾ���ˡ�������slave����θ��½�����Զ��ʧ��
	 * �������Ϊfalse, ������slaveʧ����Ҫ������־ʱ��������������Ȼ��˳�����Ա�slave
	 */
	private boolean isRetryOnFail = true;//��ǰslaveĿ��ͬ��ʧ�ܣ��Ƿ�ͨ���������������ԣ�Ҳ���Ƿ�����־���¼��
	
	private Map<String/*columnName*/,Object/*���µ�nullʱ�Զ����ɵ�Ĭ��ֵ*/> defaultNullValues = Collections.EMPTY_MAP;
	private Map<String/*columnName*/, Long[]/*0:��Сֵ��1�����ֵ*/> columRanges = Collections.EMPTY_MAP;

	/**
     * ��������null��һ����ʱ������������Զ�תΪָ����Ĭ��ֵ����������ֻ֧����ֵlong��String,���磺
	 * sku_id:0,item_id:65,seller_id:63
	 * sku_id:0,item_id:65,name:'aaa'
	 */
	public void setDefaultValuesOnNull(String defaultValuesOnNull) {
		String[] cols = defaultValuesOnNull.split(",");
		Map<String, Object> colvalues = new HashMap<String, Object>(cols.length);
		for (int i = 0; i < cols.length; i++) {
			String col = cols[i];
			String[] nv = col.split("\\:");
			if (nv[1].startsWith("'") && nv[1].endsWith("'")) {
				colvalues.put(nv[0], nv[1].substring(1, nv[1].length() - 1));// �ַ���
			} else {
				colvalues.put(nv[0], Long.parseLong(nv[1]));// ����
			}
		}
		this.defaultNullValues = colvalues;
	}

	/**
     * �����������ֵ�һ����ʱ��������������Ʒ�Χ��������Χ�Զ�תΪ�߽�ֵ����������ֻ֧����ֵlong��String,���磺
	 * sku_id:0_8,item_id:_65,seller_id:0_
	 */
	public void setColumRestrictRanges(String defaultValuesOnNull) {
		String[] cols = defaultValuesOnNull.split(",");
		Map<String, Long[]> colRanges = new HashMap<String, Long[]>(cols.length);
		for (int i = 0; i < cols.length; i++) {
			String col = cols[i];
			String[] nv = col.split("\\:");
			String[] range = nv[1].split("_");
			Long min = null, max = null;
			if (range.length == 1) {
				min = Long.parseLong(range[0].trim());
			} else {
				if (!"".equals(range[0].trim())) {
					min = Long.parseLong(range[0].trim());
				}
				if (!"".equals(range[1].trim())) {
					max = Long.parseLong(range[1].trim());
				}
			}
			colRanges.put(nv[0], new Long[] { min, max });
		}
		this.columRanges = colRanges;
	}
	
	public Map<String, Long[]> getColumRanges() {
		return columRanges;
	}

	public Map<String, Object> getDefaultNullValues() {
		return defaultNullValues;
	}
	
	/*public Long getRestrictValue(Number value){
		
	}*/
	
	public Object changeToDefaultOnNull(String colName, Object value) {
		if (defaultNullValues == null) {
			return value;
		}
		if (value != null) {
			return value;
		}
		return defaultNullValues.get(colName);
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Slave {").append("\n");
		buffer.append("name: ").append(name).append("\n");
		buffer.append("dbType: ").append(dbType).append("\n");
		buffer.append("dataSourceName: ").append(dataSourceName).append("\n");
		if (databaseShardColumn != null) {
			buffer.append("databaseShardColumn: ").append(databaseShardColumn).append("\n");
		}
		if (tableShardColumn != null) {
			buffer.append("tableShardColumn: ").append(tableShardColumn).append("\n");
		}
		if (columns != null) {
			buffer.append("columns: ").append(Arrays.asList(columns)).append("\n");
		}
		buffer.append("}").append("\n");

		return buffer.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name.toLowerCase();
	}

	public DBType getDbType() {
		return dbType;
	}

	public String getType() {
		return dbType == null ? null : dbType.toString();
	}

	public void setType(String type) {
		if (type != null) {
			type = type.toUpperCase();
		}
		this.dbType = DBType.valueOf(type);
	}

	public String getDataSourceName() {
		return dataSourceName;
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	public String getDatabaseShardColumn() {
		return databaseShardColumn;
	}

	public void setDatabaseShardColumn(String databaseShardColumn) {
		this.databaseShardColumn = databaseShardColumn.toLowerCase();
	}

	public String getTableShardColumn() {
		return tableShardColumn;
	}

	public void setTableShardColumn(String tableShardColumn) {
		this.tableShardColumn = tableShardColumn.toLowerCase();
	}

	public String[] getColumns() {
		return columns;
	}

	public void setColumns(String[] columns) {
		this.columns = columns;
		for (int i = 0; i < this.columns.length; i++) {
			this.columns[i] = this.columns[i].toLowerCase();
		}
	}

	public void setCommaSeparatedColumns(String commaSeparatedColumns) {
		this.columns = commaSeparatedColumns.split(",");
		for (int i = 0; i < this.columns.length; i++) {
			this.columns[i] = this.columns[i].toLowerCase();
		}
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate slaveJdbcTemplate) {
		this.jdbcTemplate = slaveJdbcTemplate;
	}

	public SlaveReplicater getSlaveReplicater() {
		return slaveReplicater;
	}

	public void setSlaveReplicater(SlaveReplicater slaveReplicater) {
		this.slaveReplicater = slaveReplicater;
	}

	public String getSlaveReplicaterName() {
		return slaveReplicaterName;
	}

	public void setSlaveReplicaterName(String slaveReplicaterName) {
		this.slaveReplicaterName = slaveReplicaterName;
	}

	public boolean isDisableUpdate() {
		return isDisableUpdate;
	}

	public void setDisableUpdate(boolean isDisableUpdate) {
		this.isDisableUpdate = isDisableUpdate;
	}

	public boolean isDisableInsert() {
		return isDisableInsert;
	}

	public void setDisableInsert(boolean isDisableInsert) {
		this.isDisableInsert = isDisableInsert;
	}

	/**
	 * TODO:NO sync version����Ҫ�ܹ����վ��������Ƿ���Ҫsync_version.
	 * 
	 * @return
	 */
	public boolean isNoSyncVersion() {
		return isNoSyncVersion;
	}

	public void setNoSyncVersion(boolean isNoSyncVersion) {
		this.isNoSyncVersion = isNoSyncVersion;
	}

	public boolean isAutoInsert() {
		return isAutoInsert;
	}

	public void setAutoInsert(boolean isAutoInsert) {
		this.isAutoInsert = isAutoInsert;
	}

	public boolean isBreakOnFail() {
		return isBreakOnFail;
	}

	public void setBreakOnFail(boolean isBreakOnFail) {
		this.isBreakOnFail = isBreakOnFail;
	}

	public boolean isRetryOnFail() {
		return isRetryOnFail;
	}

	public void setRetryOnFail(boolean isRetryOnFail) {
		this.isRetryOnFail = isRetryOnFail;
	}

	public boolean isAllowSync() {
		return allowSync;
	}

	public void setAllowSync(boolean allowSync) {
		this.allowSync = allowSync;
	}

	public SlaveDataHandler getSlaveDataHandler() {
		return slaveDataHandler;
	}

	public void setSlaveDataHandler(SlaveDataHandler slaveDataHandler) {
		this.slaveDataHandler = slaveDataHandler;
	}

	public static void main(String[] args) {
		System.out.println(Arrays.toString("_8".split("_")));
		System.out.println(Arrays.toString("0_".split("_")));
		System.out.println(Arrays.toString("0_9".split("_")));
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.taobao.tddl.common.DataSourceChangeListener;
import com.taobao.tddl.common.TDDLConstant;
import com.taobao.tddl.common.util.DataSourceFetcher;
import com.taobao.tddl.common.util.TDDLMBeanServer;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.jdbc.group.config.ConfigManager;
import com.taobao.tddl.jdbc.group.dbselector.DBSelector;
import com.taobao.tddl.jdbc.group.exception.TGroupDataSourceException;

/**
 * TGroupDataSource����������������ʾ��������һ��DataSource��
 * ����ָTGroupDataSource�ڲ�����һ��(>=1��)ͬ�������ݿ⣬
 * ��һ�����ݿ�Ĳ�ͬ�����в�ͬ�Ķ�д���ȼ���Ȩ�أ�
 * ����д����ʱҲֻ�ǰ���д���ȼ���Ȩ�ض����е�һ�����ݿ������
 * �����һ�����ݿ��дʧ���ˣ��ٳ�����һ�����ݿ⣬
 * �����һ�����ݿ��д�ɹ��ˣ�ֱ�ӷ��ؽ����Ӧ�ò㣬
 * �������ݿ��ͬ�������ɵײ����ݿ��ڲ���ɣ�
 * TGroupDataSource����������ͬ����
 *
 * ʹ��TGroupDataSource�Ĳ���:
 * <pre>
 *      TGroupDataSource tGroupDataSource = new TGroupDataSource();
 *      tGroupDataSource.setDbGroupKey("myDbGroup");
 *      //......��������setter
 *      tGroupDataSource.init();
 *      tGroupDataSource.getConnection();
 * </pre>
 *
 *
 * @author yangzhu
 * @author linxuan
 *
 */
public class TGroupDataSource implements DataSource {
	private ConfigManager configManager;

	/**
	 * ��������Ϊһ�飬֧�ֱ�������
	 */
	private String dsKeyAndWeightCommaArray;
	private DataSourceFetcher dataSourceFetcher;
	private DBType dbType = DBType.MYSQL;

	public TGroupDataSource() {
	}

	public TGroupDataSource(String dbGroupKey, String appName) {
		this.dbGroupKey = dbGroupKey;
		this.appName = appName;
	}

	/**
	 * ����dbGroupKey��appName����ʼ�����TAtomDataSource
	 *
	 * @throws com.taobao.tddl.jdbc.group.exception.ConfigException
	 */
	public void init() {
		if (dsKeyAndWeightCommaArray != null) {
			//�������÷�ʽ��dsKeyAndWeightCommaArray + dataSourceFetcher + dyType
			DataSourceFetcher wrapper = new DataSourceFetcher() {
				@Override
				public DataSource getDataSource(String key) {
					return dataSourceFetcher.getDataSource(key);
				}

				@Override
				public DBType getDataSourceDBType(String key) {
					DBType type = dataSourceFetcher.getDataSourceDBType(key);
					return type == null ? dbType : type; //���dataSourceFetcherûdbType����tgds��dbType
				}
			};
			List<DataSourceWrapper> dss = ConfigManager.buildDataSourceWrapper(dsKeyAndWeightCommaArray, wrapper);
			init(dss);
		} else {
			checkProperties();
			configManager = new ConfigManager(this);
			configManager.init();
		}
	}

	public void init(DataSourceWrapper... dataSourceWrappers) {
		init(Arrays.asList(dataSourceWrappers));
	}

	public void init(List<DataSourceWrapper> dataSourceWrappers) {
		configManager = new ConfigManager(this);
		configManager.init(dataSourceWrappers);
	}

	public static TGroupDataSource build(String groupKey, String dsWeights, DataSourceFetcher fetcher) {
		List<DataSourceWrapper> dss = ConfigManager.buildDataSourceWrapper(dsWeights, fetcher);
		TGroupDataSource tGroupDataSource = new TGroupDataSource();
		tGroupDataSource.setDbGroupKey(groupKey);
		tGroupDataSource.init(dss);
		return tGroupDataSource;
	}

	/**
	 * ����������TAtomDataSource��������dbGroupKey��appName�������Ե�ֵ�Ƿ�Ϸ�
	 */
	private void checkProperties() {
		if (dbGroupKey == null)
			throw new TGroupDataSourceException("dbGroupKey����Ϊnull");
		dbGroupKey = dbGroupKey.trim();
		if (dbGroupKey.length() < 1)
			throw new TGroupDataSourceException("dbGroupKey�ĳ���Ҫ����0��ǰ���հ׺�β���հײ�������");

		if (appName == null)
			throw new TGroupDataSourceException("appName����Ϊnull");
		appName = appName.trim();
		if (appName.length() < 1)
			throw new TGroupDataSourceException("appName�ĳ���Ҫ����0��ǰ���հ׺�β���հײ�������");
	}

	/**
	 * Σ�սӿڡ�һ�����ڲ��ԡ�Ӧ��Ҳ����ֱ��ͨ���ýӿ���������Դ����
	 */
	public void resetDbGroup(String configInfo) {
		configManager.resetDbGroup(configInfo);
	}

	//�����ʼ��𣬵����߲��ܻ��棬�����ʧȥ��̬��
	DBSelector getDBSelector(boolean isRead) {
		return configManager.getDBSelector(isRead, this.autoSelectWriteDataSource);
	}

	/* ========================================================================
	 * �����Ǳ�����ǰд���������ĸ�����ִ�е�, ����������־�����ĳ���
	 * ======================================================================*/
	private static ThreadLocal<DataSourceWrapper> targetThreadLocal;

	/**
	 * ͨ��springע���ֱ�ӵ��ø÷����������ر�Ŀ����¼
	 */
	public void setTracerWriteTarget(boolean isTraceTarget) {
		if (isTraceTarget) {
			if (targetThreadLocal == null) {
				targetThreadLocal = new ThreadLocal<DataSourceWrapper>();
			}
		} else {
			targetThreadLocal = null;
		}
	}

	/**
	 * ��ִ����д�����󣬵��øķ�����õ�ǰ�߳�д���������ĸ�����Դִ�е�
	 * ��ȡ���Զ��������
	 */
	public DataSourceWrapper getCurrentTarget() {
		if (targetThreadLocal == null) {
			return null;
		}
		DataSourceWrapper dsw = targetThreadLocal.get();
		targetThreadLocal.remove();
		return dsw;
	}

	/**
	 * ���ε��ø÷�������Ŀ���
	 */
	void setWriteTarget(DataSourceWrapper dsw) {
		if (targetThreadLocal != null) {
			targetThreadLocal.set(dsw);
		}
	}

	/* ========================================================================
	 * ��������API
	 * ======================================================================*/
//��ConfigManager�����ǽ�������Ϣ���շ�װΪ��дDBSelector��Ҫ�õ���dbKey��DataSource��ӳ�䣬��DBSelector�е���Ϣ���������
	public Map<String, DataSource> getDataSourceMap() {
		Map<String, DataSource> dsMap = new LinkedHashMap<String, DataSource>();
		dsMap.putAll(this.getDBSelector(true).getDataSources());
		dsMap.putAll(this.getDBSelector(false).getDataSources());
		return dsMap;
	}

	public Map<String, DataSource> getDataSourcesMap(boolean isRead) {
		return this.getDBSelector(isRead).getDataSources();
	}

	public void setDataSourceChangeListener(DataSourceChangeListener dataSourceChangeListener) {
		this.configManager.setDataSourceChangeListener(dataSourceChangeListener);
	}

	/* ========================================================================
	 * ������javax.sql.DataSource��APIʵ��
	 * ======================================================================*/

	public TGroupConnection getConnection() throws SQLException {
		return new TGroupConnection(this);
	}

	public TGroupConnection getConnection(String username, String password) throws SQLException {
		return new TGroupConnection(this, username, password);
	}

	//���������ֶε�����ʵ�ʵ�DataSourceʱ���봫�ݹ�ȥ

	//jdbc�淶: DataSource�ս���ʱLogWriterΪnull
	private PrintWriter out = null;

	public PrintWriter getLogWriter() throws SQLException {
		return out;
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		this.out = out;
	}

	//jdbc�淶: DataSource�ս���ʱLoginTimeoutΪ0
	private int seconds = 0;

	public int getLoginTimeout() throws SQLException {
		return seconds;
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		this.seconds = seconds;
	}

	public static void setShutDownMBean(boolean shutDownMBean) {
		TDDLMBeanServer.shutDownMBean=shutDownMBean;
	}

	////////////////////////////////////////////////////////////////////////////
	/**
	 * ���߼���getter/setter
	 */

	private String appName;

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	private String dbGroupKey;

	public String getDbGroupKey() {
		return dbGroupKey;
	}

	private String fullDbGroupKey = null;

	public String getFullDbGroupKey() {
		if (fullDbGroupKey == null)
			fullDbGroupKey = PREFIX + getDbGroupKey();
		return fullDbGroupKey;
	}

	public String getDbGroupExtraConfigKey(){
		return EXTRA_PREFIX+getDbGroupKey()+"."+getAppName();
	}

	public void setDbGroupKey(String dbGroupKey) {
		this.dbGroupKey = dbGroupKey;
	}

	private int retryingTimes = 3; //Ĭ�϶�дʧ��ʱ����3��

	public int getRetryingTimes() {
		return retryingTimes;
	}

	public void setRetryingTimes(int retryingTimes) {
		this.retryingTimes = retryingTimes;
	}

	private long configReceiveTimeout = TDDLConstant.DIAMOND_GET_DATA_TIMEOUT; //ȡ������Ϣ��Ĭ�ϳ�ʱʱ��Ϊ30��

	public long getConfigReceiveTimeout() {
		return configReceiveTimeout;
	}

	public void setConfigReceiveTimeout(long configReceiveTimeout) {
		this.configReceiveTimeout = configReceiveTimeout;
	}

	public void setDsKeyAndWeightCommaArray(String dsKeyAndWeightCommaArray) {
		this.dsKeyAndWeightCommaArray = dsKeyAndWeightCommaArray;
	}

	//�������ڼ����������л�ʱ�Ƿ���Ҫ���ҵ�һ����д�Ŀ�
	private boolean autoSelectWriteDataSource = false;

	public boolean getAutoSelectWriteDataSource() {
		return autoSelectWriteDataSource;
	}

	public void setAutoSelectWriteDataSource(boolean autoSelectWriteDataSource) {
		this.autoSelectWriteDataSource = autoSelectWriteDataSource;
	}

	public void setDataSourceFetcher(DataSourceFetcher dataSourceFetcher) {
		this.dataSourceFetcher = dataSourceFetcher;
	}

	public void setDbType(DBType dbType) {
		this.dbType = dbType;
	}

	private static String VERSION = "2.4.1";
	private static String PREFIX = "com.taobao.tddl.jdbc.group_V" + VERSION + "_";
	private static String EXTRA_PREFIX = "com.taobao.tddl.jdbc.extra_config.group_V" + VERSION + "_";

	public static String getFullDbGroupKey(String dbGroupKey) {
		return PREFIX + dbGroupKey;
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
}

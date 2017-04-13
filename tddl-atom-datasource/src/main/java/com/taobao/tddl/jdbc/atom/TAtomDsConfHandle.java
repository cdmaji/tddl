/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.atom;

import java.sql.SQLException;import java.util.List;import java.util.Map;import java.util.concurrent.locks.ReentrantLock;import javax.sql.DataSource;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import com.taobao.datasource.LocalTxDataSourceDO;import com.taobao.datasource.TaobaoDataSourceFactory;import com.taobao.datasource.resource.adapter.jdbc.local.LocalTxDataSource;import com.taobao.tddl.common.Monitor;import com.taobao.tddl.common.config.ConfigDataListener;import com.taobao.tddl.common.util.TStringUtil;import com.taobao.tddl.jdbc.atom.common.TAtomConURLTools;import com.taobao.tddl.jdbc.atom.common.TAtomConfParser;import com.taobao.tddl.jdbc.atom.common.TAtomConstants;import com.taobao.tddl.jdbc.atom.config.DbConfManager;import com.taobao.tddl.jdbc.atom.config.DbPasswdManager;import com.taobao.tddl.jdbc.atom.config.DiamondDbConfManager;import com.taobao.tddl.jdbc.atom.config.DiamondDbPasswdManager;import com.taobao.tddl.jdbc.atom.config.object.AtomDbStatusEnum;import com.taobao.tddl.jdbc.atom.config.object.AtomDbTypeEnum;import com.taobao.tddl.jdbc.atom.config.object.TAtomDsConfDO;import com.taobao.tddl.jdbc.atom.exception.AtomAlreadyInitException;import com.taobao.tddl.jdbc.atom.exception.AtomIllegalException;import com.taobao.tddl.jdbc.atom.exception.AtomInitialException;import com.taobao.tddl.jdbc.atom.jdbc.TDataSourceWrapper;import com.taobao.tddl.jdbc.atom.listener.TAtomDbStatusListener;
/**
 * ���ݿ⶯̬�л���Handle�࣬�������ݿ�Ķ�̬�л� ��������������
 * 
 * @author qihao
 * 
 */
class TAtomDsConfHandle {
	private static Log logger = LogFactory.getLog(TAtomDsConfHandle.class);

	private String appName;

	private String dbKey;

	/**
	 * ����ʱ����
	 */
	private volatile TAtomDsConfDO runTimeConf = new TAtomDsConfDO();

	/**
	 * �������ã����������͵Ķ�̬����
	 */
	private TAtomDsConfDO localConf = new TAtomDsConfDO();

	/**
	 * ȫ�����ã�Ӧ�����ö��Ĺ���
	 */
	private DbConfManager dbConfManager;

	/**
	 * �������ö��Ĺ���
	 */
	private DbPasswdManager dbPasswdManager;

	/**
	 * Jboss����Դͨ��init��ʼ��
	 */
	private volatile LocalTxDataSource jbossDataSource;

	/**
	 * ���ݿ�״̬�ı�ص�
	 */
	private volatile List<TAtomDbStatusListener> dbStatusListeners;

	/**
	 * ��ʼ�����Ϊһ����ʼ���������б��ص����ý�ֹ�Ķ�
	 */
	private volatile boolean initFalg;

	/**
	 * ����Դ������������Ҫ������Դ�����ؽ�����ˢ��ʱ��Ҫ�Ȼ�ø���
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * ��ʼ��������������Ӧ������Դ��ֻ�ܱ�����һ��
	 * 
	 * @throws Exception
	 */
	protected void init() throws Exception {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] double call Init !");
		}
		// 1.��ʼ���������
		if (TStringUtil.isBlank(this.appName) || TStringUtil.isBlank(this.dbKey)) {
			String errorMsg = "[attributeError] TAtomDatasource of appName Or dbKey is Empty !";
			logger.error(errorMsg);
			throw new AtomIllegalException(errorMsg);
		}
		// 2.����dbConfManager
		DiamondDbConfManager defaultDbConfManager = new DiamondDbConfManager();
		defaultDbConfManager.setGlobalConfigDataId(TAtomConstants
				.getGlobalDataId(this.dbKey));
		defaultDbConfManager.setAppConfigDataId(TAtomConstants.getAppDataId(
				this.appName, this.dbKey));
		// ��ʼ��dbConfManager
		defaultDbConfManager.init();
		dbConfManager = defaultDbConfManager;
		// 3.��ȡȫ������
		String globaConfStr = dbConfManager.getGlobalDbConf();
		// ע��ȫ�����ü���
		registerGlobaDbConfListener(defaultDbConfManager);
		if (TStringUtil.isBlank(globaConfStr)) {
			String errorMsg = "[ConfError] read globalConfig is Empty !";
			logger.error(errorMsg);
			throw new AtomInitialException(errorMsg);
		}
		// 4.��ȡӦ������
		String appConfStr = dbConfManager.getAppDbDbConf();
		// ע��Ӧ�����ü���
		registerAppDbConfListener(defaultDbConfManager);
		if (TStringUtil.isBlank(appConfStr)) {
			String errorMsg = "[ConfError] read appConfig is Empty !";
			logger.error(errorMsg);
			throw new AtomInitialException(errorMsg);
		}
		lock.lock();
		try {
			// 5.��������string��TAtomDsConfDO
			runTimeConf = TAtomConfParser.parserTAtomDsConfDO(globaConfStr,
					appConfStr);
			// 6.��������������
			overConfByLocal(localConf, runTimeConf);
			// 7.���û�����ñ������룬���ö������룬��ʼ��passwdManager
			if (TStringUtil.isBlank(this.runTimeConf.getPasswd())) {
				// ���dbKey�Ͷ�Ӧ��userName�Ƿ�Ϊ��
				if (TStringUtil.isBlank(runTimeConf.getUserName())) {
					String errorMsg = "[attributeError] TAtomDatasource of UserName is Empty !";
					logger.error(errorMsg);
					throw new AtomIllegalException(errorMsg);
				}
				DiamondDbPasswdManager diamondDbPasswdManager = new DiamondDbPasswdManager();
				diamondDbPasswdManager.setPasswdConfDataId(TAtomConstants
						.getPasswdDataId(runTimeConf.getDbName(),
								runTimeConf.getDbType(),
								runTimeConf.getUserName()));
				diamondDbPasswdManager.init();
				dbPasswdManager = diamondDbPasswdManager;
				// ��ȡ����
				String passwd = dbPasswdManager.getPasswd();
				registerPasswdConfListener(diamondDbPasswdManager);
				if (TStringUtil.isBlank(passwd)) {
					String errorMsg = "[PasswdError] read passwd is Empty !";
					logger.error(errorMsg);
					throw new AtomInitialException(errorMsg);
				}
				runTimeConf.setPasswd(passwd);
			}
			// 8.ת��tAtomDsConfDO
			LocalTxDataSourceDO localTxDataSourceDO = convertTAtomDsConf2JbossConf(
					this.runTimeConf,
					TAtomConstants.getDbNameStr(this.appName, this.dbKey));
			// 9.������������������ȷֱ���׳��쳣
			if (!checkLocalTxDataSourceDO(localTxDataSourceDO)) {
				String errorMsg = "[ConfigError]init dataSource Prams Error! config is : "
						+ localTxDataSourceDO.toString();
				logger.error(errorMsg);
				throw new AtomInitialException(errorMsg);
			}
			// 10.��������Դ
			// �ر�TB-DATASOURCE��JMXע��
			localTxDataSourceDO.setUseJmx(false);
			LocalTxDataSource localTxDataSource = TaobaoDataSourceFactory
					.createLocalTxDataSource(localTxDataSourceDO);
			// 11.�������õ�����Դ��ָ��TAtomDatasource��
			this.jbossDataSource = localTxDataSource;
			clearDataSourceWrapper();
			initFalg = true;
		} finally {
			lock.unlock();
		}
	}

	private void clearDataSourceWrapper() {
		Monitor.removeSnapshotValuesCallback(wrapDataSource);
		wrapDataSource = null;
	}

	/**
	 * ע������仯������
	 * 
	 * @param dbPasswdManager
	 */
	private void registerPasswdConfListener(DbPasswdManager dbPasswdManager) {
		dbPasswdManager.registerPasswdConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[Passwd HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data ||TStringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String localPasswd = TAtomDsConfHandle.this.localConf
							.getPasswd();
					if (TStringUtil.isNotBlank(localPasswd)) {
						// �������������passwdֱ�ӷ��ز�֧�ֶ�̬�޸�
						return;
					}
					String newPasswd = TAtomConfParser.parserPasswd(data);
					String runPasswd = TAtomDsConfHandle.this.runTimeConf
							.getPasswd();
					if (!TStringUtil.equals(runPasswd, newPasswd)) {
						TAtomDsConfHandle.this.jbossDataSource
								.setPassword(newPasswd);
						try {
							// ��������Դ
							TAtomDsConfHandle.this.flushDataSource();
							// �����µ����ø�������ʱ������
							TAtomDsConfHandle.this.runTimeConf
									.setPasswd(newPasswd);
						} catch (Exception e) {
							logger.error(
									"[Flsh Passwd Error] flush dataSource Error !",
									e);
						}
					}
				} finally {
					lock.unlock();
				}
			}
		});
	}

	/**
	 * ȫ�����ü���,ȫ�����÷����仯�� ��Ҫ����FLUSH����Դ
	 * 
	 * @param defaultDbConfManager
	 */
	private void registerGlobaDbConfListener(DbConfManager dbConfManager) {
		dbConfManager.registerGlobaDbConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[GlobaConf HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || TStringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String globaConfStr = data;
					// �����ȫ�����÷����仯��������IP,PORT,DBNAME,DBTYPE,STATUS
					TAtomDsConfDO tmpConf = TAtomConfParser
							.parserTAtomDsConfDO(globaConfStr, null);
					TAtomDsConfDO newConf = TAtomDsConfHandle.this.runTimeConf
							.clone();
					// �������͵����ã����ǵ�ǰ������
					newConf.setIp(tmpConf.getIp());
					newConf.setPort(tmpConf.getPort());
					newConf.setDbName(tmpConf.getDbName());
					newConf.setDbType(tmpConf.getDbType());
					newConf.setDbStatus(tmpConf.getDbStatus());
					// ��������������
					overConfByLocal(TAtomDsConfHandle.this.localConf, newConf);
					// ������͹��������ݿ�״̬�� RW/R->NA,ֱ�����ٵ�����Դ������ҵ���߼���������
					if (AtomDbStatusEnum.NA_STATUS != TAtomDsConfHandle.this.runTimeConf
							.getDbStautsEnum()
							&& AtomDbStatusEnum.NA_STATUS == tmpConf
									.getDbStautsEnum()) {
						try {
							TAtomDsConfHandle.this.jbossDataSource.destroy();
							logger.warn("[NA STATUS PUSH] destroy DataSource !");
						} catch (Exception e) {
							logger.error(
									"[NA STATUS PUSH] destroy DataSource  Error!",
									e);
						}
					} else {
						// ת��tAtomDsConfDO
						LocalTxDataSourceDO localTxDataSourceDO = convertTAtomDsConf2JbossConf(
								newConf, TAtomConstants.getDbNameStr(
										TAtomDsConfHandle.this.appName,
										TAtomDsConfHandle.this.dbKey));
						// ���ת�������Ƿ���ȷ
						if (!checkLocalTxDataSourceDO(localTxDataSourceDO)) {
							logger.error("[GlobaConfError] dataSource Prams Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						// ������͵�״̬ʱ NA->RW/R ʱ��Ҫ���´�������Դ��������ˢ��
						if (TAtomDsConfHandle.this.runTimeConf
								.getDbStautsEnum() == AtomDbStatusEnum.NA_STATUS
								&& (newConf.getDbStautsEnum() == AtomDbStatusEnum.RW_STATUS || newConf
										.getDbStautsEnum() == AtomDbStatusEnum.R_STAUTS)) {
							// ��������Դ
							try {
								// �ر�TB-DATASOURCE��JMXע��
								localTxDataSourceDO.setUseJmx(false);
								LocalTxDataSource localTxDataSource = TaobaoDataSourceFactory
										.createLocalTxDataSource(localTxDataSourceDO);
								TAtomDsConfHandle.this.jbossDataSource = localTxDataSource;
								logger.warn("[NA->RW/R STATUS PUSH] ReCreate DataSource !");
							} catch (Exception e) {
								logger.error(
										"[NA->RW/R STATUS PUSH] ReCreate DataSource Error!",
										e);
							}
						} else {
							boolean needFlush = checkGlobaConfChange(
									TAtomDsConfHandle.this.runTimeConf, newConf);
							// ������������ñ仯�Ƿ���Ҫ�ؽ�����Դ
							if (needFlush) {
								TAtomDsConfHandle.this.jbossDataSource
										.setConnectionURL(localTxDataSourceDO
												.getConnectionURL());
								TAtomDsConfHandle.this.jbossDataSource
										.setDriverClass(localTxDataSourceDO
												.getDriverClass());
								TAtomDsConfHandle.this.jbossDataSource
										.setExceptionSorterClassName(localTxDataSourceDO
												.getExceptionSorterClassName());
								try {
									// ��������Դ
									TAtomDsConfHandle.this.flushDataSource();
								} catch (Exception e) {
									logger.error(
											"[Flsh GlobaConf Error] flush dataSource Error !",
											e);
								}
							}
						}
					}
					//�������ݿ�״̬������
					processDbStatusListener(TAtomDsConfHandle.this.runTimeConf.getDbStautsEnum(),
							newConf.getDbStautsEnum());
					//�����µ����ø�������ʱ������
					TAtomDsConfHandle.this.runTimeConf = newConf;
					clearDataSourceWrapper();
				} finally {
					lock.unlock();
				}
			}

			private boolean checkGlobaConfChange(TAtomDsConfDO runConf,
					TAtomDsConfDO newConf) {
				boolean needFlush = false;
				if (!TStringUtil.equals(runConf.getIp(), newConf.getIp())) {
					needFlush = true;
					return needFlush;
				}
				if (!TStringUtil.equals(runConf.getPort(), newConf.getPort())) {
					needFlush = true;
					return needFlush;
				}
				if (!TStringUtil.equals(runConf.getDbName(), newConf.getDbName())) {
					needFlush = true;
					return needFlush;
				}
				if (runConf.getDbTypeEnum() != newConf.getDbTypeEnum()) {
					needFlush = true;
					return needFlush;
				}
				return needFlush;
			}
		});
	}

	/**
	 * Ӧ�����ü�������Ӧ�����÷����仯ʱ�����ַ��� �仯�����ã�������������flush����reCreate
	 * 
	 * @param defaultDbConfManager
	 */
	private void registerAppDbConfListener(DbConfManager dbConfManager) {
		dbConfManager.registerAppDbConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[AppConf HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || TStringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String appConfStr = data;
					TAtomDsConfDO tmpConf = TAtomConfParser
							.parserTAtomDsConfDO(null, appConfStr);
					TAtomDsConfDO newConf = TAtomDsConfHandle.this.runTimeConf
							.clone();
					// ��Щ�������ò��ܱ�������Կ�¡�ϵ����ã�Ȼ���µ�set��ȥ
					newConf.setUserName(tmpConf.getUserName());
					newConf.setMinPoolSize(tmpConf.getMinPoolSize());
					newConf.setMaxPoolSize(tmpConf.getMaxPoolSize());
					newConf.setIdleTimeout(tmpConf.getIdleTimeout());
					newConf.setBlockingTimeout(tmpConf.getBlockingTimeout());
					newConf.setPreparedStatementCacheSize(tmpConf
							.getPreparedStatementCacheSize());
					newConf.setConnectionProperties(tmpConf
							.getConnectionProperties());
					newConf.setOracleConType(tmpConf.getOracleConType());
					// ����3�������ʵ��
					newConf.setWriteRestrictTimes(tmpConf
							.getWriteRestrictTimes());
					newConf.setReadRestrictTimes(tmpConf.getReadRestrictTimes());
					newConf.setThreadCountRestrict(tmpConf
							.getThreadCountRestrict());
					newConf.setTimeSliceInMillis(tmpConf.getTimeSliceInMillis());
					// ��������������
					overConfByLocal(TAtomDsConfHandle.this.localConf, newConf);
					// ת��tAtomDsConfDO
					LocalTxDataSourceDO localTxDataSourceDO = convertTAtomDsConf2JbossConf(
							newConf, TAtomConstants.getDbNameStr(
									TAtomDsConfHandle.this.appName,
									TAtomDsConfHandle.this.dbKey));
					// ���ת�������Ƿ���ȷ
					if (!checkLocalTxDataSourceDO(localTxDataSourceDO)) {
						logger.error("[GlobaConfError] dataSource Prams Error! dataId : "
								+ dataId + " config : " + data);
						return;
					}
					boolean isNeedReCreate = isNeedReCreate(
							TAtomDsConfHandle.this.runTimeConf, newConf);
					if (isNeedReCreate) {
						try {
							TAtomDsConfHandle.this.jbossDataSource.destroy();
							logger.warn("[destroy OldDataSource] dataId : "
									+ dataId);
							LocalTxDataSource localTxDataSource = TaobaoDataSourceFactory
									.createLocalTxDataSource(localTxDataSourceDO);
							logger.warn("[create newDataSource] dataId : "
									+ dataId);
							TAtomDsConfHandle.this.jbossDataSource = localTxDataSource;
							clearDataSourceWrapper();
							TAtomDsConfHandle.this.runTimeConf = newConf;
						} catch (Exception e) {
							logger.error(
									"[Flsh AppConf Error] reCreate dataSource Error ! dataId: "
											+ dataId, e);
						}
					} else {
						boolean isNeedFlush = isNeedFlush(
								TAtomDsConfHandle.this.runTimeConf, newConf);
						/**
						 * ��ֵ�仯����ˢ�³��е�����Դ��ֻҪ����runTimeConf���������wrapDataSource
						 */
						boolean isRestrictChange = isRestrictChange(
								TAtomDsConfHandle.this.runTimeConf, newConf);
						if (isNeedFlush) {
							TAtomDsConfHandle.this.jbossDataSource
									.setConnectionURL(localTxDataSourceDO
											.getConnectionURL());
							TAtomDsConfHandle.this.jbossDataSource
									.setUserName(localTxDataSourceDO
											.getUserName());
							try {
								// ��������Դ
								TAtomDsConfHandle.this.flushDataSource();
								// �����µ����ø�������ʱ������
								TAtomDsConfHandle.this.runTimeConf = newConf;
								clearDataSourceWrapper();
							} catch (Exception e) {
								logger.error(
										"[Flash GlobaConf Error] flush dataSource Error !",
										e);
							}
						} else if (isRestrictChange) {
							TAtomDsConfHandle.this.runTimeConf = newConf;
							clearDataSourceWrapper();
						}
					}
				} finally {
					lock.unlock();
				}
			}

			private boolean isNeedReCreate(TAtomDsConfDO runConf,
					TAtomDsConfDO newConf) {
				boolean needReCreate = false;
				if (AtomDbTypeEnum.ORACLE == newConf.getDbTypeEnum()) {
					Map<String, String> newProp = newConf
							.getConnectionProperties();
					Map<String, String> runProp = runConf
							.getConnectionProperties();
					if (!runProp.equals(newProp)) {
						return true;
					}
				}
				if (runConf.getMinPoolSize() != newConf.getMinPoolSize()) {
					return true;
				}
				if (runConf.getMaxPoolSize() != newConf.getMaxPoolSize()) {
					return true;
				}
				if (runConf.getBlockingTimeout() != newConf
						.getBlockingTimeout()) {
					return true;
				}
				if (runConf.getIdleTimeout() != newConf.getIdleTimeout()) {
					return true;
				}
				if (runConf.getPreparedStatementCacheSize() != newConf
						.getPreparedStatementCacheSize()) {
					return true;
				}
				return needReCreate;
			}

			private boolean isNeedFlush(TAtomDsConfDO runConf,
					TAtomDsConfDO newConf) {
				boolean needFlush = false;
				if (AtomDbTypeEnum.MYSQL == newConf.getDbTypeEnum()) {
					Map<String, String> newProp = newConf
							.getConnectionProperties();
					Map<String, String> runProp = runConf
							.getConnectionProperties();
					if (!runProp.equals(newProp)) {
						return true;
					}
				}
				if (!TStringUtil.equals(runConf.getUserName(),
						newConf.getUserName())) {
					return true;
				}
				if (!TStringUtil.equals(runConf.getPasswd(), newConf.getPasswd())) {
					return true;
				}
				return needFlush;
			}

			private boolean isRestrictChange(TAtomDsConfDO runConf,
					TAtomDsConfDO newConf) {
				if (runConf.getReadRestrictTimes() != newConf
						.getReadRestrictTimes()) {
					return true;
				}

				if (runConf.getWriteRestrictTimes() != newConf
						.getWriteRestrictTimes()) {
					return true;
				}

				if (runConf.getThreadCountRestrict() != newConf
						.getThreadCountRestrict()) {
					return true;
				}

				if (runConf.getTimeSliceInMillis() != newConf
						.getTimeSliceInMillis()) {
					return true;
				}

				return false;
			}
		});
	}

	/**
	 * ��TAtomDsConfDOת����LocalTxDataSourceDO
	 * 
	 * @param tAtomDsConfDO
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected static LocalTxDataSourceDO convertTAtomDsConf2JbossConf(TAtomDsConfDO tAtomDsConfDO, String dbName) {
		LocalTxDataSourceDO localTxDataSourceDO = new LocalTxDataSourceDO();
		if (TStringUtil.isNotBlank(dbName)) {
			localTxDataSourceDO.setJndiName(dbName);
		}
		localTxDataSourceDO.setUserName(tAtomDsConfDO.getUserName());
		localTxDataSourceDO.setPassword(tAtomDsConfDO.getPasswd());
		localTxDataSourceDO.setDriverClass(tAtomDsConfDO.getDriverClass());
		localTxDataSourceDO.setExceptionSorterClassName(tAtomDsConfDO.getSorterClass());
		//�������ݿ���������conURL��setConnectionProperties
		if (AtomDbTypeEnum.ORACLE == tAtomDsConfDO.getDbTypeEnum()) {
			String conUlr = TAtomConURLTools.getOracleConURL(tAtomDsConfDO.getIp(), tAtomDsConfDO.getPort(),
					tAtomDsConfDO.getDbName(), tAtomDsConfDO.getOracleConType());
			localTxDataSourceDO.setConnectionURL(conUlr);
			//�����oracleû������ConnectionProperties����Ը�Ĭ�ϵ�
			if (!tAtomDsConfDO.getConnectionProperties().isEmpty()) {
				localTxDataSourceDO.setConnectionProperties(tAtomDsConfDO.getConnectionProperties());
			} else {
				localTxDataSourceDO.setConnectionProperties(TAtomConstants.DEFAULT_ORACLE_CONNECTION_PROPERTIES);
			}
		} else if (AtomDbTypeEnum.MYSQL == tAtomDsConfDO.getDbTypeEnum()) {
			String conUlr = TAtomConURLTools.getMySqlConURL(tAtomDsConfDO.getIp(), tAtomDsConfDO.getPort(),
					tAtomDsConfDO.getDbName(), tAtomDsConfDO.getConnectionProperties());
			localTxDataSourceDO.setConnectionURL(conUlr);
			//��������ҵ�mysqlDriver�е�Valid��ʹ�ã���������valid
			try {
				Class validClass = Class.forName(TAtomConstants.DEFAULT_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				if (null != validClass) {
					localTxDataSourceDO
							.setValidConnectionCheckerClassName(TAtomConstants.DEFAULT_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				} else {
					logger.warn("MYSQL Driver is Not Suport "
							+ TAtomConstants.DEFAULT_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("MYSQL Driver is Not Suport " + TAtomConstants.DEFAULT_MYSQL_VALID_CONNECTION_CHECKERCLASS);
			} catch (NoClassDefFoundError e) {
				logger.warn("MYSQL Driver is Not Suport " + TAtomConstants.DEFAULT_MYSQL_VALID_CONNECTION_CHECKERCLASS);
			}
			
			//��������ҵ�mysqlDriver�е�integrationSorter��ʹ�÷���ʹ��Ĭ�ϵ�
			try {
				Class integrationSorterCalss = Class.forName(TAtomConstants.MYSQL_INTEGRATION_SORTER_CLASS);
				if (null != integrationSorterCalss) {
					localTxDataSourceDO.setExceptionSorterClassName(TAtomConstants.MYSQL_INTEGRATION_SORTER_CLASS);
				} else {
					localTxDataSourceDO.setExceptionSorterClassName(TAtomConstants.DEFAULT_MYSQL_SORTER_CLASS);
					logger.warn("MYSQL Driver is Not Suport " + TAtomConstants.MYSQL_INTEGRATION_SORTER_CLASS
							+ " use default sorter " + TAtomConstants.DEFAULT_MYSQL_SORTER_CLASS);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("MYSQL Driver is Not Suport " + TAtomConstants.MYSQL_INTEGRATION_SORTER_CLASS
						+ " use default sorter " + TAtomConstants.DEFAULT_MYSQL_SORTER_CLASS);
			} catch (NoClassDefFoundError e){
				logger.warn("MYSQL Driver is Not Suport " + TAtomConstants.MYSQL_INTEGRATION_SORTER_CLASS
						+ " use default sorter " + TAtomConstants.DEFAULT_MYSQL_SORTER_CLASS);
			}
		}
		localTxDataSourceDO.setMinPoolSize(tAtomDsConfDO.getMinPoolSize());
		localTxDataSourceDO.setMaxPoolSize(tAtomDsConfDO.getMaxPoolSize());
		localTxDataSourceDO.setPreparedStatementCacheSize(tAtomDsConfDO.getPreparedStatementCacheSize());
		if (tAtomDsConfDO.getIdleTimeout() > 0) {
			localTxDataSourceDO.setIdleTimeoutMinutes(tAtomDsConfDO.getIdleTimeout());
		}
		if (tAtomDsConfDO.getBlockingTimeout() > 0) {
			localTxDataSourceDO.setBlockingTimeoutMillis(tAtomDsConfDO.getBlockingTimeout());
		}
		return localTxDataSourceDO;
	}

	protected static boolean checkLocalTxDataSourceDO(
			LocalTxDataSourceDO localTxDataSourceDO) {
		if (null == localTxDataSourceDO) {
			return false;
		}

		if (TStringUtil.isBlank(localTxDataSourceDO.getConnectionURL())) {
			logger.error("[DsConfig Check] ConnectionURL is Empty !");
			return false;
		}

		if (TStringUtil.isBlank(localTxDataSourceDO.getUserName())) {
			logger.error("[DsConfig Check] UserName is Empty !");
			return false;
		}

		if (TStringUtil.isBlank(localTxDataSourceDO.getPassword())) {
			logger.error("[DsConfig Check] Password is Empty !");
			return false;
		}

		if (TStringUtil.isBlank(localTxDataSourceDO.getDriverClass())) {
			logger.error("[DsConfig Check] DriverClass is Empty !");
			return false;
		}

		if (localTxDataSourceDO.getMinPoolSize() < 1) {
			logger.error("[DsConfig Check] MinPoolSize Error size is:"
					+ localTxDataSourceDO.getMinPoolSize());
			return false;
		}

		if (localTxDataSourceDO.getMaxPoolSize() < 1) {
			logger.error("[DsConfig Check] MaxPoolSize Error size is:"
					+ localTxDataSourceDO.getMaxPoolSize());
			return false;
		}

		if (localTxDataSourceDO.getMinPoolSize() > localTxDataSourceDO
				.getMaxPoolSize()) {
			logger.error("[DsConfig Check] MinPoolSize Over MaxPoolSize Minsize is:"
					+ localTxDataSourceDO.getMinPoolSize()
					+ "MaxSize is :"
					+ localTxDataSourceDO.getMaxPoolSize());
			return false;
		}
		return true;
	}

	/**
	 * ���ñ������ø��Ǵ����TAtomDsConfDO������
	 * 
	 * @param tAtomDsConfDO
	 */
	private void overConfByLocal(TAtomDsConfDO localDsConfDO,
			TAtomDsConfDO newDsConfDO) {
		if (null == newDsConfDO || null == localDsConfDO) {
			return;
		}
		if (TStringUtil.isNotBlank(localDsConfDO.getDriverClass())) {
			newDsConfDO.setDriverClass(localDsConfDO.getDriverClass());
		}
		if (TStringUtil.isNotBlank(localDsConfDO.getSorterClass())) {
			newDsConfDO.setSorterClass(localDsConfDO.getSorterClass());
		}
		if (TStringUtil.isNotBlank(localDsConfDO.getPasswd())) {
			newDsConfDO.setPasswd(localDsConfDO.getPasswd());
		}
		if (null != localDsConfDO.getConnectionProperties()
				&& !localDsConfDO.getConnectionProperties().isEmpty()) {
			newDsConfDO.setConnectionProperties(localDsConfDO
					.getConnectionProperties());
		}
	}

	/**
	 * Datasource �İ�װ��
	 */
	private volatile TDataSourceWrapper wrapDataSource = null;

	public DataSource getDataSource() throws SQLException {
		if (wrapDataSource == null) {
			lock.lock();
			try {
				if (wrapDataSource != null) {
					// ˫�����
					return wrapDataSource;
				}
				String errorMsg = "";
				if (null == jbossDataSource) {
					errorMsg = "[InitError] TAtomDsConfHandle maybe forget init !";
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				DataSource dataSource = jbossDataSource.getDatasource();
				if (null == dataSource) {
					errorMsg = "[InitError] TAtomDsConfHandle maybe init fail !";
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				// ������ݿ�״̬������ֱ���׳��쳣
				if (null == this.getStatus()) {
					errorMsg = "[DB Stats Error] DbStatus is Null: "
							+ this.getDbKey();
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				TDataSourceWrapper tDataSourceWrapper = new TDataSourceWrapper(
						dataSource, runTimeConf);
				tDataSourceWrapper.setDatasourceName(dbKey);				tDataSourceWrapper.setDatasourceIp(runTimeConf.getIp());				tDataSourceWrapper.setDatasourcePort(runTimeConf.getPort());				tDataSourceWrapper.setDatasourceRealDbName(runTimeConf.getDbName());
				tDataSourceWrapper.setDbStatus(getStatus());
				logger.warn("set datasource key: " + dbKey);
				wrapDataSource = tDataSourceWrapper;

				return wrapDataSource;

			} finally {
				lock.unlock();
			}
		} else {
			return wrapDataSource;
		}
	}

	public void flushDataSource() {
		if (null != this.jbossDataSource) {
			logger.warn("[DataSource Flush] Start!");
			this.jbossDataSource.flush();
			logger.warn("[DataSource Flush] End!");
		}
	}

	protected void destroyDataSource() throws Exception {
		if (null != this.jbossDataSource) {
			logger.warn("[DataSource Stop] Start!");
			this.jbossDataSource.destroy();
			if (null != this.dbConfManager) {
				this.dbConfManager.stopDbConfManager();
			}
			if (null != this.dbPasswdManager) {
				this.dbPasswdManager.stopDbPasswdManager();
			}
			logger.warn("[DataSource Stop] End!");
		}

	}

	void setSingleInGroup(boolean isSingleInGroup) {
		this.runTimeConf.setSingleInGroup(isSingleInGroup);
	}

	public void setAppName(String appName) throws AtomAlreadyInitException {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] couldn't Reset appName !");
		}
		this.appName = appName;
	}

	public void setDbKey(String dbKey) throws AtomAlreadyInitException {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] couldn't Reset dbKey !");
		}
		this.dbKey = dbKey;
	}

	public void setLocalPasswd(String passwd) throws AtomAlreadyInitException {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] couldn't Reset passwd !");
		}
		this.localConf.setPasswd(passwd);
	}

	public void setLocalConnectionProperties(Map<String, String> map)
			throws AtomAlreadyInitException {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] couldn't Reset connectionProperties !");
		}
		this.localConf.setConnectionProperties(map);
	}

	public void setLocalDriverClass(String driverClass)
			throws AtomAlreadyInitException {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] couldn't Reset driverClass !");
		}
		this.localConf.setDriverClass(driverClass);
	}

	public void setLocalSorterClass(String sorterClass)
			throws AtomAlreadyInitException {
		if (initFalg) {
			throw new AtomAlreadyInitException(
					"[AlreadyInit] couldn't Reset sorterClass !");
		}
		this.localConf.setSorterClass(sorterClass);
	}

	public String getAppName() {
		return appName;
	}

	public String getDbKey() {
		return dbKey;
	}

	public AtomDbStatusEnum getStatus() {
		return this.runTimeConf.getDbStautsEnum();
	}

	public AtomDbTypeEnum getDbType() {
		return this.runTimeConf.getDbTypeEnum();
	}

	public void setDbStatusListeners(
			List<TAtomDbStatusListener> dbStatusListeners) {
		this.dbStatusListeners = dbStatusListeners;
	}

	private void processDbStatusListener(AtomDbStatusEnum oldStatus,
			AtomDbStatusEnum newStatus) {
		if (null != oldStatus && oldStatus != newStatus) {
			if (null != dbStatusListeners) {
				for (TAtomDbStatusListener statusListener : dbStatusListeners) {
					try {
						statusListener.handleData(oldStatus, newStatus);
					} catch (Exception e) {
						logger.error("[call StatusListenner Error] !", e);
						continue;
					}
				}
			}
		}
	}
}

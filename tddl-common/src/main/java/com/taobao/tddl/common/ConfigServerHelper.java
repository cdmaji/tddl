/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common;

import java.io.ByteArrayInputStream;import java.io.IOException;import java.io.Serializable;import java.text.MessageFormat;import java.util.Properties;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import com.taobao.tddl.common.config.ConfigDataHandler;import com.taobao.tddl.common.config.ConfigDataHandlerFactory;import com.taobao.tddl.common.config.ConfigDataListener;import com.taobao.tddl.common.config.impl.DefaultConfigDataHandlerFactory;
/**
 * ���ĳ־û����ݵĸ�����
 * 
 * @author linxuan
 * 
 */
public final class ConfigServerHelper {
	private static final Log log = LogFactory.getLog(ConfigServerHelper.class);

	public static final int SUBSCRIBE_REGISTER_WAIT_TIME = 30000;

	public static final String DATA_ID_PREFIX = "com.taobao.tddl.v1_";

	public static final String DATA_ID_TDDL_SHARD_RULE = DATA_ID_PREFIX + "{0}_shardrule";

	public static final String DATA_ID_REPLICATION_SWITCH = DATA_ID_PREFIX + "{0}_replication.switch";

	public static final String DATA_ID_DBINFO_DBROLE = "com.taobao.dbinfo.dbrole.v1";

	public static final String DATA_ID_DB_GROUP_KEYS = DATA_ID_PREFIX + "{0}_dbgroups";

	/**
	 * syncServer����ҵ���б����ĸ����ã��������ÿ��ҵ��ID����������һ��TDatasource�������ʼ��replication
	 */
	public static final String DATA_ID_REPLICATION = DATA_ID_PREFIX + "{0}_replication";

	/**
	 * ��־���ռλ������ҵ��ID������־���syncServer��ID����Ϊ���ҵ����ܹ���һ����־��
	 */
	public static final String DATA_ID_SYNCLOG_DBSET = DATA_ID_PREFIX + "{0}_synclog.dbset";

	public static final String DATA_ID_SYNCLOG_DBWEIGHT = DATA_ID_PREFIX + "{0}_synclog.dbweight";

	/**
	 * ����;ֲ��Ĺ�ϵ��Ȩ�������ֲ��ԣ�
	 * 1. ÿ��dbIndexһ��Ȩ��dataId������Ϊ��dbIndex��Ͻ���Ȩ������
	 * 2. һ��TDataSource(һ��Ӧ��)һ��Ȩ��dataId����������dbIndex��Ȩ������
	 * 3. ���߶��У�ͬʱʹ�ã�dbIndex��Ȩ�����ã�����Ӧ�õĴ��ȫ��Ȩ������
	 * ���ò���2
	 * 
	 * һ��TDataSource(һ��Ӧ��)һ��Ȩ��dataId�� {0}:Ӧ������appName,��IC
	 * ���ݸ�ʽ��
	 *   slave_1=R10W10,R20W0
	 *   slave_3=R10W20,R20W10
	 *   slave_5=RW
	 *   master_0=RW
	 */
	public static final String DATA_ID_APP_DBWEIGHT = DATA_ID_PREFIX + "{0}.dbweight";

	/**
	 * ÿ��dbIndexһ��Ȩ��dataId
	 * {0} Ӧ������appName,��IC
	 * {1} dbIndex��TDataSource��dataSourcePool��key
	 * 
	 * ���ݸ�ʽ��
	 *   dbindex_0=R10W10,R20W0,R10W0 
	 */
	public static final String DATA_ID_GLOBAL_DBINFO = DATA_ID_PREFIX + "{0}.global.dbinfo";

	public static final String DATA_ID_TDDL_CLIENT_CONFIG = DATA_ID_PREFIX + "{0}_tddlconfig";

	public enum TDDLConfigKey {		statKeyRecordType, statKeyLeftCutLen, statKeyRightCutLen, statKeyExcludes, StatRealDbInWrapperDs, //		StatChannelMask, statDumpInterval/*��*/, statCacheSize, statAtomSql, statKeyIncludes, //		SmoothValveProperties,CountPunisherProperties,//		//add by junyu		sqlExecTimeOutMilli/*sql��ʱʱ��*/,atomSqlSamplingRate/*atom��sqlͳ�ƵĲ�����*/;	}

	/**
	 * ����Ӧ�õķֿ�ֱ�����
	 */
	public static Object subscribeShardRuleConfig(String appName, DataListener listener) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_TDDL_SHARD_RULE).format(new Object[] { appName });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	/**
	 * ����Ӧ�õ����ݿ�Ȩ������
	 */
	public static Object subscribeAppDbWeight(String appName, DataListener listener) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_APP_DBWEIGHT).format(new Object[] { appName });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	/**
	 * ����Ӧ�õ��и�������
	 */
	public static Object subscribeReplicationConfig(String appName, DataListener listener) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_REPLICATION).format(new Object[] { appName });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	/**
	 * ����Ӧ�õ��и��ƿ���
	 */
	public static Object subscribeReplicationSwitch(String appName, DataListener listener) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_REPLICATION_SWITCH).format(new Object[] { appName });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	/**
	 * ������־������
	 */
	public static Object subscribeSyncLogDbConfig(String syncServerID, DataListener listener) {
		if (syncServerID == null || syncServerID.length() == 0) {
			throw new IllegalStateException("û��ָ������������ID��syncServerID");
		}
		String dataId = new MessageFormat(DATA_ID_SYNCLOG_DBSET).format(new Object[] { syncServerID });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	/**
	 * ������־��Ȩ������
	 */
	public static Object subscribeSyncLogDbWeight(String syncServerID, DataListener listener) {
		if (syncServerID == null || syncServerID.length() == 0) {
			throw new IllegalStateException("û��ָ������������ID��syncServerID");
		}
		String dataId = new MessageFormat(DATA_ID_SYNCLOG_DBWEIGHT).format(new Object[] { syncServerID });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	/**
	 * ��������TDDL�ͻ�������
	 */
	public static Object subscribeTDDLConfig(String appName, DataListener listener) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_TDDL_CLIENT_CONFIG).format(new Object[] { appName });
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	private static String getCallerClassName() {
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		return stes[stes.length - 1].getClassName();
	}

	public static Object subscribeDbInofDbRoleDb(DataListener listener) {
		return ConfigServerHelper.subscribePersistentData(getCallerClassName(), DATA_ID_DBINFO_DBROLE, listener);
	}

	/**
	 * @return ��һ�λ�ȡ��data������ʱonDataReceiveAtRegister�Ѿ����ù�һ��
	 */
	public static Object subscribePersistentData(String dataId, final DataListener listener) {
		return subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	public static Object subscribeData(String dataId, final DataListener listener) {
		//���ķǳ־����ݣ��Ͷ��ĳ־����ݵĽӿ�����ȫ��ͬ��
		return subscribePersistentData(getCallerClassName(), dataId, listener);
	}

	private volatile static ConfigDataHandlerFactory cdhf;
	private static final long DIAMOND_FIRST_DATA_TIMEOUT=15*1000;
	
	public static Object subscribePersistentData(String subscriberName, String dataId, final DataListener listener) {
		cdhf = new DefaultConfigDataHandlerFactory();
		ConfigDataHandler matrixHandler = cdhf.getConfigDataHandler(dataId, null);
		String datas = matrixHandler.getData(DIAMOND_FIRST_DATA_TIMEOUT, ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY); //ȡ������Ϣ��Ĭ�ϳ�ʱʱ��Ϊ30��

		//����ȥ�����µı�������
		log.warn(dataId + "'s firstData=" + datas);
		if (datas != null) {
			try {
				listener.onDataReceiveAtRegister(datas);
			} catch (Throwable t) {
				//��֤��ʹ�״δ���dataId�����쳣��listenerҲһ���ᱻע�ᣬҵ����Ȼ���յ���������
				log.error("onDataReceiveAtRegister�׳��쳣��dataId:" + dataId, t);
			}
		}
		matrixHandler.addListener(new ConfigDataListener() {
			@Override
			public void onDataRecieved(String dataId, String data) {
				log.info("recieve data,data id:"+dataId+" data:"+data);
				listener.onDataReceive(data);
			}
		},null);

		return datas;
	}

	/**
	 * һ��Util�����������ٸ��Util�࣬�ʷŵ����
	 * ��Properties�����Properties�ַ�������ΪProperties���� 
	 */
	public static Properties parseProperties(Object data, String msg) {
		Properties p;
		if (data == null) {
			log.warn(msg + "data == null");
			return null;
		} else if (data instanceof Properties) {
			p = (Properties) data;
		} else if (data instanceof String) {
			p = new Properties();
			try {
				p.load(new ByteArrayInputStream(((String) data).getBytes()));
			} catch (IOException e) {
				log.error(msg + "�޷��������͵����ã�" + data, e);
				return null;
			}
		} else {
			log.warn(msg + "�����޷�ʶ��" + data);
			return null;
		}
		return p;
	}

	//���Ը�������������һ���־�����
	public static void publish(String dataId, Serializable data) {
		publish(dataId, data, null);
	}

	public static void publish(String dataId, Serializable data, String group) {
		return;
		//DiamondҪ�ö����sdk�������ݡ�������Ի�����mock
		//TODO �ĳ�sdk��ʽ��֧�ֲ���
	}

	/*private static Object fetchConfig(Subscriber subscriber) {
		try {
			Subscription subscription = subscriber.getSubscription();
			List<Object> data = subscription.waitNext(10);
			if (data == null || data.size() == 0) {
				data = subscription.waitNext(SUBSCRIBE_REGISTER_WAIT_TIME);
			} else {
				List<Object> data2 = subscription.waitNext(SUBSCRIBE_REGISTER_WAIT_TIME);
				if (data2 != null && data2.size() != 0) {
					data = data2;
				}
			}
			return data == null || data.size() == 0 ? null : data.get(0);
		} catch (CancellationException e) {
			log.error("", e);
			return null;
		} catch (InterruptedException e) {
			log.error("", e);
			return null;
		}
	}*/

	public static interface DataListener {
		/**
		 * ע��֮������DataObserver֮ǰ��fetchConfig���ܵ�����ʱ�����ø÷�����һ������ҵ���ʼ��ʱ
		 */
		void onDataReceiveAtRegister(Object data);

		/**
		 * ע��֮�󣬵�һ�ν��յ����͵���onDataReceiveAtRegister������ϣ�
		 * ����DataObserver֮���ٽӵ���̬���ͣ����ø÷�����һ������ҵ������ʱ
		 */
		void onDataReceive(Object data);
	}

	public static abstract class AbstractDataListener implements DataListener {
		public void onDataReceiveAtRegister(Object data) {
			this.onDataReceive(data);
		}
	}

	public static String getDBGroupsConfig(String appName) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_DB_GROUP_KEYS).format(new Object[] { appName });
		return dataId;
	}

	public static String getShardRuleConfig(String appName) {
		if (appName == null || appName.length() == 0) {
			throw new IllegalStateException("û��ָ��Ӧ������appName");
		}
		String dataId = new MessageFormat(DATA_ID_TDDL_SHARD_RULE).format(new Object[] { appName });
		return dataId;
	}
}

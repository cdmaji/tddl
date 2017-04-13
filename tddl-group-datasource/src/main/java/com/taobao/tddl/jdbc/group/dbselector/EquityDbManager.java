/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.dbselector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.common.WeightRandom;
import com.taobao.tddl.common.util.NagiosUtils;
import com.taobao.tddl.jdbc.group.DataSourceWrapper;
import com.taobao.tddl.jdbc.group.config.ConfigManager;
import com.taobao.tddl.jdbc.group.config.GroupExtraConfig;

/**
 * �Ե����ݿ������
 * �����Ƕ��Եȣ��������⣬ÿ�����������ȫ��ͬ���Եȶ�ȡ
 * ������д�Եȣ�����־�⣬ÿ�������ݲ�ͬ��һ������д���ĸ��ⶼ���ԡ��Ե�д��
 *
 * ֧�ֶ�̬����Ȩ�أ���̬�Ӽ���
 *
 * @author linxuan
 * @author yangzhu
 *
 */

//��Ϊ��������Ϣ�䶯ʱÿ�ζ�����������һ���µ�EquityDbManagerʵ����
//����ԭ�е���"��̬�ı�"��صĴ������µ�EquityDbManagerʵ������ȫ��ɾ��
public class EquityDbManager extends AbstractDBSelector {
	private static final Log logger = LogFactory.getLog(EquityDbManager.class);

	private Map<String /* dsKey */, DataSourceHolder> dataSourceMap;
	private WeightRandom weightRandom;

	public EquityDbManager(Map<String, DataSourceWrapper> dataSourceWrapperMap, Map<String, Integer> weightMap) {
		this.dataSourceMap = new HashMap<String, DataSourceHolder>(dataSourceWrapperMap.size());
		for (Map.Entry<String, DataSourceWrapper> e : dataSourceWrapperMap.entrySet()) {
			this.dataSourceMap.put(e.getKey(), new DataSourceHolder(e.getValue()));
		}
		this.weightRandom = new WeightRandom(weightMap);
	}

	public EquityDbManager(Map<String, DataSourceWrapper> dataSourceWrapperMap,
			Map<String, Integer> weightMap,
			GroupExtraConfig groupExtraConfig) {
		super.groupExtraConfig = groupExtraConfig;
		this.dataSourceMap = new HashMap<String, DataSourceHolder>(dataSourceWrapperMap.size());
		for (Map.Entry<String, DataSourceWrapper> e : dataSourceWrapperMap.entrySet()) {
			this.dataSourceMap.put(e.getKey(), new DataSourceHolder(e.getValue()));
		}
		this.weightRandom = new WeightRandom(weightMap);
	}

	private static String selectAliveKey(WeightRandom weightRandom, List<String> excludeKeys) {
		if (null == excludeKeys) {
			excludeKeys = new ArrayList<String>();
		}
		return weightRandom.select(excludeKeys);
	}

	/**
	 * @return ����Ȩ�أ��������һ��DataSource
	 */
	public DataSource select() {
		String key = selectAliveKey(weightRandom, null);
		if (null != key) {
			return this.get(key);
		} else {
			return null;
		}
	}

	/**
	 * ����ָ��dsKey��Ӧ������Դ������Ӧ����Դ�ĵ�ǰȨ��Ϊ0���򷵻�null
	 * @param dsKey �ڲ���ÿһ������DataSource��Ӧ��key, �ڳ�ʼ��dbSelectorʱָ��
	 * @return ����dsKey��Ӧ������Դ
	 */
	public DataSourceWrapper get(String dsKey) {
		DataSourceHolder holder = dataSourceMap.get(dsKey);
		Integer weigthValue = this.weightRandom.getWeightConfig().get(dsKey);
		if (weigthValue == null || weigthValue.equals(0))
			return null;
		return holder == null ? null : holder.dsw;
	}

	//TODO ���ǽӿ��Ƿ���СΪֻ����DataSource[]
	public Map<String, DataSource> getDataSources() {
		Map<String, DataSource> dsMap = new HashMap<String, DataSource>(dataSourceMap.size());
		for (Map.Entry<String, DataSourceHolder> e : dataSourceMap.entrySet()) {
			dsMap.put(e.getKey(), e.getValue().dsw);
		}
		return dsMap;
	}

	public Map<String, Integer> getWeights() {
		return weightRandom.getWeightConfig();
	}

	/**
	 * ������������ݿ�������ִ��һ���ص�������ʧ���˸���Ȩ��ѡ��һ��������
	 * �Ը���Ȩ��ѡ�񵽵�DataSource�����û���������ò���args�����Ե���DataSourceTryer��tryOnDataSource����
	 * @param failedDataSources ��֪��ʧ��DS�����쳣
	 * @param args ͸����DataSourceTryer��tryOnDataSource������
	 * @return null��ʾִ�гɹ��������ʾ���Դ���ִ��ʧ�ܣ�����SQLException�б�
	 */
	protected <T> T tryExecuteInternal(Map<DataSource, SQLException> failedDataSources, DataSourceTryer<T> tryer,
			int times, Object... args) throws SQLException {
		//�����֧�����ԣ���times��Ϊ1�Ϳ�����
		if (!this.isSupportRetry) {
			times = 1;
		}
		WeightRandom wr = this.weightRandom;
		List<SQLException> exceptions = new ArrayList<SQLException>(0);
		List<String> excludeKeys = new ArrayList<String>(0);
		if (failedDataSources != null) {
			exceptions.addAll(failedDataSources.values());
			times = times - failedDataSources.size(); //�۳��Ѿ�ʧ�ܵ������Դ���
			for (SQLException e : failedDataSources.values()) {
				if (!exceptionSorter.isExceptionFatal(e)) {
					//��һ���쳣����ʵ����������쳣����map�޷�֪��˳��ֻ�ܱ������������ݿⲻ�����쳣�����׳�
					//�ǲ���Ӧ���ڷ��ַ����ݿ�fatal֮��������׳��������Ƿŵ�failedDataSources���map��?(guangxia)
					return tryer.onSQLException(exceptions, exceptionSorter, args);
				}
			}
		}
		for (int i = 0; i < times; i++) {
			String name = selectAliveKey(wr, excludeKeys);
			if (name == null) {
				// Ϊ����չ
				exceptions.add(new NoMoreDataSourceException("tryTime:" + i + ", excludeKeys:" + excludeKeys
						+ ", weightConfig:" + wr.getWeightConfig()));
				break;
			}

			DataSourceHolder dsHolder = dataSourceMap.get(name);
			if (dsHolder == null) {
				//��Ӧ�ó��ֵġ���ʼ���߼�Ӧ�ñ�֤�յ�����Դ(null)���ᱻ����dataSourceMap
				throw new IllegalStateException("Can't find DataSource for name:" + name);
			}
			if (failedDataSources != null && failedDataSources.containsKey(dsHolder.dsw)) {
				excludeKeys.add(name);
				i--; //��β������Դ���
				continue;
			}
			//TODO �б�Ҫÿ�ζ����DataSource��״̬�� ���һ������Դ�������NA����һ��ֻ���Ŀ���д��¼��Ҫ������һ������Դ
			if (!ConfigManager.isDataSourceAvailable(dsHolder.dsw, this.readable)) {
				excludeKeys.add(name);
				i--; //��β������Դ���
				continue;
			}

			try {
				if (dsHolder.isNotAvailable) {
					boolean toTry = System.currentTimeMillis() - dsHolder.lastRetryTime > retryBadDbInterval;
					if (toTry && dsHolder.lock.tryLock()) {
						try {
							T t = tryer.tryOnDataSource(dsHolder.dsw, args); //ͬһ��ʱ��ֻ����һ���̼߳���ʹ���������Դ��
							dsHolder.isNotAvailable = false; //��һ���߳����ԣ�ִ�гɹ�����Ϊ���ã��Զ��ָ�
							return t;
						} finally {
							dsHolder.lastRetryTime = System.currentTimeMillis();
							dsHolder.lock.unlock();
						}
					} else {
						excludeKeys.add(name); //�����߳������Ѿ����ΪnotAvailable������Դ
						i--; //��β������Դ���
						continue;
					}
				} else {
					return tryer.tryOnDataSource(dsHolder.dsw, args); //��һ�γɹ�ֱ�ӷ���
				}
			} catch (SQLException e) {
				exceptions.add(e);
				boolean isFatal = exceptionSorter.isExceptionFatal(e);
				if (isFatal) {
					NagiosUtils.addNagiosLog(NagiosUtils.KEY_DB_NOT_AVAILABLE + "|" + name, e.getMessage());
					dsHolder.isNotAvailable = true;
				}
				if (!isFatal || failedDataSources == null) {
					//throw e; //����������ݿⲻ�����쳣�����߲�Ҫ�����ԣ�ֱ���׳�
					break;
				}
				logger.warn(new StringBuilder().append(i + 1).append("th try locate on [").append(name).append(
						"] failed:").append(e.getMessage()).toString()); //���ﲻ���쳣ջ��,ȫ������ʧ�ܲ��ɵ����ߴ�
				excludeKeys.add(name);
			}
		}
		return tryer.onSQLException(exceptions, exceptionSorter, args);
	}

	private final Random random = new Random();

	/**
	 * �������������Ȩ�ش������ֵΪdataSourceIndex��i������Դ
	 * ���Ȩ�ش�û�ж���i/I����dataSourceIndex���ڼ�����·�ɵ�group�еĵڼ���������Դ
	 *
     * һ��db����ͬʱ���ö��i����ͬ��db����������ͬ��i������Ȩ�ش�= db0:rwi0i2, db1:ri1, db2:ri1, db3:ri2 ��
     *     �û�ָ��dataSourceIndex=0��·�ɵ�db0����ֻ��db0��i0��
     *     �û�ָ��dataSourceIndex=1�����·�ɵ�db1��db2����db1��db2����i1��
     *     �û�ָ��dataSourceIndex=2�����·�ɵ�db0��db3����db0��db3����i2��
     * ���û������i������db0:rw, db1:r; ָ��dataSourceIndex=1��·�ɵ�db1
	 */
	protected DataSourceHolder findDataSourceWrapperByIndex(int dataSourceIndex) {
		List<DataSourceHolder> holders = new ArrayList<DataSourceHolder>();
		for (DataSourceHolder dsh : dataSourceMap.values()) {
			if (dsh.dsw.isMatchDataSourceIndex(dataSourceIndex))
				holders.add(dsh);
		}

		if (!holders.isEmpty()) {
			return holders.get(random.nextInt(holders.size()));
		} else {
			return null;
		}
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.dbselector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.jdbc.sorter.ExceptionSorter;
import com.taobao.tddl.common.exception.runtime.NotSupportException;
import com.taobao.tddl.jdbc.group.DataSourceWrapper;

/**
 * �����ȼ�ѡ���selector
 * 
 * ÿ��ѡ��ֻ�����ȼ���ߵ�һ��DB��ѡ�����������ã��ż�������һ�����ȼ���DB����ѡ��
 * 
 * ���ȼ���ͬ��DB�������ѡ��
 * 
 * ԭʼ����TCҪ����ÿ��dbgroup�����ȶ����⣬�����ⲻ����ʱ���Զ������� 
 * ��չ����һ���౸��������������⡣�����ⶼ������ʱ���Ŷ�����
 * 
 * Ϊ�˷��㴦��ͽӿ�һ�£�������Ҫ�� 
 * 1. Ŀǰֻ֧�ֶ������ȼ��� 
 * 2. һ��Ȩ�����͵���Ϣ�У������� 
 * 3. һ������Դֻ����һ�����ȼ����У�
 * 
 * @author linxuan
 * 
 */
public class PriorityDbGroupSelector extends AbstractDBSelector {
	private static final Log logger = LogFactory.getLog(PriorityDbGroupSelector.class);

	/**
	 * �����ȼ�˳�������ݿ��顣Ԫ��0���ȼ���ߡ�ÿ��EquityDbManagerԪ�ش��������ͬ���ȼ���һ�����ݿ�
	 */
	private EquityDbManager[] priorityGroups;

	public PriorityDbGroupSelector(EquityDbManager[] priorityGroups) {
		this.priorityGroups = priorityGroups;
		if (priorityGroups == null || priorityGroups.length == 0) {
			throw new IllegalArgumentException("EquityDbManager[] priorityGroups is null or empty");
		}
	}

	public DataSource select() {
		for (int i = 0; i < priorityGroups.length; i++) {
			DataSource ds = priorityGroups[i].select();
			if (ds != null) {
				return ds;
			}
		}
		return null;
	}

	public DataSourceWrapper get(String dsKey) {
		for (int i = 0; i < priorityGroups.length; i++) {
			DataSourceWrapper ds = priorityGroups[i].get(dsKey);
			if (ds != null) {
				return ds;
			}
		}
		return null;
	}

	/**
	 * ȡÿ�������weightKey���ܵ�weightKey�Ľ�������������
	 */
	public void setWeight(Map<String, Integer> weightMap) {
		/*
		for (int i = 0; i < priorityGroups.length; i++) {
			Map<String, Integer> oldWeights = priorityGroups[i].getWeights();
			Map<String, Integer> newWeights = new HashMap<String, Integer>(oldWeights.size());
			for (Map.Entry<String, Integer> e : weightMap.entrySet()) {
				if (oldWeights.containsKey(e.getKey())) {
					newWeights.put(e.getKey(), e.getValue());
				}
			}
			priorityGroups[i].setWeightRandom(new WeightRandom(newWeights));
		}
		*/
	}

	private static class DataSourceTryerWrapper<T> implements DataSourceTryer<T> {
		private final List<SQLException> historyExceptions;
		private final DataSourceTryer<T> tryer;

		public DataSourceTryerWrapper(DataSourceTryer<T> tryer, List<SQLException> historyExceptions) {
			this.tryer = tryer;
			this.historyExceptions = historyExceptions;
		}

		public T onSQLException(List<SQLException> exceptions, ExceptionSorter exceptionSorter, Object... args)
				throws SQLException {
			Exception last = exceptions.get(exceptions.size() - 1);
			if (last instanceof NoMoreDataSourceException) {
				if (exceptions.size() > 1) {
					exceptions.remove(exceptions.size() - 1);
				}
				historyExceptions.addAll(exceptions);
				throw (NoMoreDataSourceException) last;
			} else {
				return tryer.onSQLException(exceptions, exceptionSorter, args);
			}
		}

		public T tryOnDataSource(DataSourceWrapper dsw, Object... args) throws SQLException {
			return tryer.tryOnDataSource(dsw, args);
		}
	};

	/**
	 * ����EquityDbManager��tryExecuteʵ�֣����û���tryer��һ����װ����wrapperTryer.onSQLException��
	 * ��⵽���һ��e��NoMoreDataSourceExceptionʱ������ԭtryer��onSQLException, ת�������������ȼ���
	 */
	protected <T> T tryExecuteInternal(Map<DataSource, SQLException> failedDataSources, DataSourceTryer<T> tryer,
			int times, Object... args) throws SQLException {
		final List<SQLException> historyExceptions = new ArrayList<SQLException>(0);
		DataSourceTryer<T> wrapperTryer = new DataSourceTryerWrapper<T>(tryer, historyExceptions); //�ƻ���ľ

		for (int i = 0; i < priorityGroups.length; i++) {
			try {
				return priorityGroups[i].tryExecute(failedDataSources, wrapperTryer, times, args);
			} catch (NoMoreDataSourceException e) {
				logger.warn("NoMoreDataSource for retry for priority group " + i);
			}
		}
		//���е����ȼ��鶼�����ã����׳��쳣
		return tryer.onSQLException(historyExceptions, exceptionSorter, args);
	}

	@Override
	public void setSupportRetry(boolean isSupportRetry) {
		for (int i = 0; i < priorityGroups.length; i++) {
			priorityGroups[i].setSupportRetry(isSupportRetry);
		}
		this.isSupportRetry = isSupportRetry;
	}

	public void setReadable(boolean readable) {
		for (int i = 0; i < priorityGroups.length; i++) {
			priorityGroups[i].setReadable(readable);
		}

		this.readable = readable;
	}

	/*
	public DataSource[] getDataSourceArray() {
		List<DataSource> dataSources = new ArrayList<DataSource>();
		for (EquityDbManager e : priorityGroups) {
			for (DataSource ds : e.getDataSourceArray())
				dataSources.add(ds);
		}
		return dataSources.toArray(new DataSource[0]);
	}
	*/

	public Map<String, DataSource> getDataSources() {
		throw new NotSupportException("getDataSources()");
	}

	protected DataSourceHolder findDataSourceWrapperByIndex(int dataSourceIndex) {
		for (int i = 0; i < priorityGroups.length; i++) {
			DataSourceHolder holder = priorityGroups[i].findDataSourceWrapperByIndex(dataSourceIndex);
			if (holder != null)
				return holder;

		}
		return null;
	}
}

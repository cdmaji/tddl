/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.dbselector;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.taobao.tddl.client.jdbc.sorter.ExceptionSorter;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.jdbc.group.DataSourceWrapper;
import com.taobao.tddl.jdbc.group.util.ExceptionUtils;

/**
 * �Ե����ݿ�ѡ������
 * ��������ȫ��ͬ��һ�����ѡ��һ����
 * ���ڶ�HA/RAC���,���������ȡһ�����Ĳ���
 * 
 * @author linxuan
 */
public interface DBSelector {
	public static final int NOT_EXIST_USER_SPECIFIED_INDEX=-1;
	
	/**
	 * @return ���ظ�Selector�ı�ʶ
	 */
	String getId();

	/**
	 * �Ե����ݿ�ѡ������
	 * ��������ȫ��ͬ��һ�����ѡ��һ����
	 * ���ڶ�HA/RAC���,���������ȡһ�����Ĳ���
	 */
	DataSource select();

	/**
	 * ����ָ��dsKey��Ӧ������Դ������Ӧ����Դ�ĵ�ǰȨ��Ϊ0���򷵻�null
	 * �������ͬʱ���������ж�һ��dsKey��Ӧ�Ŀ��Ƿ�ɶ����д��
	 *   rselector.get(wBaseDsKey) != null ��ɶ�
	 *   wselector.get(rBaseDsKey) != null ���д
	 * TGroupConnection��д���Ӹ��õľ�ʵ�ֻ��õ��������
	 * 
	 * @param dsKey �ڲ���ÿһ������DataSource��Ӧ��key, �ڳ�ʼ��dbSelectorʱָ��
	 * @return ����dsKey��Ӧ������Դ
	 */
	DataSource get(String dsKey);

	/**
	 * �������ݿ����ͣ�Ŀǰֻ����ѡ��exceptionSorter 
	 */
	void setDbType(DBType dbType);

	/**
	 * ��ѡ�񵽵�DataSource�ʹ����args������ִ��
	 *    tryer.tryOnDataSource(String dsKey, DataSource ds, Object... args)
	 * ÿ��ѡ��DataSource���ų��ϴ�����ʧ�ܵ�, ֱ���ﵽָ�������Դ��������ڼ��׳������ݿⲻ�����쳣
	 * 
	 * �׳��쳣�������������쳣�б��������args������
	 *    tryer.onSQLException(List<SQLException> exceptions, Object... args)
	 * 
	 * @param tryer
	 * @param times
	 * @param args
	 * @throws SQLException
	 */
	<T> T tryExecute(DataSourceTryer<T> tryer, int times, Object... args) throws SQLException;

	/**
	 * @param failedDataSources: �ڵ��ø÷���ǰ���Ѿ���֪�Թ�ʧ�ܵ�DataSource�Ͷ�Ӧ��SQLException
	 * �������������ԭ������Ϊ���ݿ��������ΪgetConnection/createStatement/execute����������������һ�����try catch��
	 * failedDataSources == null ��ʾ����Ҫ���ԣ������κ��쳣ֱ���׳�������д���ϵĲ���
	 */
	<T> T tryExecute(Map<DataSource, SQLException> failedDataSources, DataSourceTryer<T> tryer, int times,
			Object... args) throws SQLException;

	/**
	 * �Ƿ�֧�����ԡ�
	 * ����ӿ�������ӿڡ�������Թ����㹻�ȶ�������ȥ������������Ҫ���Եĳ����ṩ˫�ر�֤
	 * @return �Ƿ�֧������
	 */
	boolean isSupportRetry();

	void setReadable(boolean readable);

	Map<String, DataSource> getDataSources(); //ֱ�ӻ�ȡ��Ӧ������Դ

	/**
	 * ��DBSelector���������Դ������ִ�в����Ļص��ӿ�
	 */
	public static interface DataSourceTryer<T> {
		/**
		 * @param dsKey �ڲ���ÿһ������DataSource��Ӧ��key, �ڳ�ʼ��dbSelectorʱָ��
		 * @param ds
		 * @param args �û�����tryExecuteʱ����Ĳ����б�
		 * @return
		 * @throws SQLException
		 */
		//T tryOnDataSource(String dsKey, DataSource ds, Object... args) throws SQLException;
		/**
		 * tryExecute�����Ե���tryOnDataSource���������ݿⲻ�����쳣�����������Դ���ʱ������ø÷���
		 * @param exceptions ��������ʧ���׳����쳣��
		 *    ���һ���쳣���������ݿⲻ���õ��쳣��Ҳ��������ͨ��SQL�쳣
		 *    ���һ��֮ǰ���쳣�����ݿⲻ���õ��쳣
		 * @param exceptionSorter ��ǰ�õ����ж�Exception���͵ķ�����
		 * @param args ��tryOnDataSourceʱ��args��ͬ�������û�����tryExecuteʱ�����arg
		 * @return �û���ʵ���ߣ������Ƿ񷵻�ʲôֵ
		 * @throws SQLException
		 */
		T onSQLException(List<SQLException> exceptions, ExceptionSorter exceptionSorter, Object... args)
				throws SQLException;

		T tryOnDataSource(DataSourceWrapper dsw, Object... args) throws SQLException;
	}

	/**
	 * DataSourceTryer.onSQLException ֱ���׳��쳣
	 */
	public static abstract class AbstractDataSourceTryer<T> implements DataSourceTryer<T> {
		@SuppressWarnings("unchecked")
		public T onSQLException(List<SQLException> exceptions, ExceptionSorter exceptionSorter, Object... args)
				throws SQLException {
			ExceptionUtils.throwSQLException(exceptions, null, Collections.EMPTY_LIST);
			return null;
		}
	}

}

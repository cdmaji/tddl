/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import net.sf.json.JSONArray;import net.sf.json.JSONException;import net.sf.json.JSONObject;import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.common.DataSourceChangeListener;
import com.taobao.tddl.common.config.ConfigDataHandler;
import com.taobao.tddl.common.config.ConfigDataHandlerFactory;
import com.taobao.tddl.common.config.ConfigDataListener;
import com.taobao.tddl.common.config.impl.DefaultConfigDataHandlerFactory;
import com.taobao.tddl.common.util.DataSourceFetcher;
import com.taobao.tddl.common.util.TStringUtil;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.jdbc.atom.TAtomDataSource;
import com.taobao.tddl.jdbc.atom.config.object.AtomDbStatusEnum;
import com.taobao.tddl.jdbc.group.DataSourceWrapper;
import com.taobao.tddl.jdbc.group.TGroupDataSource;
import com.taobao.tddl.jdbc.group.dbselector.AbstractDBSelector;
import com.taobao.tddl.jdbc.group.dbselector.DBSelector;
import com.taobao.tddl.jdbc.group.dbselector.EquityDbManager;
import com.taobao.tddl.jdbc.group.dbselector.OneDBSelector;
import com.taobao.tddl.jdbc.group.dbselector.PriorityDbGroupSelector;
import com.taobao.tddl.jdbc.group.dbselector.RuntimeWritableAtomDBSelector;
import com.taobao.tddl.jdbc.group.exception.ConfigException;
import com.taobao.tddl.jdbc.group.exception.TAtomDataSourceException;

/**
 * һ��ConfigManager��Ӧһ��TGroupDataSource��
 * ��Ҫ���ڽ�����Group��dataIDȡ�õĶ�Ӧ�����ַ����ţ�����db0:rwp1q1i0, db1:rwp0q0i1����
 * ת��Ϊ������Group���������ϵ�ṹ��һ��Group���������Atom db0 �� db1 �� ������ʹ��һ�� Map<String,
 * DataSourceWrapper> ����ʾ ���е�String Ϊÿ��Atom DS ��dbKey ��DataSourceWrapper
 * Ϊ������װ��TAtomDataSource
 * ---������Ҫ����һ�£�Ϊʲô��ֱ��ʹ��AtomDataSource����Ϊÿ��AtomDataSource������Ӧ��Ȩ�غ����ȼ���Ϣ ��ˣ���Ҫ***����
 *
 *
 * ���У����õ�ÿһ��Atom DataSourceҲֻ����Atom
 * ��dbKey��ʾ����ˣ����ǻ���Ҫ���ݴ�dbKeyȡ��Atom��������Ϣ�����ҽ�����װ��һ��AtomDataSource���� �����Ҫ***����
 *
 * �������map�ܸ���dbKeyѸ�ٵ��ҵ���Ӧ��DatasourceҲ�ǲ����ģ����ǵ�Group��Ӧ���Ƕ�Ӧ��͸���ģ�
 * ��ˣ������ǵĶ�д�������ʱ��Group��Ӧ���ܹ��������õ�Ȩ�غ����ȼ����Զ���ѡ��һ�����ʵ�DB�Ͻ��ж�д��
 * ���ԣ����ǻ���Ҫ��������Ϣ����һ��DBSelector���Զ�����ɸ���Ȩ�ء����ȼ�ѡ����ʵ�Ŀ��� ��ˣ���Ҫ***����
 *
 *
 *
 * @author yangzhu
 * @author linxuan refactor
 *
 */
public class ConfigManager {
	private static final Log logger = LogFactory.getLog(ConfigManager.class);

	private final ConfigDataListener configReceiver; // //��̬����Diamond���͹�������Ϣ
	private ConfigDataHandlerFactory configFactory;
	private ConfigDataHandler globalHandler;

	//add by junyu
	private final ConfigDataListener extraGroupConfigReceiver;
	private ConfigDataHandler extraHandler;
	private ConfigDataHandlerFactory extraFactory;

	private final TGroupDataSource tGroupDataSource;

	private boolean createTAtomDataSource = true;

	private Map<String/* Atom dbIndex */, DataSourceWrapper/* Wrapper����Atom DS */> dataSourceWrapperMap = new HashMap<String, DataSourceWrapper>();

	private volatile GroupExtraConfig groupExtraConfig = new GroupExtraConfig();

	public ConfigManager(TGroupDataSource tGroupDataSource) {
		this.tGroupDataSource = tGroupDataSource;
		this.configReceiver = new ConfigReceiver();
		this.extraGroupConfigReceiver=new ExtraGroupConfigReceiver();
	}

	/**
	 * ��Diamond����������ȡ��Ϣ������TAtomDataSource�����������ȼ���Ϣ�Ķ�дDBSelector ---add by
	 * mazhidan.pt
	 */
	public void init() {
		// ����: ��Ҫ�ڹ���DefaultDiamondManagerʱ��ע��ManagerListener(����:configReceiver)
		// Ҳ����˵����Ҫ������: new DefaultDiamondManager(dbGroupKey, configReceiver)��
		// ����Ҫ���null���ȵ�һ��ȡ����Ϣ��������ɺ���ע�ᣬ�������Բ���ͬ���������κ��벢����ص����⣬
		// ��Ϊ�п����ڵ�һ�θ�ȡ����Ϣ��Diamond���������Ǳ������޸��˼�¼������ManagerListener����߳������յ���Ϣ��
		// ��ɳ�ʼ���̺߳�ManagerListener�߳�ͬʱ������Ϣ��
		configFactory = new DefaultConfigDataHandlerFactory();
		globalHandler = configFactory.getConfigDataHandler(
				tGroupDataSource.getFullDbGroupKey(), null);

		String dsWeightCommaStr = globalHandler.getData(
				tGroupDataSource.getConfigReceiveTimeout(),
				ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY);

		//extra config
		extraFactory=new DefaultConfigDataHandlerFactory();
		extraHandler=extraFactory.getConfigDataHandler(
				tGroupDataSource.getDbGroupExtraConfigKey(), null);
		String extraConfig=extraHandler.getData(tGroupDataSource.getConfigReceiveTimeout(),
				ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY);

		if(extraConfig!=null){
			parseExtraConfig(extraConfig);
			extraHandler.addListener(extraGroupConfigReceiver, null);
		}

		List<DataSourceWrapper> dswList = parse2DataSourceWrapperList(dsWeightCommaStr);
		resetByDataSourceWrapper(dswList);
		globalHandler.addListener(configReceiver, null);
	}

	/**
	 * ������ͨ��DataSource�����дDBSelector
	 */
	public void init(List<DataSourceWrapper> dataSourceWrappers) {
		if ((dataSourceWrappers == null) || dataSourceWrappers.size() < 1) {
			throw new ConfigException("dataSourceWrappers����Ϊnull�ҳ���Ҫ����0");
		}
		createTAtomDataSource = false;
		// update(createDBSelectors2(dataSourceWrappers));
		resetByDataSourceWrapper(dataSourceWrappers);
	}

	private class MyDataSourceFetcher implements DataSourceFetcher {
		private DBType dbType = DBType.MYSQL;

		@Override
		public DataSource getDataSource(String dsKey) {
			DataSourceWrapper dsw = dataSourceWrapperMap.get(dsKey);
			if (dsw != null) {
				dbType = dsw.getDBType();
				return dsw.getWrappedDataSource();
			} else {
				if (createTAtomDataSource) {
					TAtomDataSource atom = createTAtomDataSource(dsKey);
					dbType = DBType.valueOf(atom.getDbType().name());
					return atom;
				} else {
					throw new IllegalArgumentException(dsKey + " not exist!");
				}
			}
		}

		@Override
		public DBType getDataSourceDBType(String key) {
			return dbType;
		}
	};

	// configInfo����: db1:rw, db2:r, db3:r
	private void parse(String dsWeightCommaStr) {
		List<DataSourceWrapper> dswList = parse2DataSourceWrapperList(dsWeightCommaStr);
		resetByDataSourceWrapper(dswList);
	}

	/**
	 * extraConfig is a json format string,include table dataSourceIndex
	 * relation or sql dataSourceIndex relation or default go main db config.
	 * example: {sqlDsIndex: { 0:[sql1,sql2,sql3], 1:[sql0], 2:[sql4]
	 * }, tabDsIndex: { 0:[table1,table2] 1:[table3,table4] },
	 * defaultMain:true}
	 *
	 * @throws JSONException
	 *
	 **/
	@SuppressWarnings("rawtypes")	private void parseExtraConfig(String extraConfig) {
		if(extraConfig==null){
			this.groupExtraConfig.getSqlForbidSet().clear();
			this.groupExtraConfig.getSqlDsIndexMap().clear();
			this.groupExtraConfig.getTableDsIndexMap().clear();
			this.groupExtraConfig.setDefaultMain(false);
		}
		try {
			JSONObject obj = JSONObject.fromObject(extraConfig);
			if(obj.has("sqlForbid")) {
				Set<String> tempSqlForbidSet = new HashSet<String>();
				JSONArray array = obj.getJSONArray("sqlForbid");
				for(int i=0; i<array.size(); i++) {
					String sql=array.getString(i);
					String nomalSql = TStringUtil.fillTabWithSpace(sql
							.trim().toLowerCase());
					if(nomalSql != null && !nomalSql.trim().isEmpty()){
						tempSqlForbidSet.add(nomalSql);
					}
				}
				this.groupExtraConfig.setSqlForbidSet(tempSqlForbidSet);
			}
			else {
				this.groupExtraConfig.getSqlForbidSet().clear();
			}

			if (obj.has("sqlDsIndex")) {
				Map<String, Integer> tempSqlDsIndexMap = new HashMap<String, Integer>();
				JSONObject sqlDsIndex = obj.getJSONObject("sqlDsIndex");
				Iterator it=sqlDsIndex.keys();
				while(it.hasNext()){
					String key=String.valueOf(it.next()).trim();
					Integer index=Integer.valueOf(key);
					JSONArray array=sqlDsIndex.getJSONArray(key);
					for (int i=0;i<array.size();i++) {
						String sql=array.getString(i);
						String nomalSql = TStringUtil.fillTabWithSpace(sql
								.trim().toLowerCase());
						if (tempSqlDsIndexMap.get(nomalSql) == null) {
							tempSqlDsIndexMap.put(nomalSql,index);
						} else {
							// have a nice log
							throw new ConfigException(
									"sql can not be route to different dataSourceIndex:"
											+ sql);
						}
					}
				}
				this.groupExtraConfig.setSqlDsIndexMap(tempSqlDsIndexMap);
			}
			else {
				this.groupExtraConfig.getSqlDsIndexMap().clear();
			}

			if (obj.has("tabDsIndex")) {
				Map<String, Integer> tempTabDsIndexMap = new HashMap<String, Integer>();
				JSONObject sqlDsIndex = obj.getJSONObject("tabDsIndex");
				Iterator it=sqlDsIndex.keys();
				while(it.hasNext()){
					String key=String.valueOf(it.next()).trim();
					Integer index=Integer.valueOf(key);
					JSONArray array=sqlDsIndex.getJSONArray(key);
					for (int i=0;i<array.size();i++) {
						String table=array.getString(i);
						String nomalTable = table.trim().toLowerCase();
						if (tempTabDsIndexMap.get(nomalTable) == null) {
							tempTabDsIndexMap.put(nomalTable, index);
						} else {
							// have a nice log
							throw new ConfigException(
									"table can not be route to different dataSourceIndex:"
											+ table);
						}
					}
				}
				this.groupExtraConfig.setTableDsIndexMap(tempTabDsIndexMap);
			}
			else {
				this.groupExtraConfig.getTableDsIndexMap().clear();
			}

			if (obj.has("defaultMain")) {
				this.groupExtraConfig.setDefaultMain(obj.getBoolean("defaultMain"));
			}
			else {
				this.groupExtraConfig.setDefaultMain(false);
			}

		} catch (JSONException e) {
			throw new ConfigException(
					"group extraConfig is not json valid string:" + extraConfig,
					e);
		}
	}

	/**
	 * ����: ���ŵ�λ�ú���Ҫ��Ҫ������������������Ҳ��Ҫ��Ϊ��ʡ�Ե��� ���ݿ�ĸ��� =
	 * ���ŵĸ���+1����0��1��2...��ţ�����"db1,,db3"��ʵ������3�����ݿ⣬
	 * ҵ���ͨ����һ��ThreadLocal������ThreadLocal�о�������������š�
	 */
	private List<DataSourceWrapper> parse2DataSourceWrapperList(
			String dsWeightCommaStr) {
		logger.info("[parse2DataSourceWrapperList]dsWeightCommaStr="
				+ dsWeightCommaStr);
		if ((dsWeightCommaStr == null)
				|| (dsWeightCommaStr = dsWeightCommaStr.trim()).length() == 0) {
			throw new ConfigException("��dbGroupKey:'"
					+ tGroupDataSource.getFullDbGroupKey()
					+ "'��Ӧ��������Ϣ����Ϊnull�ҳ���Ҫ����0");
		}
		return buildDataSourceWrapper(dsWeightCommaStr,
				new MyDataSourceFetcher());
	}

	/**
	 * ����װ�õ�AtomDataSource���б���һ����װΪ���Ը���Ȩ�����ȼ����ѡ��ģ����DBSelector ---add by
	 * mazhidan.pt
	 *
	 * @param dswList
	 */
	private void resetByDataSourceWrapper(List<DataSourceWrapper> dswList) {
		// ɾ���Ѿ������ڵ�DataSourceWrapper
		Map<String, DataSourceWrapper> newDataSourceWrapperMap = new HashMap<String, DataSourceWrapper>(
				dswList.size());
		for (DataSourceWrapper dsw : dswList) {
			newDataSourceWrapperMap.put(dsw.getDataSourceKey(), dsw);
		}
		Map<String, DataSourceWrapper> old = this.dataSourceWrapperMap;
		this.dataSourceWrapperMap = newDataSourceWrapperMap;
		old.clear();
		old = null;

		DBSelector r_DBSelector = null;
		DBSelector w_DBSelector = null;

		// ���ֻ��һ��db������OneDBSelector
		if (dswList.size() == 1) {
			DataSourceWrapper dsw2 = dswList.get(0);
			r_DBSelector = new OneDBSelector(dsw2);
			r_DBSelector.setDbType(dsw2.getDBType());
			w_DBSelector = r_DBSelector;
		} else {
			// ��д���ȼ�Map
			Map<Integer/* ���ȼ� */, List<DataSourceWrapper>/* ���ȼ�Ϊkey��DS �б� */> rPriority2DswList = new HashMap<Integer, List<DataSourceWrapper>>();
			Map<Integer, List<DataSourceWrapper>> wPriority2DswList = new HashMap<Integer, List<DataSourceWrapper>>();
			for (DataSourceWrapper dsw1 : dswList) {
				add2LinkedListMap(rPriority2DswList, dsw1.getWeight().p, dsw1);
				add2LinkedListMap(wPriority2DswList, dsw1.getWeight().q, dsw1);
			}
			r_DBSelector = createDBSelector(rPriority2DswList, true);
			w_DBSelector = createDBSelector(wPriority2DswList, false);
		}

		r_DBSelector.setReadable(true);
		w_DBSelector.setReadable(false);

		this.readDBSelectorWrapper = r_DBSelector;
		this.writeDBSelectorWrapper = w_DBSelector;

		if (tGroupDataSource.getAutoSelectWriteDataSource())
			runtimeWritableAtomDBSelectorWrapper = new RuntimeWritableAtomDBSelector(
					dataSourceWrapperMap, groupExtraConfig);

		// System.out.println("dataSourceWrapperMap=" + dataSourceWrapperMap);
		if (this.dataSourceChangeListener != null) {
			dataSourceChangeListener.onDataSourceChanged(null);// ҵ��ͨ��getDataSource()��ȡ���º�Ľ��
		}
	}

	private DataSourceChangeListener dataSourceChangeListener;

	public void setDataSourceChangeListener(
			DataSourceChangeListener dataSourceChangeListener) {
		this.dataSourceChangeListener = dataSourceChangeListener;
	}

	/*
	 * //���ص�����Ԫ�ظ����̶���2����1����read���ڶ�����write private DBSelector[]
	 * createDBSelectors(List<String> dbKeyAndWeightList) { DBSelector
	 * r_DBSelector = null; DBSelector w_DBSelector = null;
	 *
	 * //���ֻ��һ��db������OneDBSelector if (dbKeyAndWeightList.size() == 1) { String[]
	 * dbKeyAndWeight = split(dbKeyAndWeightList.get(0), ":"); DataSourceWrapper
	 * dsw = createDataSourceWrapper(dbKeyAndWeight[0], (dbKeyAndWeight.length
	 * == 2 ? dbKeyAndWeight[1] : null), 0); //ֻ��һ������Դʱ������Դ����Ϊ0
	 *
	 * r_DBSelector = new OneDBSelector(dsw);
	 * r_DBSelector.setDbType(dsw.getDBType()); w_DBSelector = r_DBSelector; }
	 * else { List<List<DataSourceWrapper>> rDataSourceWrappers = new
	 * ArrayList<List<DataSourceWrapper>>(); List<List<DataSourceWrapper>>
	 * wDataSourceWrappers = new ArrayList<List<DataSourceWrapper>>();
	 *
	 * for (int i = 0; i < dbKeyAndWeightList.size(); i++) { String[]
	 * dbKeyAndWeight = split(dbKeyAndWeightList.get(i), ":");
	 *
	 * DataSourceWrapper dsw = createDataSourceWrapper(dbKeyAndWeight[0],
	 * (dbKeyAndWeight.length == 2 ? dbKeyAndWeight[1] : null), i);
	 *
	 * insertSort(rDataSourceWrappers, dsw, true);
	 * insertSort(wDataSourceWrappers, dsw, false); }
	 *
	 * r_DBSelector = createDBSelector(rDataSourceWrappers, true); w_DBSelector
	 * = createDBSelector(wDataSourceWrappers, false); }
	 *
	 * r_DBSelector.setReadable(true); w_DBSelector.setReadable(false);
	 *
	 * return new DBSelector[] { r_DBSelector, w_DBSelector }; }
	 */

	/**
	 * ��������k ���ȼ� ����������ȼ���Ӧ��V list ���档 ----��Ϊ�����ж��DS������ͬ�����ȼ� ---add by
	 * mazhidan.pt
	 */
	private static <K, V> void add2LinkedListMap(Map<K, List<V>> m, K key,
			V value) {
		// ��Map����ȡ��������ȼ���List
		List<V> c = (List<V>) m.get(key);
		// ���Ϊ�գ���newһ��
		if (c == null) {
			c = new LinkedList<V>();
			m.put(key, c);
		}
		// ��Ϊ�գ��ں���add()
		c.add(value);
	}

	/**
	 * @param dsWeightCommaStr
	 *            : ���� db0:rwp1q1i0, db1:rwp0q0i1
	 */
	public static List<DataSourceWrapper> buildDataSourceWrapper(
			String dsWeightCommaStr, DataSourceFetcher fetcher) {
		String[] dsWeightArray = dsWeightCommaStr.split(","); // ���ŷָ���db0:rwp1q1i0,
																// db1:rwp0q0i1
		List<DataSourceWrapper> dss = new ArrayList<DataSourceWrapper>(
				dsWeightArray.length);
		for (int i = 0; i < dsWeightArray.length; i++) {
			String[] dsAndWeight = dsWeightArray[i].split(":"); // ð�ŷָ���db0:rwp1q1i0
			String dsKey = dsAndWeight[0].trim();
			String weightStr = dsAndWeight.length == 2 ? dsAndWeight[1] : null;

			// ������group����һ����ʵdataSource�����������group����
			// ���dataSource������ �������һ��dataSource������Ϊ׼
			DataSource dataSource = fetcher.getDataSource(dsKey);
			DBType fetcherDbType = fetcher.getDataSourceDBType(dsKey);
			// dbType = fetcherDbType == null ? dbType : fetcherDbType;
			DataSourceWrapper dsw = new DataSourceWrapper(dsKey, weightStr,
					dataSource, fetcherDbType, i);
			dss.add(dsw);
		}
		return dss;
	}

	/**
	 * ���ݸ����ľ��ж�д���ȼ���ÿ�����ȼ���Ӧ��DataSource�����Map������DBSelector---add by mazhidan.pt
	 *
	 * @param priority2DswList
	 * @param isRead
	 * @return
	 */
	private DBSelector createDBSelector(
			Map<Integer/* ���ȼ� */, List<DataSourceWrapper>> priority2DswList,
			boolean isRead) {
		if (priority2DswList.size() == 1) { // ֻ��һ�����ȼ�ֱ��ʹ��EquityDbManager
			return createDBSelector2(priority2DswList.entrySet().iterator()
					.next().getValue(), isRead);
		} else {
			List<Integer> priorityKeys = new LinkedList<Integer>();
			priorityKeys.addAll(priority2DswList.keySet());
			Collections.sort(priorityKeys); // ���ȼ���С��������
			EquityDbManager[] priorityGroups = new EquityDbManager[priorityKeys
					.size()];
			for (int i = 0; i < priorityGroups.length; i++) { // �������ȼ��ŵ���ǰ��
				List<DataSourceWrapper> dswList = priority2DswList
						.get(priorityGroups.length - 1 - i); // ����
				// PriorityDbGroupSelector����EquityDbManager�׳���NoMoreDataSourceException��ʵ�֣�
				// �������Ｔʹֻ��һ��dsҲֻ����Ȼ��EquityDbManager
				priorityGroups[i] = createEquityDbManager(dswList, isRead, groupExtraConfig);


			}
			return new PriorityDbGroupSelector(priorityGroups);
		}
	}

	private AbstractDBSelector createDBSelector2(
			List<DataSourceWrapper> dswList, boolean isRead) {
		AbstractDBSelector dbSelector;
		if (dswList.size() == 1) {
			DataSourceWrapper dsw = dswList.get(0);
			dbSelector = new OneDBSelector(dsw);
			dbSelector.setDbType(dsw.getDBType());
		} else {
			dbSelector = createEquityDbManager(dswList, isRead,
					groupExtraConfig);
		}
		return dbSelector;
	}

	/*
	 * private DBSelector createDBSelector(List<List<DataSourceWrapper>> list,
	 * boolean isRead) {
	 *
	 * int size = list.size(); //���ȼ������ if (size == 1) {
	 * //ֻ��һ�����ȼ�ֱ��ʹ��EquityDbManager return createEquityDbManager(list.get(0),
	 * isRead); } else { EquityDbManager[] priorityGroups = new
	 * EquityDbManager[size]; for (int i = 0; i < size; i++) { priorityGroups[i]
	 * = createEquityDbManager(list.get(i), isRead); } return new
	 * PriorityDbGroupSelector(priorityGroups); } }
	 */

	private static EquityDbManager createEquityDbManager(
			List<DataSourceWrapper> list, boolean isRead,
			GroupExtraConfig groupExtraConfig) {
		Map<String, DataSourceWrapper> dataSourceMap = new HashMap<String, DataSourceWrapper>(
				list.size());
		Map<String, Integer> weightMap = new HashMap<String, Integer>(
				list.size());

		DBType dbType = null;
		for (DataSourceWrapper dsw : list) {
			String dsKey = dsw.getDataSourceKey();
			dataSourceMap.put(dsKey, dsw);
			weightMap
					.put(dsKey, isRead ? dsw.getWeight().r : dsw.getWeight().w);

			if (dbType == null) {
				dbType = dsw.getDBType();
			}
		}
		EquityDbManager equityDbManager = new EquityDbManager(dataSourceMap,
				weightMap, groupExtraConfig);
		equityDbManager.setDbType(dbType);
		return equityDbManager;
	}

	/**
	 * ��Ϊ����Դ����ͨ����3�����ң���������ʹ���˼򵥵Ĳ��������㷨��
	 * ����List(��:List<List<DataSourceWrapper>>)�����������ֽṹ��
	 * ��һ��List(����)�������ȼ������ȼ���ߵ�����List��0��λ�ô��������1��λ�ã��������ƣ�
	 * �ڶ���List(����)��ʾ��ͬ���ȼ��Ķ������Դ�� ---- |p9|-->|db0|-->|db2| ---- |p8|-->|db1|
	 * ---- |p7|-->|db3| ----
	 *
	 * @param priorityList
	 *            ��һ�����ź��������Դ�б��ɵ�����ȷ����Ϊnull��
	 * @param dsw
	 *            ��ǰ��Ҫ���뵽�б������Դ
	 * @param isRead
	 *            ��Ϊ���ȼ��ֶ�д���ȼ�������p��ʾ��д��q��ʾ�����isReadΪtrue��ʹ��P��������q��
	 */
	/*
	 * public static void insertSort(List<List<DataSourceWrapper>> priorityList,
	 * DataSourceWrapper dsw, boolean isRead) {
	 * //�������д��Ȩ��Ϊ0����ô�ͱ�ʾ������Դ��Ӧ�����ݿⲻ�ɶ��򲻿�д����ʱʲô�����������Դ�����Դ�� // if ((isRead &&
	 * dsw.getWeight().r == 0) || (!isRead && dsw.getWeight().w == 0)) //
	 * return;
	 *
	 * List<DataSourceWrapper> samePriorityDataSourceWrappers;
	 *
	 * int newPriority = isRead ? dsw.getWeight().p : dsw.getWeight().q;
	 *
	 * int index = 0; int size = priorityList.size();
	 *
	 * while (index < size) { samePriorityDataSourceWrappers =
	 * priorityList.get(index);
	 *
	 * //ȥ��priorityList�е���ЧԪ�أ���ֹ��ָ���쳣���±�Խ���쳣�� if (samePriorityDataSourceWrappers
	 * == null || samePriorityDataSourceWrappers.size() == 0) {
	 * priorityList.remove(index); size--; continue; }
	 *
	 * Weight oldWeight = samePriorityDataSourceWrappers.get(0).getWeight(); int
	 * oldPriority = isRead ? oldWeight.p : oldWeight.q;
	 *
	 * if (newPriority == oldPriority) { //���ﲻ�ð�Ȩ��������
	 * samePriorityDataSourceWrappers.add(dsw); return; } else if (newPriority >
	 * oldPriority) { break; } else { index++; } }
	 *
	 * //û���ҵ���ͬ���ȼ�ʱ�²���һ�����ȼ� (��size=0ʱҲ���ߵ�����) samePriorityDataSourceWrappers =
	 * new ArrayList<DataSourceWrapper>();
	 * samePriorityDataSourceWrappers.add(dsw); priorityList.add(index,
	 * samePriorityDataSourceWrappers); }
	 */
	/*
	 * private DataSourceWrapper createDataSourceWrapper(String dsKey, String
	 * weightStr, int dataSourceIndex) { aliveDataSourceKeys.add(dsKey);
	 *
	 * DataSourceWrapper dsw = dataSourceWrapperMap.get(dsKey); if (dsw != null)
	 * { //dsw.setWeightStr(weightStr);
	 * //dsw.setDataSourceIndex(dataSourceIndex); } else { if
	 * (createTAtomDataSource) { TAtomDataSource ads =
	 * createTAtomDataSource(dsKey); dsw = new DataSourceWrapper(dsKey,
	 * weightStr, ads, getDBTypeFrom(ads), dataSourceIndex);
	 * dataSourceWrapperMap.put(dsKey, dsw); } else { throw new
	 * IllegalArgumentException(dsKey + " not exist!"); } } return dsw; }
	 */

	/**
	 * ��Ϊ���ǵĵ�Group�����У����õ�AtomDataSource���������DataSource��dbkey��ʾ
	 * ���ԣ�������Ҫ�����dbkey��Diamond��ȡ�����Ƕ�Ӧ��������Ϣ����������������AtomDataSource ---add by
	 * mazhidan.pt
	 */
	private TAtomDataSource createTAtomDataSource(String dsKey) {
		TAtomDataSource ads = null;
		try {
			ads = new TAtomDataSource();
			ads.setAppName(tGroupDataSource.getAppName());
			ads.setDbKey(dsKey);

			ads.init(); // TAtomDataSource��init()��throws Exception

			ads.setLogWriter(tGroupDataSource.getLogWriter());
			ads.setLoginTimeout(tGroupDataSource.getLoginTimeout());
		} catch (Exception e) {
			throw new TAtomDataSourceException("TAtomDataSource�޷���ʼ��: dsKey="
					+ dsKey, e);
		}
		return ads;
	}

	/**
	 * ���ݵ�ǰ�Ķ�д״̬���������Դ�Ƿ���ã�����Դ�����֣�TAtomDataSource����ͨ������Դ(��DBCP����Դ)
	 *
	 * @param ds
	 *            Ҫ��������Դ
	 * @param isRead
	 *            �Ƕ�����Դ���ж�����(isRead=true)������д����(isRead=false)
	 * @return ��ͨ������Դ���ܵ�ǰ�Ķ�д״̬��ʲô�����ǿ��õģ�����true��
	 *         TAtomDataSource�����ǰ��״̬��NA����false, �������WR״̬�Լ�isRead��ֵ����
	 */
	public static boolean isDataSourceAvailable(DataSource ds, boolean isRead) {
		if (ds instanceof DataSourceWrapper)
			ds = ((DataSourceWrapper) ds).getWrappedDataSource();

		if (!(ds instanceof TAtomDataSource))
			return true;

		AtomDbStatusEnum status = ((TAtomDataSource) ds).getDbStatus();
		if (status.isNaStatus())
			return false;

		if (status.isRstatus() && isRead)
			return true;
		if (status.isWstatus() && !isRead)
			return true;

		return false;
	}

	/**
	 * ������TGroupDataSource��TGroupConnection�������ط���DBSelector��Ϊһ���ֶα���������
	 * ����dbȨ�����ñ���֮���޷�ʹ�����µ�Ȩ������
	 */
	private volatile DBSelector readDBSelectorWrapper;
	private volatile DBSelector writeDBSelectorWrapper;
	private volatile DBSelector runtimeWritableAtomDBSelectorWrapper;

	/**
	 * �����Ƕ�����д��ѡ���Ӧ��DBSelector---add by mazhidan.pt
	 */
	public DBSelector getDBSelector(boolean isRead,
			boolean autoSelectWriteDataSource) {
		DBSelector dbSelector = isRead ? readDBSelectorWrapper
				: writeDBSelectorWrapper;
		if (!isRead && autoSelectWriteDataSource) {
			// ��Ϊ����dbSelector�ڲ���TAtomDataSource����ָ��ͬһ��ʵ�������ĳһ��TAtomDataSource��״̬���ˣ�
			// ��ô���а������TAtomDataSource��dbSelector����֪��״̬�ı��ˣ�
			// ����ֻҪ��һ��TAtomDataSource��״̬���W��
			// ��ô�������dbSelector��ר�����ڶ��ģ�����ר������д�ģ�Ҳ�����ǲ���runtimeWritableAtomDBSelector��
			// ֻҪ������hasWritableDataSource()���᷵��true

			// if(!dbSelector.hasWritableDataSource())
			dbSelector = runtimeWritableAtomDBSelectorWrapper;
		}
		return dbSelector;
	}

	private class ConfigReceiver implements ConfigDataListener {
		public void onDataRecieved(String dataId, String data) {
			try {
				parse(data);
			} catch (Throwable t) {
				logger.error("��̬����������Ϣʱ���ִ���:" + data, t);
			}
		}
	}

	private class ExtraGroupConfigReceiver implements ConfigDataListener{
		@Override
		public void onDataRecieved(String dataId, String data) {
			logger.info("receive group extra data:"+data);
			parseExtraConfig(data);
		}
	}

	// �����ڲ���
	public void receiveConfigInfo(String configInfo) {
		configReceiver.onDataRecieved(null, configInfo);
	}

	// �����ڲ���
	public void resetDbGroup(String configInfo) {
		try {
			parse(configInfo);
		} catch (Throwable t) {
			logger.error("resetDbGroup failed:" + configInfo, t);
		}
	}
}

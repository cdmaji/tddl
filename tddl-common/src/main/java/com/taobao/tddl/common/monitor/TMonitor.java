/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	//package com.taobao.tddl.common.monitor;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.Map.Entry;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.locks.ReentrantLock;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
///**
// * guangxia ��monitor���Ǻ��ܹ�������������������һ���µ�monitor
// * 
// * ����key�ģ���ص��ӿڵ�
// * 
// * @author shenxun
// * @author junyu
// * 
// */
//public class TMonitor {
//	private static final Log logger = LogFactory.getLog(TMonitor.class);
//
//	/**
//	 * ��λ�Ǻ���
//	 */
//	private static volatile long statInterval = 2 * 60 * 1000;
//
//	/**
//	 * ��Ҫ�޸�
//	 */
//	private static final long cleanInterval = 30 * 60 * 1000;
//	private static int limit = 500;
//
//	private static List<LogOutputListener> monitorLogListeners = new LinkedList<LogOutputListener>();
//	private static List<SnapshotValuesOutputCallBack> snapshotValueCallBack = new LinkedList<SnapshotValuesOutputCallBack>();
//
//	private static volatile boolean started = false;
//
//	private static volatile ConcurrentHashMap<String, Values> currentStatMapNeedLimit = new ConcurrentHashMap<String, Values>();
//	private static volatile ConcurrentHashMap<String, Values> currentStatMapWithOutLimit = new ConcurrentHashMap<String, Values>();
//	private static final ReentrantLock lockLimit = new ReentrantLock();
//	private static CycleExecuteTimer outPutTimer;
//	private static CycleExecuteTimer cleanTimer;
//
//	static {
//		start();
//	}
//
//	private static void start() {
//		if (started) {
//			return;
//		}
//
//		/**
//		 * �����������־������
//		 * 
//		 * 1.CallBack����־�� 2.����key����־�� 3.���޶�key����־��
//		 * 
//		 * ����ͬʱ��շ��޶�key��valueֵ��
//		 * 
//		 * ÿ��2����ִ��һ�Ρ�
//		 */
//		outPutTimer = new CycleExecuteTimer("LogOutPutTask", new Runnable() {
//			public void run() {
//				writeCallBackLog();
//				ConcurrentHashMap<String, Values> newMap = copyNewWithOutLimitMap();
//				ConcurrentHashMap<String, Values> oldMap = currentStatMapWithOutLimit;
//				currentStatMapWithOutLimit = newMap;
//				writeLogMapToFile(currentStatMapNeedLimit);
//				writeLogMapToFile(oldMap);
//				oldMap.clear();
//			}
//		}, statInterval, TimeUnit.MILLISECONDS, null);
//
//		outPutTimer.start();
//
//		/**
//		 * �����������key����Map������ Ĭ����00:00:00,00:30:00,01:00:00,...,23:30:00����
//		 */
//		cleanTimer = new CycleExecuteTimer(
//				"LimitLogCleanTask", new Runnable() {
//					public void run() {
//						resetLimitMap();
//					}
//				}, cleanInterval, TimeUnit.MILLISECONDS, new HalfTimeComputer());
//
//		cleanTimer.start();
//		addOutputListener(DefaultLogOutputListener.getInstance());
//		started = true;
//		logger.warn("tddl monitor start...");
//
//	}
//
//	/**
//	 * ��������������map
//	 */
//	private static void resetLimitMap() {
//		lockLimit.lock();
//		try {
//			// ԭ�ӵ����size��������
//			size = 0;
//			ConcurrentHashMap<String, Values> oldMap = currentStatMapNeedLimit;
//
//			currentStatMapNeedLimit = new ConcurrentHashMap<String, Values>(
//					limit);
//			// help gc
//			oldMap.clear();
//			logger.warn("���key��������Map");
//		} finally {
//			lockLimit.unlock();
//		}
//	}
//
//	/**
//	 * ֻ���value,����key
//	 */
//	private static ConcurrentHashMap<String, Values> copyNewWithOutLimitMap() {
//		Set<String> keySet = currentStatMapWithOutLimit.keySet();
//		ConcurrentHashMap<String, Values> keepKeysMap = new ConcurrentHashMap<String, Values>();
//		for (String key : keySet) {
//			keepKeysMap.put(key, new Values());
//		}
//		return keepKeysMap;
//	}
//
//	/**
//	 * ��ȡ�Զ�����־���ݲ���ӡ(���̣߳�������)
//	 */
//	private static void writeCallBackLog() {
//		ConcurrentHashMap<String, Values> tempMap = new ConcurrentHashMap<String, Values>();
//		for (SnapshotValuesOutputCallBack callBack : snapshotValueCallBack) {
//			ConcurrentHashMap<String, Values> values = callBack.getValues();
//			Map<String, Values> copiedMap = new HashMap<String, TMonitor.Values>(
//					values);
//			for (Entry<String, Values> entry : copiedMap.entrySet()) {
//				Values value = tempMap.get(entry.getKey());
//				if (null == value) {
//					value = new Values();
//					tempMap.putIfAbsent(entry.getKey(), value);
//				}
//				value.value1.addAndGet(entry.getValue().value1.get());
//				value.value2.addAndGet(entry.getValue().value2.get());
//			}
//		}
//
//		writeLogMapToFile(tempMap);
//	}
//
//	/**
//	 * ���ڴ������������־��
//	 * 
//	 * @param oldMap
//	 */
//	private static void writeLogMapToFile(ConcurrentHashMap<String, Values> map) {
//		for (LogOutputListener listener : monitorLogListeners) {
//			listener.actionPerform(map, System.currentTimeMillis());
//		}
//	}
//
//	/**
//	 * ���һ����־��Ϣ��������key������map�У���ʱˢ�����ļ���־�� ˢ����־ʱ����key�����value��
//	 * 
//	 * @param key
//	 * @param value1
//	 * @param value2
//	 */
//	private static void add(String key, long value1, long value2) {
//		Values values = currentStatMapWithOutLimit.get(key);
//
//		if (null == values) {
//			Values newValues = new Values();
//			Values alreadyValues = currentStatMapWithOutLimit.putIfAbsent(key,
//					newValues);
//			if (null == alreadyValues) {
//				// ��ʾԭ��put�ɹ�
//				values = newValues;
//			} else {
//				// ��ʾԭ��putʱ�Ѿ���ֵ�ˡ�
//				values = alreadyValues;
//			}
//		}
//
//		values.value1.addAndGet(value1);
//		values.value2.addAndGet(value2);
//	}
//
//	/**
//	 * ���һ����־��Ϣ������key������map�У���ʱ������ļ���־��. 1.���key������,���ҵ�ǰmap key����С���޶�ֵ����ô����
//	 * һ���µ�<key,value>�ṹ���뵽map�С� 2.���key������,���ҵ�ǰmap key���������޶�ֵ, ��ô���� ������־��Ϣ��
//	 * 3.���key����,��ô����keyȡ��value���Ҽ����µ�ֵ��
//	 * 
//	 * ˢ����־ʱ�����key,value,����Сʱ������־map
//	 * 
//	 * @param key
//	 * @param value1
//	 * @param value2
//	 */
//	private static void addWithLimit(String key, long value1, long value2) {
//		if (key == null || key.length() == 0) {
//			return;
//		}
//		key = "limit_" + key;
//
//		Values values = currentStatMapNeedLimit.get(key);
//		/**
//		 * 1. �ж�value�Ƿ�Ϊnull 2. �ж��Ƿ�������� ���� �ж�value�Ƿ�Ϊnull �ж��Ƿ�������� ��� ���� ����
//		 */
//		if (null == values) {
//			if (size + 1 > limit) {
//				// ���size ������������ˣ���ôֱ�ӷ��أ�
//				logger.debug("size ������Χ������");
//				return;
//			} else {
//				// ��ô������size���������ӣ� ����û�����key����Ӧ��value
//
//				lockLimit.lock();
//				try {
//					// ˫���
//					values = currentStatMapNeedLimit.get(key);
//					if (null == values) {
//
//						if (size + 1 > limit) {
//							return;
//						}
//						// ��֤size++ ��put��һ��ԭ�Ӳ���
//						size++;
//						values = new Values();
//						currentStatMapNeedLimit.put(key, values);
//					}
//				} finally {
//					lockLimit.unlock();
//				}
//			}
//		}
//
//		values.value1.addAndGet(value1);
//		values.value2.addAndGet(value2);
//
//	}
//
//	private static volatile int size = 0; // �ƺ��ǿ��Բ���volatile�ģ����������
//
//	public static List<LogOutputListener> getOutputListener() {
//		return monitorLogListeners;
//	}
//	public static synchronized void removeSnapshotValuesCallback(SnapshotValuesOutputCallBack callbackList){
//		snapshotValueCallBack.remove(callbackList);
//	}
//	public static synchronized void addSnapshotValuesCallbask(
//			SnapshotValuesOutputCallBack callbackList) {
//		if (snapshotValueCallBack.contains(callbackList)) {
//			// only one instance is allowed
//			return;
//		}
//		snapshotValueCallBack.add(callbackList);
//	}
//
//	public static synchronized void addOutputListener(LogOutputListener listener) {
//		if (monitorLogListeners.contains(listener)) {
//			// only one instance is allowed
//			return;
//		}
//		monitorLogListeners.add(listener);
//	}
//
//	public static long getStatInterval() {
//		return statInterval;
//	}
//
//	public static synchronized void setStatInterval(long statIntervals) {
//		statInterval = statIntervals;
//	}
//
//	public static void reStartMonitor(){
//		outPutTimer.stop();
//		cleanTimer.stop();
//		started=false;
//		start();
//	}
//
//	public static class Values {
//		public final AtomicLong value2 = new AtomicLong(0L);
//		public final AtomicLong value1 = new AtomicLong(0L);
//
//		@Override
//		public String toString() {
//			StringBuilder sb = new StringBuilder();
//			long v1 = value1.get();
//			long v2 = value2.get();
//
//			sb.append("[").append(v1).append(":").append(v2).append("]");
//			return sb.toString();
//		}
//	}
//}

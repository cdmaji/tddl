/*(C) 2007-2012 Alibaba Group Holding Limited.	
 *This program is free software; you can redistribute it and/or modify	
*it under the terms of the GNU General Public License version 2 as	
* published by the Free Software Foundation.	
* Authors:	
*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	
*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	
*/	
package com.taobao.tddl.common.monitor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.taobao.tddl.common.LoggerInit;

/**
 * �������д��־���ߡ������tps�ͳ����־��̫�������
 * ��ͳ����־���ڴ���������ϲ�����ʱˢ����
 * add(key1(ͳ��Ŀ��),key2(group),key3(flag),timeuse)
 * 
 * �ڴ��м�ˢ����ĽṹΪ��
 * sql(md5) dbname/app   flag     count(sum) time(sum)    min         max         
 * sql      logicDbName  ִ�гɹ�  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      realDbName1  ִ�гɹ�  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      realDbName2  ִ�гɹ�  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      realDbName2  ִ��ʧ��  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      realDbName2  ִ�г�ʱ  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      null         �����ɹ�  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      null         ����ʧ��  ִ�д���   ��Ӧʱ��   ��С��Ӧʱ�� �����Ӧʱ��
 * sql      null         ��������  ִ�д���   ���д���     NA          NA
 * 
 * �������־�����������ɵı�������ǣ�
 * sql dbname/app �ɹ�����  �ɹ�ƽ����Ӧʱ�� �ɹ���С��Ӧʱ�� �ɹ������Ӧʱ��  ʧ�ܴ���  ʧ��ƽ����Ӧʱ�� ʧ����С��Ӧʱ�� ʧ�������Ӧʱ��
 * 
 * key̫������⣺
 * �ö���map����map��ʱ��ˢ��ִ�д�����С��1/3���ݡ������ĺô��ǲ���ÿ��get/put�����򡣲���Ƶ��ˢ����
 * �������ؾ��󲿷��ȵ�key�����������൱�ڶԷ��ȵ��key��������д�롣
 * 
 * �����ã�
 * ��Ϊ�ۼ���һ��ʱ���ڵ�ִ�д�������Ӧʱ�䣬����ͬʱ��Ϊʱ��Ƭ��ʽ��ʵʱ��ر��������Ǳ����ļ��ʱ�����Ҫ���С
 * 
 * @author linxuan
 *
 */
public class AtomBufferedStatLogWriter {
	public static final Logger statlog = LoggerInit.TDDL_Atom_Statistic_LOG;
	public static final String logFieldSep = "#@#"; // sql�г��ָ���С��������ʽ����ͻ
	public static final String linesep = System.getProperty("line.separator");
	public static volatile int maxkeysize = 2048;
	public static volatile int dumpInterval = 300; // ��λ�롣Ĭ��5����ȫ��ˢ��һ��
	public static final SimpleDateFormat df = new SimpleDateFormat("yyy-MM-dd HH:mm:ss:SSS");

	private static LogWriter logWriter = new LogWriter() {
		private void addLine(StringBuilder sb, Object key, Object group, Object flag, StatCounter sc, String time) {
			sb.append(key).append(logFieldSep).append(group).append(logFieldSep).append(sc.getAttach1())
			        .append(logFieldSep).append(sc.getAttach2()).append(logFieldSep).append(sc.getAttach3())
			        .append(logFieldSep).append(flag).append(logFieldSep)
					.append(sc.getCount()).append(logFieldSep).append(sc.getValue()).append(logFieldSep).append(
							sc.getMin()).append(logFieldSep).append(sc.getMax()).append(logFieldSep).append(time)
					.append(linesep);
		}

		public void writeLog(Map<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> map) {
			statlog.debug(Thread.currentThread().getName() + "[writeLog]map.size()=" + map.size() + linesep);
			StringBuilder sb = new StringBuilder();
			String time = df.format(new Date());
			for (Map.Entry<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> e0 : map
					.entrySet()) {
				for (Map.Entry<Object, ConcurrentHashMap<Object, StatCounter>> e1 : e0.getValue().entrySet()) {
					for (Map.Entry<Object, StatCounter> e2 : e1.getValue().entrySet()) {
						StatCounter sc = e2.getValue();
						addLine(sb, e0.getKey(), e1.getKey(), e2.getKey(), sc, time);
					}
				}
			}
			statlog.warn(sb);
		}
	};

	public static void setLogWriter(LogWriter logWriter) {
		AtomBufferedStatLogWriter.logWriter = logWriter;
	}

	public static interface LogWriter {
		void writeLog(Map<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> map);
	}

	static class StatCounter {
		private final AtomicLong count = new AtomicLong(0L);
		private final AtomicLong value = new AtomicLong(0L);
		private final AtomicLong min = new AtomicLong(Long.MAX_VALUE); // value��Сֵ
		private final AtomicLong max = new AtomicLong(Long.MIN_VALUE); // value���ֵ
        private final Object attach1;
        private final Object attach2;
        private final Object attach3;
        public StatCounter(Object attach1,Object attach2,Object attach3){
        	this.attach1=attach1;
        	this.attach2=attach2;
        	this.attach3=attach3;
        }
		
		public void add(long c, long v) {
			// this.count.incrementAndGet();
			this.count.addAndGet(c);
			this.value.addAndGet(v);
			while (true) {
				long vmin = min.get();
				if (v < vmin) {
					if (min.compareAndSet(vmin, v)) {
						break;
					}
					continue; // �п����Ѿ��������߳�������һ����С�ģ����Լ����ж�
				}
				break;
			}
			while (true) {
				long vmax = max.get();
				if (v > vmax) {
					if (max.compareAndSet(vmax, v)) {
						break;
					}
					continue; // �п����Ѿ��������߳�������һ���δ�ģ����Լ����ж�
				}
				break;
			}
		}

		public synchronized void reset() {
			this.count.set(0L);
			this.value.set(0L);
			this.min.set(Long.MAX_VALUE);
			this.max.set(Long.MIN_VALUE);
		}

		public long getCount() {
			return this.count.get();
		}

		public long getValue() {
			return this.value.get();
		}

		public long getMin() {
			return this.min.get();
		}

		public long getMax() {
			return this.max.get();
		}

		public Object getAttach1() {
			return attach1;
		}

		public Object getAttach2() {
			return attach2;
		}

		public Object getAttach3() {
			return attach3;
		}

		public long[] get() {
			return new long[] { this.count.get(), this.value.get(), this.min.get(), this.max.get() };
		}
	}

	private static ConcurrentHashMap<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> keys = new ConcurrentHashMap<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>>(
			maxkeysize, 0.75f, 32);

	public static void add(Object key, Object group, Object flag, long timeuse) {
		add(key, group, flag, 1, timeuse);
	}
	
	public static void add(Object key, Object group, Object flag, long count, long timeuse) {
		add(key, group,flag,"--","--","--",1, timeuse);
	}

	public static void add(Object key, Object group,Object flag,Object attach1,Object attach2,Object attach3,long count, long timeuse) {
		ConcurrentHashMap<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> oldkeys = keys;
		ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>> groups = oldkeys.get(key);
		if (groups == null) {
			ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>> newGroups = new ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>();
			groups = oldkeys.putIfAbsent(key, newGroups);
			if (groups == null) {
				groups = newGroups;
				insureSize();
			}
		}
		ConcurrentHashMap<Object, StatCounter> flags = groups.get(group);
		if (flags == null) {
			ConcurrentHashMap<Object, StatCounter> newFlags = new ConcurrentHashMap<Object, StatCounter>();
			flags = groups.putIfAbsent(group, newFlags);
			if (flags == null) {
				flags = newFlags;
			}
		}
		StatCounter counter = flags.get(flag);
		if (counter == null) {
			StatCounter newCounter = new StatCounter(attach1,attach2,attach3);
			counter = flags.putIfAbsent(flag, newCounter);
			if (counter == null) {
				counter = newCounter;
			}
		}
		counter.add(count, timeuse);
	}

	private static Lock lock = new ReentrantLock();
	private static volatile boolean isInFlushing = false;
	private static ExecutorService flushExecutor = Executors.newSingleThreadExecutor();
	private static final Thread fullDumpThread;

	static {
		LoggerInit.initTddlLog();
		fullDumpThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(dumpInterval * 1000);
					} catch (InterruptedException e) {
					}
					submitFlush(true);
				}
			}
		});
		fullDumpThread.start();
	}

	private static void insureSize() {
		if (keys.size() < maxkeysize) {
			return;
		}
		// logger.info("[insureSize]keys.size()="+keys.size());
		submitFlush(false);
	}

	private static boolean submitFlush(final boolean isFlushAll) {
		if (!isInFlushing && lock.tryLock()) {
			try {
				isInFlushing = true;
				flushExecutor.execute(new Runnable() {
					public void run() {
						try {
							if (isFlushAll) {
								flushAll();
							} else {
								flushLRU();
							}
						} finally {
							isInFlushing = false;
						}
					}
				});
			} finally {
				lock.unlock();
			}
			return true;
		}
		return false;
	}

	/**
	 * ֻ��һ���̻߳�ִ��flushAll��flushLRU����
	 */
	private static void flushAll() {
		Map<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> res = keys;
		keys = new ConcurrentHashMap<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>>(
				maxkeysize);
		try {
			Thread.sleep(5); // �ȴ��Ѿ�����keys������̼߳�����
		} catch (InterruptedException e) {
		}
		logWriter.writeLog(res);
		res = null;
	}

	private static final Comparator<Object[]> countsComparator = new Comparator<Object[]>() {
		public int compare(Object[] keycount1, Object[] keycount2) {
			Long v1 = (Long) keycount1[1];
			Long v2 = (Long) keycount2[1];
			return v1.compareTo(v2);
		}
	};

	/**
	 * ˢ��ִ�д������ٵ�key��ֻ����keysize��2/3
	 */
	private static void flushLRU() {
		List<Object[]> counts = new ArrayList<Object[]>();
		for (Map.Entry<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> e0 : keys.entrySet()) {
			long count = 0;
			for (Map.Entry<Object, ConcurrentHashMap<Object, StatCounter>> e1 : e0.getValue().entrySet()) {
				for (Map.Entry<Object, StatCounter> e2 : e1.getValue().entrySet()) {
					count += e2.getValue().getCount();
				}
			}
			counts.add(new Object[] { e0.getKey(), count });
		}
		statlog.debug("sortedSize=" + counts.size() + ",keys.size=" + keys.size() + linesep);// sortedSize=1135,keys.size=1169
		Collections.sort(counts, countsComparator);
		int i = 0;
		int remain = maxkeysize * 2 / 3; // ����2/3
		int flush = keys.size() - remain; // ��ʱsize�����Ѿ�����
		Map<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>> flushed = new HashMap<Object, ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>>>();
		for (Object[] keycount : counts) {
			Object key = keycount[0];
			ConcurrentHashMap<Object, ConcurrentHashMap<Object, StatCounter>> removed = keys.remove(key);
			if (removed != null) {
				flushed.put(key, removed);
				i++;
			} else {
				statlog.warn("-------------- Should not happen!!! ------------");
			}
			if (i >= flush) {
				if (keys.size() <= remain)
					break;
			}
		}
		logWriter.writeLog(flushed);
		flushed = null;
	}

	public static void main(String[] args) throws InterruptedException {
		LoggerInit.TDDL_Nagios_LOG.fatal("test");
		LoggerInit.TDDL_SQL_LOG.fatal("test");
		LoggerInit.TDDL_MD5_TO_SQL_MAPPING.fatal("test");
		LoggerInit.TDDL_LOG.fatal("test");
		for (int i = 0; i < 5000; i++) {
			AtomBufferedStatLogWriter.add("select 1", "tc", "success", 20L);
			AtomBufferedStatLogWriter.add("select 1", "tc", "faile", 50L);
			AtomBufferedStatLogWriter.add("update 1", "ic", "success", 10L);
			AtomBufferedStatLogWriter.add("insert a" + i, "ic", "success", 11L);
			if (i % 10 == 0) {
				Thread.sleep(1);
			}
		}
		statlog.info("-------------------------------------");
		// BufferedStatLogWriter.flushLRU();
		Thread.sleep(10000);
		statlog.info("end");
	}
}

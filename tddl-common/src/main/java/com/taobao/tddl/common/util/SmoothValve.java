/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.common.ConfigServerHelper;

/**
 * ƽ�����š����ڿ�����״̬�л���
 * ��������õ�����ʱ��˲������������ӵȵı����������
 * 
 * ��һ��ֻ����ִ��1�Σ��ڶ�������ִ��2�Σ����������һ��֮��������(batchLimits)
 * ÿһ���������þ���Ҫ���timeDelay����󣬲ſ�ʼ��һ������ķ��š�
 * ��������ʱ���ڼ������ֱ�Ӿܾ���
 * 
 * ��������limit������Ϊ1/n�ĸ���ȥִ�С���������캯��
 * 
 * @author linxuan
 *
 */
public class SmoothValve {
	private static final Log log = LogFactory.getLog(SmoothValve.class);

	private volatile boolean available = true;
	private volatile boolean isInSmooth = false; //�Ƿ���ƽ����
	private final AtomicInteger count = new AtomicInteger();
	private final AtomicInteger batchNo = new AtomicInteger();
	private final int[] batchLimits;
	private final AtomicInteger rejectCount = new AtomicInteger();
	private final long timeDelay; //ms
	private volatile long timeBegin;

	public SmoothValve(long timeDelay) {
		this.timeDelay = timeDelay;
		this.batchLimits = new int[] { 1, 2, 4, 8, 16, 32, 64 };
	}

	/**
	 * @param timeDelay
	 * @param batchLimits ��������limit��Ϊ�˽��cliet�����൱��ʱ��ÿ��client��һ���������������������ĳ�����
	 *    -1��0������ͬ����ʾ������ǰ���ԣ�ֻ�Ƕ��ӳ���һЩʱ�䡣����һ�㲻��
	 *    -2��ʾ���������У�ֻ�����������client��1/2(�õ�ǰ����1/2�ĸ���ͨ��)
	 *    -3��ʾ���������У�ֻ�����������client��1/3(�õ�ǰ����1/3�ĸ���ͨ��)
	 *    ...
	 * ���磺batchLimits = new int[] { -4,-3,-2, 1, 2, 4, 8, 16, 32 };
	 */
	public SmoothValve(long timeDelay, int[] batchLimits) {
		this.timeDelay = timeDelay;
		this.batchLimits = batchLimits;
	}

	private volatile boolean hasRejectInLastBatch = false;

	/**
	 * @return ����ƽ�����ƺ�Ŀ��ò�������Ϣ
	 * true: ���ã� false�������ã����߿��õ�ûͨ������
	 */
	public boolean smoothThroughOnInitial() {
		if (timeDelay == 0) { //�൱����ȫ�ر��������
			return true;
		}
		while (isInSmooth && available) { //�ѿ��ã�����ƽ����
			int batch = batchNo.get();
			if (batch >= batchLimits.length) {
				if (hasRejectInLastBatch) {
					//�����ʱ�����ϴλ���reject����������һ��batchLimit
					batchNo.set(batchLimits.length - 1);
					hasRejectInLastBatch = false;
					continue;
				}
				isInSmooth = false;
				count.set(0);
				batchNo.set(0);
				rejectCount.set(0);
				return available; //���ؿ���
			}
			hasRejectInLastBatch = false;
			int limit = batchLimits[batch];
			if (limit < -1) { //0��-1���ܣ������ֱ�ӷ���
				int randomInt = new Random().nextInt(-limit);
				if (randomInt == 0) {
					return available; //���ؿ���
				} else {
					logReject(rejectCount, limit);
					return false; //���ز�����
				}
			}
			int current = count.get();
			while (current < limit) {
				if (count.compareAndSet(current, current + 1)) {
					timeBegin = System.currentTimeMillis();
					return available; //���ؿ���
				}
				current = count.get();
			}

			//current >= limit
			if (System.currentTimeMillis() - timeBegin > timeDelay) {
				//timeDelay * (batch + 1) ���������ܻ���Ϊ��ɾۼ�����
				//����ʱ�䣬��������һ������
				batchNo.compareAndSet(batch, batch + 1);
				//����ѭ��
			} else {
				logReject(rejectCount, limit);
				return false; //���ز�����
			}
		}
		return available;
	}

	private void logReject(AtomicInteger rejectCount, int limit) {
		hasRejectInLastBatch = true;
		int rc = rejectCount.incrementAndGet();
		log.warn("A request reject in available switch. limit=" + limit + ",rejectCount=" + rc);
	}

	/**
	 * @return ����ʵ�ʿ��ò����õ���Ϣ
	 */
	public boolean isAvailable() {
		return available;
	}

	public boolean isNotAvailable() {
		return !available;
	}

	/**
	 * ����Ϊ����
	 */
	public void setAvailable() {
		if (available) {
			return;
		}
		count.set(0);
		batchNo.set(0);
		this.isInSmooth = true;
		this.available = true;
	}

	/**
	 * ����Ϊ������
	 */
	public void setNotAvailable() {
		if (available) {
			rejectCount.set(0);
			this.available = false;
		}
	}

	private static enum CreateProperties {
		timeDelay, batchLimits;
	}

	public static SmoothValve parse(String str) {
		Properties p = ConfigServerHelper.parseProperties(str, "[SmoothValve Properties]");
		if (p == null) {
			log.warn("Empty tddlconfig");
			return null;
		}
		try {
			long td = 0;
			String[] limits = null;
			for (Map.Entry<Object, Object> entry : p.entrySet()) {
				String key = ((String) entry.getKey()).trim();
				String value = ((String) entry.getValue()).trim();
				switch (CreateProperties.valueOf(key)) {
				case timeDelay:
					td = Integer.parseInt(value);
					break;
				case batchLimits:
					limits = value.split("\\|");
					break;
				default:
					break;
				}
			}
			if (td == 0) {
				log.error("SmoothValve Properties incomplete");
				return null;
			}
			if (limits != null) {
				int[] limitArray = new int[limits.length];
				for (int i = 0; i < limits.length; i++) {
					limitArray[i] = Integer.parseInt(limits[i].trim());
				}
				return new SmoothValve(td, limitArray);
			} else {
				return new SmoothValve(td);
			}

		} catch (Exception e) {
			log.error("parse SmoothValve Properties failed", e);
			return null;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder("timeDelay=").append(timeDelay).append(",batchLimits=").append(
				Arrays.toString(batchLimits)).toString();
	}

}

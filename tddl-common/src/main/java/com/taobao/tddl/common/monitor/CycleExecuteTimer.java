/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.monitor;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * ִ�ж�ʱ��
 * 
 * @author junyu
 * 
 */
public class CycleExecuteTimer {
	private Log logger = LogFactory.getLog(CycleExecuteTimer.class);
	private ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor();
	private long time;
	private TimeUnit timeUnit;
	private Runnable task;
	private String taskName;
	private volatile boolean isRun;
	private TimeComputer timeComputer;
	private Thread waitThread;

	/**
	 * ����������޲ι��캯��
	 */
	@SuppressWarnings("unused")
	private CycleExecuteTimer() {
	}

	/**
	 *  ��ʼ��һ������
	 *  
	 * @param taskName ��������������־��¼
	 * 
	 * @param task
	 *            ��Ҫִ�е�����
	 * @param time
	 *            ���ʱ��
	 * @param timeUnit
	 *            ���ʱ�䵥λ
	 * @param timeComputer
	 *            ��ʱ�����������������ʱ��ʼ��
	 */
	public CycleExecuteTimer(String taskName, Runnable task, long time,
			TimeUnit timeUnit, TimeComputer timeComputer) {
		this.time = time;
		this.timeUnit = timeUnit;
		this.task = task;
		this.taskName = taskName;
		this.timeComputer = timeComputer;
	}

	public void start() {
		if (isRun) {
			logger.warn(taskName + "�����Ѿ�������");
			return;
		}

		this.isRun = true;

		rotateExecute();
	}

	private void rotateExecute() {
		long interval=-1L;
		if (this.timeComputer != null) {
			Date startTime = this.timeComputer.getMostNearTime();
			interval= this.timeComputer.getMostNearTimeInterval();

			logger.warn(taskName + "������" + startTime + "��ʼ�����뿪ʼʱ�仹�У�"
					+ interval + "����");
		}

		/**
		 * �����̶����ڵ�����
		 */
		executor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (!isRun) {
					logger.warn(taskName + "������ֹͣ��");
					return;
				}

				try {
					task.run();
				} catch (Exception e) {
					logger.error(taskName + "����ִ���쳣��",e);
				}
			}
		}, interval==-1L?0L:interval, this.time, this.timeUnit);
	}

	public void stop() {
		if (this.waitThread != null) {
			waitThread.interrupt();
			waitThread = null;
		}
		this.isRun = false;
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ��ʱ��Ƭ��Ϊ����ۣ�ÿ����һ�����������α갴ʱ��ѭ������ÿ���ۡ��α��ƶ�ʱ�����㲢��ֻ���㵱ǰ�Ĳۣ�
 * ��Ϊ����mod����(cursor = currentTime % timeslice/aSlotTime)���α굽ͷ���Զ��ۻ�������ʵ����һ����
 * 
 *                     cursor
 *                       | 
 * +---------------------+-------------------------+
 * |   |   |   |   |   | C |   |   |   |   |   |   |
 * +-----------------------------------------------+
 * |                                               |
 *  \-----------------timeslice-------------------/
 * 
 * 
 * @author linxuan
 *
 */
public class TimesliceFlowControl {
	private final static int MAX_SLOT = 20; //���20Ƭ
	private final static int MIN_SLOT_TIME = 500; //slotʱ������500����

	private final String name;
	private final AtomicInteger[] slots; //�����飬��С��ʱ����������
	private final int aSlotTimeMillis; //һ���۵�ʱ�䣬��С��ʱ�䵥λ
	private final int timesliceMillis; //�ܵ�ʱ�䴰�ڣ�ʱ��Ƭ����С
	private final int timesliceMaxIns; //ʱ��Ƭ������������ʴ���(�������)

	private final AtomicInteger total = new AtomicInteger(); //�ܵļ���
	private final AtomicInteger totalReject = new AtomicInteger(); //�ܵľܾ�/���޼���
	private volatile int cursor = 0; //�α�
	private volatile long cursorTimeMillis = System.currentTimeMillis(); //��ǰslot�Ŀ�ʼʱ��

	/**
	 * @param name ���ص�����
	 * @param slotTimeMillis //һ���۵�ʱ��
	 * @param slotCount //�۵���Ŀ
	 * @param limit //ʱ�䴰�����������ִ�еĴ�������Ϊ0������
	 */
	public TimesliceFlowControl(String name, int aSlotTimeMillis, int slotCount, int timesliceMaxIns) {
		if (slotCount < 2) {
			throw new IllegalArgumentException("slot����Ҫ������");
		}
		this.name = name;
		this.aSlotTimeMillis = aSlotTimeMillis;
		this.timesliceMillis = aSlotTimeMillis * slotCount;
		this.timesliceMaxIns = timesliceMaxIns;

		slots = new AtomicInteger[slotCount];
		for (int i = 0; i < slotCount; i++) {
			slots[i] = new AtomicInteger(0);
		}
	}

	/**
	 * ��С��ʱ�䵥λȡĬ�ϵ�500����
	 * @param name ���ص�����
	 * @param timesliceMillis ʱ��Ƭ; ��0��ʾʹ��Ĭ��ֵ1����
	 * @param limit ʱ��Ƭ���������ִ�ж��ٴΣ���Ϊ0������
	 */
	public TimesliceFlowControl(String name, int timesliceMillis, int timesliceMaxIns) {
		if (timesliceMillis == 0) {
			timesliceMillis = 60 * 1000; //ʱ��ƬĬ��1����
		}
		if (timesliceMillis < 2 * MIN_SLOT_TIME) {
			throw new IllegalArgumentException("ʱ��Ƭ����" + (2 * MIN_SLOT_TIME));
		}

		//this(name, 500, timesliceMillis / 500, limit);
		int slotCount = MAX_SLOT; //Ĭ�Ϸ�20��slot
		int slotTime = timesliceMillis / slotCount;
		if (slotTime < MIN_SLOT_TIME) {
			slotTime = MIN_SLOT_TIME; //���slotʱ��С��MIN_SLOT_TIME������С����
			slotCount = timesliceMillis / slotTime;
		}

		this.name = name;
		this.aSlotTimeMillis = slotTime;
		//this.timesliceMillis = timesliceMillis; //ֱ�Ӹ�ֵ��Ϊ����Ĺ�ϵ��������Խ��
		this.timesliceMillis = aSlotTimeMillis * slotCount;
		this.timesliceMaxIns = timesliceMaxIns;

		slots = new AtomicInteger[slotCount];
		for (int i = 0; i < slotCount; i++) {
			slots[i] = new AtomicInteger(0);
		}
	}

	public void check() {
		if (!allow()) {
			throw new IllegalStateException(reportExceed());
		}
	}

	public String reportExceed() {
		return name + " exceed the limit " + timesliceMaxIns + " in timeslice " + timesliceMillis;
	}

	public boolean allow() {
		final long current = System.currentTimeMillis();
		final int index = (int) ((current % timesliceMillis) / aSlotTimeMillis);

		if (index != cursor) {
			int oldCursor = cursor;
			cursor = index; //���츳��ֵ
			final long oldCursorTimeMillis = cursorTimeMillis;
			cursorTimeMillis = current; //���츳��ֵ

			//����̻߳���������������ÿ���̼߳����total�������ͬ
			if (current - oldCursorTimeMillis > timesliceMillis) {
				//ʱ������timesliceMillis��������ʱ��Ƭ��Ӧ��������
				for (int i = 0; i < slots.length; i++) {
					slots[i].set(0); //���㣬���Բ�����ɵļ�������
				}
				this.total.set(0);
			} else {
				do {
					//��β��β���㣩��������Ծ�����
					oldCursor++;
					if (oldCursor >= slots.length) {
						oldCursor = 0;
					}
					slots[oldCursor].set(0); //���㣬���Բ�����ɵļ�������
				} while (oldCursor != index);

				//int clearCount = slots[index].get();
				//slots[index].set(0); //���㣬���Բ�����ɵļ�������
				int newtotal = 0;
				for (int i = 0; i < slots.length; i++) {
					newtotal += slots[i].get(); //�������µĵ�ǰ��
				}
				this.total.set(newtotal); //�������������Բ�����ɵļ�������
			}
		} else {
			if (current - cursorTimeMillis > aSlotTimeMillis) {
				//index��ͬ����ʱ������һ��slot��ʱ�䣬˵������ʱ��Ƭ����Ҫ������
				cursorTimeMillis = current; //���츳��ֵ
				for (int i = 0; i < slots.length; i++) {
					slots[i].set(0); //���㣬���Բ�����ɵļ�������
				}
				this.total.set(0);
			}
			//�Ƿ�Ϊ�˱��⿪��������������жϣ�
		}

		if (timesliceMaxIns == 0) {
			return true; //0Ϊ������
		}
		if (this.total.get() < timesliceMaxIns) {
			//�����Ĳż������ܾ��Ĳ�����
			slots[index].incrementAndGet();
			total.incrementAndGet();
			return true;
		} else {
			totalReject.incrementAndGet();
			return false;
		}
	}

	/**
	 * @return ��ǰʱ��Ƭ�ڵ���ִ�д���
	 */
	public int getCurrentCount() {
		return total.get();
	}

	/**
	 * @return ������ʷ����(���󴴽�����)���ܾ�/���޵Ĵ���
	 */
	public int getTotalRejectCount() {
		return totalReject.get();
	}
}

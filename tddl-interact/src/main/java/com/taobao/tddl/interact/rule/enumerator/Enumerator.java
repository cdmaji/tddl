/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.enumerator;

import java.util.Set;

/**
 * ö�������ṩ�˸���ÿ�������� ������ȡ����ö��ֵ�Ĳ���
 * ��Ҫ�����ڽ��һ��������������������
 * sql ���� :id>100 and id < 200;
 * �����������޷�ֱ�Ӵ�����������н��м���Ȼ��򵥵�ȡ����������ģ�������μ�����ĵ��Ľ��ܡ�
 * 
 * ���Խ���ķ������ǰ�100~200֮�������ֵ������atomicIncreatementValue���趨ֵ����ö�١�
 * ö�ٳ���ֵ������set�󷵻ظ������ߡ�
 * 
 * @author shenxun
 *
 */
public interface Enumerator {
	/**
	 * @param condition ����
	 * @param cumulativeTimes ֵ�ĸ��������ڲ��������ĺ�����˵�������һ���ۼӵĴ��������޵ģ�����Ҫ�������������
	 * @param atomIncrValue ����ֵ������С�䶯�Ķ�����ԭ������ֵ��ex:�������dayofweek�����ĺ�����˵������ֵ��
	 * �����仯�Ķ��������С�䶯��ΧΪ1�졣
	 * @param needMergeValueInCloseInterval �Ƿ���Ҫ��> < >= <= ���м��㡣
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Set<Object> getEnumeratedValue(Comparable condition,Integer cumulativeTimes,Comparable<?> atomicIncreatementValue
			,boolean needMergeValueInCloseInterval);
//	 void setNeedMergeValueInCloseInterval(boolean needMergeValueInCloseInterval);
}

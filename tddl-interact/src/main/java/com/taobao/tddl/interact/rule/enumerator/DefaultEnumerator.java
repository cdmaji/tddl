/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.enumerator;

import java.util.Set;

import com.taobao.tddl.interact.sqljep.Comparative;
/**
 * ������ܽ���ö�٣���ô������Ĭ�ϵ�ö����
 * Ĭ��ö����ֻ֧��comparativeOr�������Լ����ڵĹ�ϵ����֧�ִ���С�ڵ�һϵ�й�ϵ��
 * 
 * @author shenxun
 *
 */
public class DefaultEnumerator implements CloseIntervalFieldsEnumeratorHandler{

	public void mergeFeildOfDefinitionInCloseInterval(Comparative from,
			Comparative to, Set<Object> retValue, Integer cumulativeTimes,
			Comparable<?> atomIncrValue) {
		throw new IllegalArgumentException("Ĭ��ö������֧�����");
		
	}
	public void processAllPassableFields(Comparative source,Set<Object> retValue,
			Integer cumulativeTimes, Comparable<?> atomIncrValue) {
		throw new IllegalStateException("��û���ṩ�����͵��Ӵ�����ǰ���£����ܹ����ݵ�ǰ��Χ����ѡ��" +
				"��Ӧ�Ķ������ö��ֵ��sql�в�Ҫ����> < >= <=");
	}
}

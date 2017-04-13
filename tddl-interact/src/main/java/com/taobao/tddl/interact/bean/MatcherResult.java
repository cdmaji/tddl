/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.bean;

import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * ƥ��Ľ�����󣬹�����Controller���з��ض����ƴװ
 * 
 * 
 * ��Щ�Ǵ���Ĵ�ƥ���п��Ի�õ����� ��Ҫ��Ӧ������Щ����Щ���Ƿ���������ֿ�ֱ����
 * 
 * @author shenxun
 *
 */
public interface MatcherResult {
	/**
	 * ��������Ľ������
	 * @return
	 */
	List<TargetDB> getCalculationResult();
	
	/**
	 * ƥ��Ŀ������ʲô,�������Nullֵ
	 * @return
	 */
	Map<String, Comparative> getDatabaseComparativeMap();
	
	/**
	 * ƥ��ı������ʲô,�������nullֵ
	 * @return
	 */
	Map<String,Comparative> getTableComparativeMap();
}

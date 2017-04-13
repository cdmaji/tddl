/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.bean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * �����ɸѡ��
 * 
 * @author shenxun
 *
 */
public interface ComparativeMapChoicer {

	/**
	 * ����PartinationSet ��ȡ����������Ӧֵ��map.
	 * @param arguments
	 * @param partnationSet
	 * @return
	 */
	Map<String, Comparative> getColumnsMap(List<Object> arguments, Set<String> partnationSet);
	
	Comparative getColumnComparative(List<Object> arguments, String colName);
}

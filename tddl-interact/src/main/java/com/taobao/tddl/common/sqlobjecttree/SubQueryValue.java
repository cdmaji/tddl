/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sqlobjecttree;

import java.util.Map;

/**
 * Ϊ�˽������һ�����⣺
 * �������治��֧��һ��=��comparative�а������Or�Ĺ���ƥ�䡣
 * ������Щ�������Ƕ��ֱ�����ֵ��subSelect��̳�����ӿڣ���
 * Comparative����һ��hook��ר�Ŵ�������֮�µ�����
 * @author shenxun
 *
 */
public interface SubQueryValue extends Value {
	public void setAliasMap(Map<String, SQLFragment>  map);
	
}

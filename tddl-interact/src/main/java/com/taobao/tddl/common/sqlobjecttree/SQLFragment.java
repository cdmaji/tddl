/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.sqlobjecttree;

import java.util.List;
import java.util.Set;

public interface SQLFragment extends Cloneable{
	
	 public void appendSQL(StringBuilder sb);
	 /**
	  * ��һ��sql�в����StringToken���浽�ڶ��������Ǹ�list�У�token֮���п��ܻ���һЩ�ɱ��
	  * ����������limit m,n�е�m,n.���б�����
	 * @param logicTableNames
	 * @param list
	 * @param sb
	 * @return
	 */
	public StringBuilder regTableModifiable(Set<String> logicTableNames,List<Object> list,StringBuilder sb);
}

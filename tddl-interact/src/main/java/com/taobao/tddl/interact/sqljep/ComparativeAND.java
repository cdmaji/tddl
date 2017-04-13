/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.sqljep;

import java.util.Comparator;

/**
 * AND�ڵ�
 * ��ʵ�ʵ�SQL�У�ʵ����������
 * [Comparative]              [comparative]
 * 			\                  /
 * 			  \				  /
 *             [ComparativeAnd]
 *             
 * ���������Ľڵ����
 * 
 * @author shenxun
 *
 */
public class ComparativeAND extends ComparativeBaseList{
	
	public ComparativeAND(int function, Comparable<?> value) {
		super(function, value);
	}
	
	public ComparativeAND(){
	}
	
	public ComparativeAND(Comparative item){
		super(item);
	}
	
//	/* (non-Javadoc)
//	 * @see com.taobao.tddl.common.sqljep.function.ComparativeBaseList#intersect(int, java.lang.Comparable, java.util.Comparator)
//	 * �¹��������Ѿ���������
//	 */
//	@SuppressWarnings("unchecked")
//	public boolean intersect(int function,Comparable other,Comparator comparator){
//		for(Comparative source :list){
//			if(!source.intersect(function, other, comparator)){
//				return false;
//			}
//		}
//		return true;
//	}

	@Override
	protected String getRelation() {
		return " and ";
	}

}

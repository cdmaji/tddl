/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * �����ѿ������Ժ��һ��ֵ����Ϊ�ж��������ÿ��������������з�Χ������£�
 * Ҫ�����������ֻ�н��еѿ�������ö�ٳ����п��ܵ�ֵ���������㡣
 * �������ö��ֵ�е�һ����
 * columns�ǹ�����С�����samplingField���Ƽ��Σ����Ṳ��ͬһ��������
 * ��enumFields���ʾ����������˳��ͨ���ѿ���������ʽö�ٳ���һ��ֵ��
 * 
 * @author shenxun
 *
 */
public class SamplingField{
	/**
	 * ��ʾ����������˳��ͨ���ѿ���������ʽö�ٳ���һ��ֵ
	 */
	final List<Object> enumFields ;
	
	private String mappingTargetKey;

	private Object mappingValue;
	
	/**
	 * һ������
	 */
	private final  List<String> columns ;
	
	final int capacity ;
	
	public SamplingField(List<String> columns,int capacity) {
		this.enumFields = new ArrayList<Object>(capacity);
		this.capacity = capacity;
		this.columns =Collections.unmodifiableList(columns);
	}
	
	public void add(int index,Object value){
		enumFields.add(index,value);
	}
	
	public List<String> getColumns() {
		return columns;
	}

	public List<Object> getEnumFields() {
		return enumFields;
	}

	//final���͵�enumFields,������setter,�ҳ�ʼ��ʱ�϶�ʵ����,���Կ϶���Ϊnull
	public void clear() {
		enumFields.clear();
	}


	public String getMappingTargetKey() {
		return mappingTargetKey;
	}
	public void setMappingTargetKey(String mappingTargetKey) {
		this.mappingTargetKey = mappingTargetKey;
	}
	@Override
	public String toString() {
		return "columns:"+columns+"enumedFileds:"+enumFields;
	}
	public Object getMappingValue() {
		return mappingValue;
	}
	public void setMappingValue(Object mappingValue) {
		this.mappingValue = mappingValue;
	}
	
}


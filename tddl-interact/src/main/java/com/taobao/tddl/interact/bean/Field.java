/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * �������->sourceKey��ӳ�䡣
 * 
 * �����һ��Set���������ⳡ�������㡣
 * 
 * ���set����Ҫ���þ��Ǵ��sourceKey��ͬʱ��Ҳ���ӳ���Ľ��������������mapping rule�в�tair�Ժ�����ģ�Ϊ�˼���һ�β�
 * 
 * tair�Ĺ��̣����Ҫ��¼�²�tair�Ժ��ֵ������Щ�����Ұ��ս�����з��ࡣ
 * 
 * ��Ϊӳ�����ֻ��������Ψһ����������в������㡣
 * 
 * ���������ҽ���һ��������¡�set�е�targetValueӦ�þ���sourceKeyͨ��tairӳ���Ժ�Ľ����
 * 
 * �����������,mappingKeysӦ����ԶΪ�ա�
 * 
 * ����д�������ǲ���Ⱦ����sourceKeys�������߼��Ķ�
 * 
 * @author shenxun
 * 
 */
public class Field
{
	public Field(int capacity)
	{
		sourceKeys = new HashMap<String, Set<Object>>(capacity);
	}

	public Map<String/* ���� */, Set<Object>/* �õ��ý�������ֵ�� */> sourceKeys;

	public static final Field EMPTY_FIELD = new Field(0);

	/**
	 * ����ӳ������д��ӳ��������ֵ����Щֵ��Ӧ������ͬ����������ӦmappingTargetColumn
	 */
	public Set<Object> mappingKeys;
	 /**
	 * ��Ӧ����mappingKeys��targetColumn
	 */
	public String mappingTargetColumn;
	
	
	public boolean equals(Object obj, Map<String, String> alias)
	{
		//���ڱȽ�����field�Ƿ���ȡ�field��������У���ô�����ڵ�ÿһ��ֵ��Ӧ�����ҵ���Ӧ��ֵ������ȡ�
		if (!(obj instanceof Field))
		{
			return false;
		}
		Map<String, Set<Object>> target = ((Field) obj).sourceKeys;
		for (Entry<String, Set<Object>> entry : sourceKeys.entrySet())
		{
			String srcKey = entry.getKey();
			if (alias.containsKey(srcKey))
			{
				srcKey = alias.get(srcKey);
			}
			Set<Object> targetValueSet = target.get(srcKey);
			Set<Object> sourceValueSet = entry.getValue();
			for (Object srcValue : sourceValueSet)
			{
				boolean eq = false;
				for (Object tarValue : targetValueSet)
				{
					if(tarValue.equals(srcValue)){
						eq = true;
					}
				}
				if(!eq)
				{
					return false;
				}
			}
		}
		return true;
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	//Copyright(c) Taobao.com
package com.taobao.tddl.interact.rule.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.taobao.tddl.interact.rule.enumerator.CloseIntervalFieldsEnumeratorHandler;
import com.taobao.tddl.interact.rule.enumerator.IntegerPartDiscontinousRangeEnumerator;
import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * @description
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-6-2����04:13:14
 */
public class VirturalNodeUtil {
	private static final String SLOT_PIECE_SPLIT = ",";
	private static final String RANGE_SUFFIX_SPLIT = "-";
	private static CloseIntervalFieldsEnumeratorHandler enumerator = new IntegerPartDiscontinousRangeEnumerator();

	/**
	 * ����oriMap��value��ʽΪ <b>0,1,2-6</b> 0,1��ʾ2����,'-'��ʾһ����Χ
	 * 
	 * �˺����н���Χö�ٳ�һ������,�����۱�Ϊkey,ԭ����key��Ϊvalue
	 * 
	 * example 1:keyΪ 1 valueΪ 1,2,3-6
	 * 
	 * ���ؽ��Ϊ 1->1,2->1,3->1,4->1,5->1,6->1
	 * 
	 * example 2:keyΪdb_group_1 valueΪ1,2 db_group_2 valueΪ3,4-6
	 * ���ؽ��Ϊ 1->db_group_1,2->db_group_1
	 * 3->db_group_2,4->db_group_2,5->db_group_3,2->db_group_2
	 * 
	 * <b>
	 * ��ʱ��֧���κ���ʽ��value��ʽ��.��_0000,0001֮����ַ���,ֻ����
	 * ��ѧ��ʽ�ϵ�integer,long
	 * 
	 * �����Ľ�
	 * </b>
	 * @param tableMap
	 * @return
	 */
	public static Map<String,String> extraReverseMap(Map<String,String> oriMap){
    	ConcurrentHashMap<String,String> slotMap=new ConcurrentHashMap<String, String>();
        for(Map.Entry<String,String> entry:oriMap.entrySet()){
            String[] pieces=entry.getValue().trim().split(SLOT_PIECE_SPLIT);
            for(String piece:pieces){
            	String[] range=piece.trim().split(RANGE_SUFFIX_SPLIT);
            	if(range.length==2){
            		Comparative start=new Comparative(Comparative.GreaterThanOrEqual,Integer.valueOf(range[0]));
            		Comparative end=new Comparative(Comparative.LessThanOrEqual,Integer.valueOf(range[1]));
            		int cumulativeTimes=Integer.valueOf(range[1])-Integer.valueOf(range[0]);
            		Set<Object> result=new HashSet<Object>();
            		enumerator.mergeFeildOfDefinitionInCloseInterval(start,end,result,cumulativeTimes,1);
            		for(Object v:result){
            			slotMap.put(String.valueOf(v),entry.getKey());
            		}
            	}else if(range.length==1){
            		slotMap.put(piece, entry.getKey());
            	}else{
            		throw new IllegalArgumentException("slot config error,slot piece:"+piece);
            	}
            }
        }
    	return slotMap;
    }
	
	public static void main(String[] args){
		Map<String,String> oriMap=new HashMap<String, String>();
		oriMap.put("1", "1,2,3-6");
		Map<String,String> re=VirturalNodeUtil.extraReverseMap(oriMap);
		for(Map.Entry<String,String> entry:re.entrySet()){
			StringBuilder sb=new StringBuilder(entry.getKey());
			sb.append("->");
			sb.append(entry.getValue());
			System.out.println(sb.toString());
		}
		System.out.println("-----------------------------------------------");
		oriMap.clear();
		oriMap.put("db_group_1", "1,2");
		oriMap.put("db_group_2", "3,4-6");
		Map<String,String> re2=VirturalNodeUtil.extraReverseMap(oriMap);
		for(Map.Entry<String,String> entry:re2.entrySet()){
			StringBuilder sb=new StringBuilder(entry.getKey());
			sb.append("->");
			sb.append(entry.getValue());
			System.out.println(sb.toString());
		}
	}
}

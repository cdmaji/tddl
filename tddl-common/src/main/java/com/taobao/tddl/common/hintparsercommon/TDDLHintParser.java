/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.hintparsercommon;

import java.util.HashMap;import java.util.Map;import com.taobao.tddl.common.util.TStringUtil;
/**
 * hint�������Ļ���ʵ�֡� Ϊ�˽�hint������ע�ͼ������֣��������һ�������ǡ�+��������:(����)
 * hint�������������sql����ǰ�棬���������sql��ǰ����֣�����Ϊtddl hint
 * 
 * @author whisper
 * 
 */
/* + hint1=value;hint2=value;hint3=value */
public class TDDLHintParser {
	/**
	 * ��һ��tddlע�ͽ���Ϊһ��string map. ���������������:����Ϊע�͵����⣬ֱ��д�ڷ����������������)
	 * 
	 * @param sql
	 * @return
	 */
	/* + hint1:value;hint2:value;hint3:value */
	public static Map<String, String> parseHint(String sqlHint) {
		//����ע���е�/*+��*/
		sqlHint = TStringUtil.substringBetween(sqlHint, "/*+", "*/");
		//����ʹ��StringUtil�����ĸ������������ sql = xx ;; b = uu ;���м�������ָ�;;�ᱻʶ��Ϊһ���������Խ���
		String[] hints = TStringUtil.splitm(sqlHint,";");
		Map<String, String> hintMap = new HashMap<String, String>(hints.length);
		for(String hint : hints){
			if(hint == null){
				throw new IllegalArgumentException("hint is null");
			}else{
				String[] pair = TStringUtil.splitm(hint,":");
				if(pair.length != 2){
					throw new IllegalArgumentException("�����������󣬼�ֵ�Բ�Ϊ2;"+hint);
				}
				String key = pair[0];
				key = key.trim();
				String value = pair[1];
				value = value.trim();
				hintMap.put(key, value);
			}
		}
		return hintMap;
	}
	/**
	 * �����ж��Ƿ����TDDL hint
	 * @param sql
	 * @return
	 */
	public static boolean containTDDLHint(String sql){
		if(sql == null){
			return false;
		}
		return sql.trim().startsWith("/*+");
	}
	
	 /**
     * ȡ�������ָ���֮����Ӵ���
     * 
     * <p>
     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>���򷵻�<code>null</code>��
     * <pre>
     * StringUtil.substringBetween(null, *, *)          = null
     * StringUtil.substringBetween("", "", "")          = ""
     * StringUtil.substringBetween("", "", "tag")       = null
     * StringUtil.substringBetween("", "tag", "tag")    = null
     * StringUtil.substringBetween("yabcz", null, null) = null
     * StringUtil.substringBetween("yabcz", "", "")     = ""
     * StringUtil.substringBetween("yabcz", "y", "z")   = "abc"
     * StringUtil.substringBetween("yabczyabcz", "y", "z")   = "abc"
     * </pre>
     * </p>
     *
     * @param str �ַ���
     * @param open Ҫ�����ķָ��Ӵ�1
     * @param close Ҫ�����ķָ��Ӵ�2
     *
     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>��δ�ҵ��ָ��Ӵ����򷵻�<code>null</code>
     */
	 static String substringBetween(String str, String open, String close) {
		return substringBetween(str, open, close, 0);
	}
	/**
	 * ��sql�е�hintɾ������
	 * 
	 * @param sql
	 * @return
	 */
	public static String removeHint(String sql){
		if(containTDDLHint(sql)){
			int index = sql.indexOf("*/");
			//����*/�������ַ�
			return sql.substring(index+2);
		}
		return sql;
	}
	 public static void main(String[] args) {
		long l = System.currentTimeMillis();
		String s = "/*+ db:{db1} */sql";
		for(int i = 0 ; i < 50000 ; i ++){
			removeHint(s);
		}
		System.out.println(System.currentTimeMillis() - l);
//		System.out.println(removeHint(s));
	}
	/**
     * ȡ�������ָ���֮����Ӵ���
     * 
     * <p>
     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>���򷵻�<code>null</code>��
     * <pre>
     * StringUtil.substringBetween(null, *, *)          = null
     * StringUtil.substringBetween("", "", "")          = ""
     * StringUtil.substringBetween("", "", "tag")       = null
     * StringUtil.substringBetween("", "tag", "tag")    = null
     * StringUtil.substringBetween("yabcz", null, null) = throw IllegalArgumentException
     * StringUtil.substringBetween("yabcz", "", "")     = ""
     * StringUtil.substringBetween("yabcz", "y", "z")   = "abc"
     * StringUtil.substringBetween("yabczyabcz", "y", "z")   = "abc"
     * </pre>
     * </p>
     *
     * @param str �ַ���
     * @param open Ҫ�����ķָ��Ӵ�1
     * @param close Ҫ�����ķָ��Ӵ�2
     * @param fromIndex ��ָ��index������
     *
     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>��δ�ҵ��ָ��Ӵ����򷵻�<code>null</code>
     * @throws IllegalArgumentException ���û���ҵ�open��ǩ����close��ǩ
     */
	private static String substringBetween(String str, String open,
			String close, int fromIndex) {
		if ((str == null) || (open == null) || (close == null)) {
			return null;
		}
		str = str.trim();
		int start = str.indexOf(open, fromIndex);

		if (start != -1) {
			int end = str.indexOf(close, start + open.length());

			if (end != -1) {
				return str.substring(start + open.length(), end);
			}else{
				throw new IllegalArgumentException("can't find end :"+close);
			}
		}else{
			throw new IllegalArgumentException("can't find start :"+open);
		}
	}

}

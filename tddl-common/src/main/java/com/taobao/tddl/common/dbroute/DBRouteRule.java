/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	//package com.taobao.tddl.common.dbroute;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;
//
//import org.apache.commons.logging.LogFactory;
//
//public class DBRouteRule {
//	private String dbName;
//	private String ruleBaseName;
//	private String rule;
//	private Pattern pattern;
//
//	/**
//	 * ������
//	 */
//	private int dividend = -100;
//	/**
//	 * Ŀ������
//	 */
//	private int remainder = -100;
//
//	/**
//	 * ��ʼ��
//	 */
//	public DBRouteRule(String dbName, String ruleBaseName, String rule) {
//		this.dbName = dbName;
//		this.ruleBaseName = ruleBaseName;
//		this.rule = rule;
//		init();
//	}
//
//	/**
//	 * ����һ������
//	 * 
//	 * @param ruleBaseName
//	 *            ���ݿ�ڵ������
//	 * @param rule
//	 *            ����,һ��������ʽ
//	 */
//	public void addRule(String ruleBaseName, String rule) {
//		try {
//			this.ruleBaseName = ruleBaseName;
//			this.rule = rule;
//			pattern = Pattern.compile(rule);
//		} catch (PatternSyntaxException e) {
//			LogFactory.getLog("Persistence").debug(
//					"Persistence routing rule " + rule
//							+ " is inavailble to compile." + e, e);
//		}
//	}
//
//	/**
//	 * �Ƿ���ָ����������ʽƥ��
//	 * 
//	 * @param ruleBaseName
//	 * 
//	 * @return
//	 */
//	@Deprecated
//	public boolean isMatched(String ruleBaseName) {
//		if ((ruleBaseName == null) || (ruleBaseName.length() <= 0)) {
//			return false;
//		}
//
//		CharSequence cs = ruleBaseName
//				.subSequence(0, ruleBaseName.length() - 1);
//		Matcher m = pattern.matcher(cs);
//
//		return m.matches();
//	}
//
//	/**
//	 * �Ƿ���ָ����������ʽƥ��
//	 * 
//	 * @param ruleBaseName
//	 * 
//	 * @return
//	 */
//	public boolean isMatched(long ruleEle) {
//		if (ruleEle == DBRoute.DEFAULT_USER_ID_LONG) {
//			return false;
//		}
//		if(remainder!=-100&&dividend!=-100){
//			if(ruleEle>=dividend){
//				return ruleEle%dividend==remainder;
//			}
//			else{
//				return ruleEle==remainder;
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * �������ݿ�ڵ������
//	 * 
//	 * @return
//	 */
//	public String getdbName() {
//		return dbName;
//	}
//
//	/**
//	 * @return Returns the pattern.
//	 */
//	public Pattern getPattern() {
//		return pattern;
//	}
//
//	/**
//	 * ���ظù���Ĺ�������
//	 * 
//	 * @return Returns
//	 */
//	public String getRuleBaseName() {
//		return ruleBaseName;
//	}
//
//	/**
//	 * ���ظù���
//	 * 
//	 * @return Returns
//	 */
//	public String getRule() {
//		return rule;
//	}
//
//	private void init() {
//		// modifid By ��ѯ
//		// ������д�Ժ����ع�����Ӧ��������
//		boolean inited = false;
//
//		if (rule.contains(",")) {
//			inited = true;
//			String[] strs = rule.split(",");
//			dividend = Integer.valueOf(strs[0]).intValue();
//			remainder=Integer.valueOf(strs[1]).intValue();
//		}
//		if (!inited) {
//			try {
//				pattern = Pattern.compile(rule);
//			} catch (PatternSyntaxException e) {
//				LogFactory.getLog("DBRouteRule").debug(
//						"DBRouteRule routing rule " + rule
//								+ " is inavailble to compile." + e, e);
//			}
//		}
//	}
//}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.rule.bean.AdvancedParameter;
import com.taobao.tddl.interact.rule.enumerator.Enumerator;
import com.taobao.tddl.interact.rule.enumerator.EnumeratorImp;
import com.taobao.tddl.interact.sqljep.Comparative;

public class RuleUtils {
	
	private static final Enumerator enumerator = new EnumeratorImp();
	
	public static Map<String, Set<Object>/* ����������key��ֵ��pair */> getSamplingField(Map<String, Comparative> argumentsMap,
			Set<AdvancedParameter> param) {
		// TODO:��ϸע��,����ѿ�����
		// ö���Ժ��columns�����ǵ����֮��Ķ�Ӧ��ϵ
		Map<String, Set<Object>> enumeratedMap = new HashMap<String, Set<Object>>(
				param.size());
		for (AdvancedParameter entry : param) {
			String key = entry.key;
			// ��ǰenumerator��ָ����ǰ�����Ƿ���Ҫ���������⡣
			// enumerator.setNeedMergeValueInCloseInterval();

			try {
				Set<Object> samplingField = enumerator.getEnumeratedValue(
						argumentsMap.get(key),
						entry.cumulativeTimes,
						entry.atomicIncreateValue,
						entry.needMergeValueInCloseInterval);
				enumeratedMap.put(key, samplingField);
			} catch (UnsupportedOperationException e) {
				throw new UnsupportedOperationException("��ǰ�зֿ�ֱ���ִ��󣬳��ִ����������:"
						+ entry.key, e);
			}

		}
		return enumeratedMap;
	}
//	public static String placeHolder(int bit, int table) {
//        if(bit < String.valueOf(table).length()){
//        	//��width < ��ֵ�����λ��ʱ��Ӧ��ֱ�ӷ�����ֵ
//        	return String.valueOf(table);
//        }
//		int max = (int) Math.pow(10, (bit));
//		int placedNumber = max + table;
//		String xxxfixWithPlaceHoder = String.valueOf(placedNumber).substring(1);
//		return xxxfixWithPlaceHoder;
//	}

//	public static Map<String, SharedElement> getSharedElemenetMapBySharedElementList(
//			List<? extends SharedElement> sharedElementList) {
//		Map<String, SharedElement> returnMap = new HashMap<String, SharedElement>();
//		if (sharedElementList != null) {
//			int index = 0;
//			for (SharedElement sharedElement : sharedElementList) {
//				returnMap.put(String.valueOf(index), sharedElement);
//				index++;
//			}
//		}
//		return returnMap;
//	}

//	private static final Pattern DOLLER_PATTERN = Pattern.compile("#.*?#");

	/**
	 * #para#
	 * #para,(autoinc),(range)#
	 * 
	 * @param rules
	 * @param ruleClass
	 * @param isDatabase
	 * @return
	 */
//	public static RuleChainImp getRuleChainByRuleStringList(List<Object> rules,
//			Class<? extends ListAbstractResultRule> ruleClass,
//			boolean isDatabase) {
//		if(rules == null || rules.isEmpty()){
//			return null;
//		}
//		List<ListAbstractResultRule> list = new ArrayList<ListAbstractResultRule>();
//		RuleChainImp ruleChainImp = new RuleChainImp();
//		for (Object ruleString : rules) {
//			if(ruleString instanceof String){
//				ListAbstractResultRule listRule = getRuleInstance(ruleClass);
//	//
//	//			Matcher matcher = DOLLER_PATTERN.matcher(ruleString);
//	//			Set<AdvancedParameter> params = new HashSet<AdvancedParameter>();
//	//			StringBuffer sb = new StringBuffer();
//	//			while (matcher.find()) {
//	//				String realParam = matcher.group();
//	//				AdvancedParameter advancedParameter = getAdvancedParamByParamToken(realParam);
//	//				params.add(advancedParameter);
//	//				sb.append("(map.get(\"");
//	//				matcher.appendReplacement(sb, advancedParameter.key);
//	//				sb.append("\"))");
//	//			}
//	//			matcher.appendTail(sb);
//	//			listRule.setExpression(sb.toString());
//	//			listRule.setAdvancedParameter(params);
//				//�����ŵĹ���ֱ��set
//				listRule.setExpression((String)ruleString);
//				//���������� 
//				
//				list.add(listRule);
//			}else if (ruleString instanceof ListAbstractResultRule){
//				list.add((ListAbstractResultRule)ruleString);
//			}else{
//				throw new IllegalArgumentException("not support rule type : "+ruleString.getClass());
//			}
//		}
//		ruleChainImp.setListResultRule(list);
//		ruleChainImp.setDatabaseRuleChain(isDatabase);
//		ruleChainImp.init();
//		return ruleChainImp;
//	}
////
////	protected static AdvancedParameter getAdvancedParamByParamToken(
////			String paramToken) {
////		AdvancedParameter param = new AdvancedParameter();
////		paramToken = paramToken.substring(1, paramToken.length() - 1);
////		String[] paramTokens = paramToken.split(",");
////		
////		int tokenLength = paramTokens.length;
////		switch (tokenLength) {
////		case 1:
////			param.key = paramTokens[0];
////			break;
////		case 3:
////			param.key = paramTokens[0];
////			try {
////				param.atomicIncreateValue = Integer.valueOf(paramTokens[1]);
////				param.cumulativeTimes = Integer.valueOf(paramTokens[2]);
////			} catch (NumberFormatException e) {
////				throw new IllegalArgumentException("����Ĳ�����ΪInteger����,����Ϊ:"
////						+ paramToken, e);
////			}
////			break;
////		default:
////			throw new IllegalArgumentException("����Ĳ�������������Ϊ1������3����3����ʱ��Ϊ����ʹ��"
////					+ "ö��ʱ������");
////		}
////		return param;
////	}
//
//	private static ListAbstractResultRule getRuleInstance(
//			Class<? extends ListAbstractResultRule> ruleClass) {
//		try {
//			return ruleClass.newInstance();
//		} catch (Exception e) {
//			throw new IllegalArgumentException(e);
//		}
//	}
}

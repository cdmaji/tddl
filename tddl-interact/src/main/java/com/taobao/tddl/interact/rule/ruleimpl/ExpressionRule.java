/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.ruleimpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.tddl.interact.rule.Rule;

/**
 * ͨ�����ʽ��������Ļ���
 * 
 * @author linxuan
 *
 * @param <T>
 */
public abstract class ExpressionRule<T> implements Rule<T> {

	private static final Pattern DOLLER_PATTERN;
	static {
		//�ṩһ�������õĻ��ᣬ�����ھ������Ĭ�ϵ�����£�����Ӱ��ӿڲ�κʹ���ṹ
		String regex = System.getProperty("com.taobao.tddl.rule.columnParamRegex", "#.*?#");
		DOLLER_PATTERN = Pattern.compile(regex);
	}

	/**
	 * ��ǰ������Ҫ�õ��Ĳ���
	 */
	protected final Map<String/*Сд����*/, RuleColumn> parameters;
	protected final Set<RuleColumn> parameterSet;

	/**
	 * ��ǰ������Ҫ�õ��ı��ʽ
	 */
	protected String expression;
	protected final String originExpression; //ԭʼ�ı��ʽ

	//private boolean useThreadLocalContext;
	//protected ThreadLocal<Map<Object, Object>> context;

	public ExpressionRule(String expression) {
		this.originExpression = expression;
		this.expression = expression;
		this.parameters = Collections.unmodifiableMap(parse());
		this.parameterSet = new HashSet<RuleColumn>(parameters.size());
		this.parameterSet.addAll(parameters.values());
	}

	private Map<String, RuleColumn> parse() {
		Map<String, RuleColumn> parameters = new HashMap<String, RuleColumn>();
		Matcher matcher = DOLLER_PATTERN.matcher(expression);
		int start = 0;
		StringBuffer sb = new StringBuffer();
		while (matcher.find(start)) {
			String realParam = matcher.group();
			realParam = realParam.substring(1, realParam.length() - 1);
			sb.append(expression.substring(start, matcher.start()));
			sb.append(parseParam(realParam, parameters));
			start = matcher.end();
		}
		sb.append(expression.substring(start));
		expression = sb.toString();
		return parameters;
	}

	/**
	 * ���ཫparamInDoller����ΪRuleColumn�����뵽parameters�У��������滻����ִ�
	 * @param paramInDoller
	 * @param parameters
	 * @return �滻����ִ�
	 */
	abstract protected String parseParam(String paramInDoller, Map<String, RuleColumn> parameters);

	public Map<String, RuleColumn> getRuleColumns() {
		return parameters;
	}

	public Set<RuleColumn> getRuleColumnSet() {
		return parameterSet;
	}

	/**
	 * originExpression��ͬ����ͬ��eclipse����
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionRule other = (ExpressionRule) obj;
		if (originExpression == null) {
			if (other.originExpression != null)
				return false;
		} else if (!originExpression.equals(other.originExpression))
			return false;
		return true;
	}

	/**
	 * eclipse����
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((originExpression == null) ? 0 : originExpression.hashCode());
		return result;
	}
}

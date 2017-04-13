/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.ruleimpl;

import groovy.lang.GroovyClassLoader;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;

public class GroovyRule<T> extends EnumerativeRule<T> {
	private static final Log logger = LogFactory.getLog(GroovyRule.class);
	// Ӧ������������ģ���������evel��groovy�ű���
	private static final String IMPORT_EXTRA_PARAMETER_CONTEXT = "import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;";
	private static final String IMPORT_STATIC_METHOD = "import static com.taobao.tddl.interact.rule.groovy.GroovyStaticMethod.*;";
	private static final Pattern RETURN_WHOLE_WORD_PATTERN = Pattern.compile("\\breturn\\b", Pattern.CASE_INSENSITIVE);// ȫ��ƥ��

	private Object ruleObj;
	private Method ruleMethod;
	private String extraPackagesStr;

	public GroovyRule(String expression) {
		this(expression,null);
	}
	
	public GroovyRule(String expression,String extraPackagesStr) {
		super(expression);
		if(extraPackagesStr==null){
			this.extraPackagesStr="";
		}else{
			this.extraPackagesStr=extraPackagesStr;
		}
		initGroovy();
	}

	private void initGroovy() {
		if (expression == null) {
			throw new IllegalArgumentException("δָ�� expression");
		}
		GroovyClassLoader loader = new GroovyClassLoader(GroovyRule.class.getClassLoader());
		String groovyRule = getGroovyRule(expression,extraPackagesStr);
		Class<?> c_groovy;
		try {
			c_groovy = loader.parseClass(groovyRule);
		} catch (CompilationFailedException e) {
			throw new IllegalArgumentException(groovyRule, e);
		}

		try {
			// �½���ʵ��
			ruleObj = c_groovy.newInstance();
			// ��ȡ����
			ruleMethod = getMethod(c_groovy, "eval", Map.class, Object.class);
			if (ruleMethod == null) {
				throw new IllegalArgumentException("���򷽷�û�ҵ�");
			}
			ruleMethod.setAccessible(true);
		} catch (Throwable t) {
			throw new IllegalArgumentException("ʵ�����������ʧ��", t);
		}
	}

	protected static String getGroovyRule(String expression,String extraPackagesStr) {
		StringBuffer sb = new StringBuffer();
		sb.append(extraPackagesStr);
		sb.append(IMPORT_STATIC_METHOD);
		sb.append(IMPORT_EXTRA_PARAMETER_CONTEXT);
		sb.append("public class RULE ").append("{");
		sb.append("public Object eval(Map map, Object outerCtx){");
		Matcher returnMarcher = RETURN_WHOLE_WORD_PATTERN.matcher(expression);
		if (!returnMarcher.find()) {
			sb.append("return ");
		}
		sb.append(expression);
		sb.append("+\"\";};}");
		logger.debug(sb.toString());
		return sb.toString();
	}

	/**
	 * �滻��(map.get("name"));��������ʱͨ������ȡ�ò���ֵ�����ֵ��
	 */
	@Override
	protected String replace(com.taobao.tddl.interact.rule.Rule.RuleColumn ruleColumn) {
		return new StringBuilder("(map.get(\"").append(ruleColumn.key).append("\"))").toString();
	}

	/**
	 * ����groovy�ķ�����public Object eval(Map map,Map ctx){...}");
	 */
	@SuppressWarnings("unchecked")
	public T eval(Map<String, Object> columnValues, Object outerCtx) {
		try {
			T value = (T) ruleMethod.invoke(ruleObj, columnValues, outerCtx);
			if (value == null) {
				throw new IllegalArgumentException("rule eval resulte is null! rule:" + this.expression);
			}
			return value;
		} catch (Throwable t) {
			throw new IllegalArgumentException("���÷���ʧ��: " + ruleMethod, t);
		}
	}

	private static Method getMethod(Class<?> c, String name, Class<?>... parameterTypes) {
		try {
			return c.getMethod(name, parameterTypes);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("ʵ�����������ʧ��", e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("û���������" + name, e);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder("GroovyRule{expression=").append(expression).append(", parameters=")
				.append(parameters).append("}").toString();
	}
}

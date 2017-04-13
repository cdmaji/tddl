/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.ruleimpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.rule.Samples;
import com.taobao.tddl.interact.rule.Samples.SamplesCtx;
import com.taobao.tddl.interact.rule.bean.AdvancedParameter;
import com.taobao.tddl.interact.rule.util.RuleUtils;
import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * ͨ�����ö�ٵķ�ʽʵ�ֱȽ���ƥ��
 * 
 * @author linxuan
 *
 * @param <T>
 */
public abstract class EnumerativeRule<T> extends ExpressionRule<T> {

	public EnumerativeRule(String expression) {
		super(expression);
	}

	/**
	 * �����һ�����⣺��������ͬһ���г��ֶ�Σ�������������һ�γ��ֵĵط�дȫ�����������ĳ��ֿ��Բ�д
	 */
	@Override
	protected String parseParam(String paramInDoller, Map<String, RuleColumn> parameters) {
		RuleColumn ruleColumn = null;
		if (paramInDoller.indexOf(",") == -1) {
			//���û������������ֱ�Ӵ�parameters��ȡ
			//�������RuleColumn�Ķ���Key�Ĵ���ʽһ��(toUpperCase())������ȡ����������һ�����յ�
			ruleColumn = parameters.get(paramInDoller.trim().toUpperCase());
		}
		if (ruleColumn == null) {
			ruleColumn = AdvancedParameter.getAdvancedParamByParamTokenNew(paramInDoller, true);
			parameters.put(ruleColumn.key, ruleColumn);
		}
		return replace(ruleColumn);
	}

	abstract protected String replace(RuleColumn ruleColumn);

	public Map<T, Samples> calculate(Map<String, Comparative> sqlArgs, Object ctx, Object outerCtx) {
		Map<String, Set<Object>> enumerates = getEnumerates(sqlArgs, ctx);
		Map<T, Samples> res = new HashMap<T, Samples>(1);
		for (Map<String, Object> sample : new Samples(enumerates)) { //�����ѿ�������
			T value = this.eval(sample, outerCtx);
			if (value == null) {
				throw new IllegalArgumentException("rule eval resulte is null! rule:" + this.expression);
			}
			Samples evalSamples = res.get(value);
			if (evalSamples == null) {
				evalSamples = new Samples(sample.keySet());
				res.put(value, evalSamples);
			}
			evalSamples.addSample(sample);
		}
		return res;
	}

	public Set<T> calculateNoTrace(Map<String, Comparative> sqlArgs, Object ctx, Object outerCtx) {
		Map<String, Set<Object>> enumerates = getEnumerates(sqlArgs, ctx);
		Set<T> res = new HashSet<T>(1);
		for (Map<String, Object> sample : new Samples(enumerates)) { //�����ѿ�������
			T value = this.eval(sample, outerCtx);
			if (value == null) {
				throw new IllegalArgumentException("rule eval resulte is null! rule:" + this.expression);
			}
			res.add(value);
		}
		return res;
	}
	
	public static final String REAL_TABLE_NAME_KEY="REAL_TABLE_NAME";
	public T calculateVnodeNoTrace(String key, Object ctx, Object outerCtx) {
		Map<String,Object> sample=new HashMap<String, Object>(1);
		sample.put(REAL_TABLE_NAME_KEY, key);
		T value = this.eval(sample,outerCtx);
		if (value == null) {
			throw new IllegalArgumentException("rule eval resulte is null! rule:" + this.expression);
		}
		return value;
	}

	private Map<String, Set<Object>> getEnumerates(Map<String, Comparative> sqlArgs, Object ctx) {
		Set<AdvancedParameter> thisParam = cast(this.parameterSet);
		Map<String, Set<Object>> enumerates;
		SamplesCtx samplesCtx = (SamplesCtx) ctx;
		if (samplesCtx != null) {
			Samples commonSamples = samplesCtx.samples;
			if (samplesCtx.dealType == SamplesCtx.replace) {
				Set<AdvancedParameter> withoutCommon = new HashSet<AdvancedParameter>(thisParam.size());
				for (AdvancedParameter p : thisParam) {
					if (!commonSamples.getSubColumSet().contains(p.key)) {
						withoutCommon.add(p);
					}
				}
				enumerates = RuleUtils.getSamplingField(sqlArgs, withoutCommon);
				for (String name : commonSamples.getSubColumSet()) {
					enumerates.put(name, commonSamples.getColumnEnumerates(name)); //������ֻʹ����һ��������㣬ֵ����ķ���խ������
				}
			} else if (samplesCtx.dealType == SamplesCtx.merge) {
				enumerates = RuleUtils.getSamplingField(sqlArgs, thisParam);
				for (String diffType : commonSamples.getSubColumSet()) {
					enumerates.get(diffType).addAll(commonSamples.getColumnEnumerates(diffType));
				}
			} else {
				throw new IllegalStateException("Should not happen! SamplesCtx.dealType has a new Enum?");
			}
		} else {
			enumerates = RuleUtils.getSamplingField(sqlArgs, thisParam);
		}
		return enumerates;
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}
}

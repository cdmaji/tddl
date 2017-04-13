/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.bean.MatcherResultImp;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.rule.Rule.RuleColumn;
import com.taobao.tddl.interact.rule.Samples.SamplesCtx;
import com.taobao.tddl.interact.rule.bean.AdvancedParameter;
import com.taobao.tddl.interact.rule.ruleimpl.VirtualNodeGroovyRule;
import com.taobao.tddl.interact.rule.util.RuleUtils;
import com.taobao.tddl.interact.sqljep.Comparative;

/**
 * 1.�ڷֿ��кͷֱ���û�н���������£����Լ��㣬������ѿ�����ϡ�
 * 
 * 2.�ڷֿ��кͷֱ����н����������еĸ��Ӳ�����ȫ��ͬ������£������п���������������ͬ�ġ�
 *   ������Ҫ������������󰴿���࣬��ÿ�����ڣ�ֻ�Բ����ÿ�����ֵ���б���������ȷ����
 *   ������ܻ�������ڱ���ı������%2��%8��
 *       id%2=0--db0��t0,t2,t4,t6
 *       id%2=1--db1: t1,t3,t5,t7
 *   ������0-7��Ҫ�Ⱦ����������㣬�������ֳ�����db0:0,2,4,5��db1:1,3,5,7�ٷֱ���б�������
 * 
 * 3.�ڷֿ��кͷֱ��� ������ͬ ���Ͳ�ͬ ������£��������ȡ�������ٰ�2�ķ�ʽ���㡣�����1_month ��1_day��
 *   ����db0��˫��db1��һ��һ�ű���time in��2��5�� 3��8�� 4��20�գ���ȷ���Ӧ���ǣ�
 *       db0: t_5,t_20
 *       db1: t_8
 *   ���������������ͬ�������������㲻ͬ��������� 5��31��<=time<=6��2�ա�
 *   ��������5��31�գ�6��1�չ�2��������5��31�ա�6��1�ա�6��2�չ�3������ȷ�Ľ��Ӧ���ǣ�
 *       db0: t_1,t_2
 *       db1: t_31
 *   ���������Ǳ�������ڿ⣬ͬʱ�����˿����㡣��������뾭�����������ټ��㡣
 *   ���ڱ����㲻����ȫ����������������磺�����a,b,c�����b,c,d ....
 *   
 * 4.�����ǿ�����Ĺ����У���������������ͬ������£����������Ĳ�ͬ��
 *   ������������ͬ���䲻ͬ�ĵ������������岻��ģ������%2��%8��������ʱ��2����㣬�������Ҫ8����㣬
 *   ����������ͬһ���У����8�������Ҫȫ����һ�����򣬽��а������ۺ��ټ�������ܱ�֤�������ȷ��
 *   �����ĵ����������ڱ�ģ������id%8 ��id%3, ͳһ����������������㼸��
 *       DB0:t0,t1,t2; DB1:t0,t1,t2
 *   ����������������Թ����У���������������ͬʱ��Ҫ��Ӧ�ð��������������һ�¡�
 *    
 * @author linxuan
 *
 */
public class VirtualTableRuleMatcher {

	private static final boolean isCrossInto;
	static {
		//�ṩһ�������õĻ��ᣬ�����ھ������Ĭ�ϵ�����£�����Ӱ��ӿڲ�κʹ���ṹ
		String str = System.getProperty("com.taobao.tddl.rule.isCrossIntoCalculate", "false");
		isCrossInto = Boolean.parseBoolean(str);
	}
	
	/**
	 * @param comparativeMapChoicer
	 * @param args sql�Ĳ����б�
	 * @param rule sql�����������Ӧ�Ĺ���
	 * @return
	 */
	public MatcherResult match(boolean needSourceKey,ComparativeMapChoicer choicer, List<Object> args, VirtualTableRule<String, String> rule) {
		if (needSourceKey) {
	        //��ʱ��֧������ڵ�
			return matchWithSourceKey(choicer, args, rule);
		} else {
			return matchNoSourceKey(choicer, args, rule);
		}
	}

	private MatcherResult matchNoSourceKey(ComparativeMapChoicer choicer, List<Object> args,
			VirtualTableRule<String, String> rule) {
		//���п��������õ����кͶ�Ӧ�ıȽ���,��Ϊһ��������ƥ��ʱ�󶨲����Ļ��档
		//һ��ʼ�Ͱ����е�ȡ���������Ż���������Щ�ǲ���Ҫȡ�ġ�Ҳ��ʡ��Щ�󶨲����Ĳ���
		Map<String, Comparative> allRuleArgs = new HashMap<String, Comparative>(2);
		Map<String, Comparative> dbRuleArgs = new HashMap<String, Comparative>(2); //ƥ��Ĺ�������Ӧ�������ͱȽ���
		Map<String, Comparative> tbRuleArgs = new HashMap<String, Comparative>(2); //ƥ��Ĺ�������Ӧ�������ͱȽ���
		Object outerCtx = rule.getOuterContext();
		
		//��Ȼ����ҪdefaultDbValue��defaultTbValue��
		Rule<String> dbRule = findMatchedRule(allRuleArgs, rule.getDbShardRules(), dbRuleArgs, choicer, args, rule);
		Rule<String> tbRule = findMatchedRule(allRuleArgs, rule.getTbShardRules(), tbRuleArgs, choicer, args, rule);

		Map<String, Set<String>> topology;
		if (dbRule == null && tbRule == null) {
			//���޿���򣬾�̬��������ֻ��һ���⣬������û�����Ҳ�ã�ȫ��ɨҲ�ã�������;�̬����һ��
			//���п������ô��ȫ��ɨ��������û�����Ҳ�ã�ȫ��ɨҲ�ã������Ȼ�;�̬����һ������
			topology = rule.getActualTopology(); //���²�֪��������������
		} else if (dbRule == null) {
			Set<String> tbValues = tbRule.calculateNoTrace(tbRuleArgs, null, outerCtx);
			//1.�޿���� 2.�е���ûƥ�䵽 ����ʱ�����һ����Ϊ�գ��ҺͿ����ȫ�޹������Լ����Լ���
			topology = new HashMap<String, Set<String>>(rule.getActualTopology().size());
			for (String dbValue : rule.getActualTopology().keySet()) {
				topology.put(dbValue, tbValues);
			}
		} else if (tbRule == null) {
		    //dbRule is VirtualNodeGroovyRule, just do the FullTableScan;
			if(dbRule instanceof VirtualNodeGroovyRule){
				topology = rule.getActualTopology();
			}else{
				//1.�ޱ���� 2.�е���ûƥ�䵽 ����ʱ�����һ����Ϊ�գ��Һͱ����ȫ�޹������Լ����Լ���
				Set<String> dbValues = dbRule.calculateNoTrace(dbRuleArgs, null, outerCtx);
				topology = new HashMap<String, Set<String>>(dbValues.size());
				for (String dbValue : dbValues) {
					topology.put(dbValue, rule.getActualTopology().get(dbValue));
				}
			}
		} else {
			//�������Ϳ����������޽���
			Set<String> commonSet = getCommonColumnSet(dbRule, tbRule);
			String[] commonColumn = commonSet == null ? null : commonSet.toArray(new String[commonSet.size()]);
			if (commonColumn == null || commonColumn.length == 0) {
				//�޽���
				//modify by junyu,2011-9-23,��������ڵ�
				Set<String> tbValues = tbRule.calculateNoTrace(tbRuleArgs, null, outerCtx);
				Set<String> dbValues = null;
				if(dbRule instanceof VirtualNodeGroovyRule){
					//˵��dbRule������ӳ��
					topology = new HashMap<String, Set<String>>();
					for(String tab:tbValues){
					    String db = dbRule.calculateVnodeNoTrace(tab, null, outerCtx);
					    if(!topology.containsKey(db)){
					    	Set<String> tbSet=new HashSet<String>();
					    	tbSet.add(tab);
					    	topology.put(db, tbSet);
					    }else{
					    	topology.get(db).add(tab);
					    }
					}
				}else{
					dbValues = dbRule.calculateNoTrace(dbRuleArgs, null, outerCtx);
					topology = new HashMap<String, Set<String>>(dbValues.size());
					for (String dbValue : dbValues) {
						topology.put(dbValue, tbValues);
					}
				}
			} else {
				//�н���
				if (!isCrossInto) {
					topology = crossNoSourceKey1(dbRule, dbRuleArgs, tbRule, tbRuleArgs, commonColumn, outerCtx);
				} else {
					topology = crossNoSourceKey2(dbRule, dbRuleArgs, tbRule, tbRuleArgs, commonSet, outerCtx);
				}
			}
		}
		
		return new MatcherResultImp(buildTargetDbList(topology), dbRuleArgs, tbRuleArgs);
	}
	
	private MatcherResult matchWithSourceKey(ComparativeMapChoicer choicer, List<Object> args,
			VirtualTableRule<String, String> rule) {
		//���п��������õ����кͶ�Ӧ�ıȽ���,��Ϊһ��������ƥ��ʱ�󶨲����Ļ��档
		//һ��ʼ�Ͱ����е�ȡ���������Ż���������Щ�ǲ���Ҫȡ�ġ�Ҳ��ʡ��Щ�󶨲����Ĳ���
		Map<String, Comparative> allRuleArgs = new HashMap<String, Comparative>(2);
		Map<String, Comparative> dbRuleArgs = new HashMap<String, Comparative>(2); //ƥ��Ĺ�������Ӧ�������ͱȽ���
		Map<String, Comparative> tbRuleArgs = new HashMap<String, Comparative>(2); //ƥ��Ĺ�������Ӧ�������ͱȽ���
		Object outerCtx = rule.getOuterContext();

		//��Ȼ����ҪdefaultDbValue��defaultTbValue��
		Rule<String> dbRule = findMatchedRule(allRuleArgs, rule.getDbShardRules(), dbRuleArgs, choicer, args, rule);
		Rule<String> tbRule = findMatchedRule(allRuleArgs, rule.getTbShardRules(), tbRuleArgs, choicer, args, rule);

		Map<String, Map<String, Field>> topology;
		if (dbRule == null && tbRule == null) {
			//���޿���򣬾�̬��������ֻ��һ���⣬������û�����Ҳ�ã�ȫ��ɨҲ�ã�������;�̬����һ��
			//���п������ô��ȫ��ɨ��������û�����Ҳ�ã�ȫ��ɨҲ�ã������Ȼ�;�̬����һ������
			topology = new HashMap<String, Map<String, Field>>(rule.getActualTopology().size());
			for (Map.Entry<String, Set<String>> e : rule.getActualTopology().entrySet()) {
				topology.put(e.getKey(), toMapField(e.getValue()));
			}
		} else if (dbRule == null) {
			//1.�޿���� 2.�е���ûƥ�䵽 ����ʱ�����һ����Ϊ�գ��ҺͿ����ȫ�޹������Լ����Լ���
			Map<String, Samples> tbValues = cast(tbRule.calculate(tbRuleArgs, null, outerCtx));
			topology = new HashMap<String, Map<String, Field>>(rule.getActualTopology().size());
			for (String dbValue : rule.getActualTopology().keySet()) {
				topology.put(dbValue, toMapField(tbValues));
			}
		} else if (tbRule == null) {
			//if dbRule is VirtualNodeGroovyRule,just do the fullTableScan
			if(dbRule instanceof VirtualNodeGroovyRule){
				topology = new HashMap<String, Map<String, Field>>(rule.getActualTopology().size());
				for (Map.Entry<String, Set<String>> e : rule.getActualTopology().entrySet()) {
					topology.put(e.getKey(), toMapField(e.getValue()));
				}
			}else{
				//1.�ޱ���� 2.�е���ûƥ�䵽 ����ʱ�����һ����Ϊ�գ��Һͱ����ȫ�޹������Լ����Լ���
				Set<String> dbValues = dbRule.calculateNoTrace(dbRuleArgs, null, outerCtx);
				topology = new HashMap<String, Map<String, Field>>(dbValues.size());
				for (String dbValue : dbValues) {
					topology.put(dbValue, toMapField(rule.getActualTopology().get(dbValue)));
				}
			}
		} else {
			//�������Ϳ����������޽���
			Set<String> commonSet = getCommonColumnSet(dbRule, tbRule);
			String[] commonColumn = commonSet == null ? null : commonSet.toArray(new String[commonSet.size()]);
			if (commonColumn == null || commonColumn.length == 0) {
				//�޽���
				//modify by junyu,2011-10-24,ԭ��û�м��������Ҫ������id in group �Ż��Ĳ���
				if(dbRule instanceof VirtualNodeGroovyRule){
					Map<String,Samples> tbValues =cast(tbRule.calculate(tbRuleArgs, null, outerCtx));
					//˵��dbRule������ӳ��
					topology = new HashMap<String, Map<String,Field>>();
					Map<String,Map<String,Samples>> templogy=new HashMap<String,Map<String,Samples>>();
					for(Map.Entry<String,Samples> entry:tbValues.entrySet()){
					    String db = dbRule.calculateVnodeNoTrace(entry.getKey(), null, outerCtx);
					    if(!topology.containsKey(db)){
					    	Map<String,Samples> tbSet=new HashMap<String,Samples>();
					    	tbSet.put(entry.getKey(), entry.getValue());
					    	templogy.put(db, tbSet);
					    }else{
					    	templogy.get(db).put(entry.getKey(), entry.getValue());
					    }
					}
					
					for(Map.Entry<String,Map<String,Samples>> entry:templogy.entrySet()){
						topology.put(entry.getKey(), toMapField(entry.getValue()));
					}
				}else{
					Set<String> dbValues = dbRule.calculateNoTrace(dbRuleArgs, null, outerCtx);
					Map<String, Samples> tbValues = cast(tbRule.calculate(tbRuleArgs, null, outerCtx));
					topology = new HashMap<String, Map<String, Field>>(dbValues.size());
					for (String dbValue : dbValues) {
						topology.put(dbValue, toMapField(tbValues));
					}
				}
			} else { //�н���
				if (!isCrossInto) {
					topology = crossWithSourceKey1(dbRule, dbRuleArgs, tbRule, tbRuleArgs, commonColumn, outerCtx);
				} else {
					topology = crossWithSourceKey2(dbRule, dbRuleArgs, tbRule, tbRuleArgs, commonSet, outerCtx);
				}
			}
		}
		
		return new MatcherResultImp(buildTargetDbListWithSourceKey(topology), dbRuleArgs, tbRuleArgs);
	}

	private Map<String, Set<String>> crossNoSourceKey1(Rule<String> matchedDbRule,
			Map<String, Comparative> matchedDbRuleArgs, Rule<String> matchedTbRule,
			Map<String, Comparative> matchedTbRuleArgs, String[] commonColumn, Object outerCtx) {
		SamplesCtx dbRuleCtx = null; //���ڱ������������������ͬ���������Ͳ�ͬ���У������ö�ٽ�����������ö�ټ�
		Set<AdvancedParameter> diifTypeInCommon = diifTypeInCommon(matchedDbRule, matchedTbRule, commonColumn);
		if (diifTypeInCommon != null && !diifTypeInCommon.isEmpty()) {
			//�����а�����ö�����Ͳ�ͬ���У��������1_month����ʾ1_day
			Map<String, Set<Object>> tbTypes = RuleUtils.getSamplingField(matchedTbRuleArgs, diifTypeInCommon);
			dbRuleCtx = new SamplesCtx(new Samples(tbTypes), SamplesCtx.merge);
		}
		Map<String, Samples> dbValues = cast(matchedDbRule.calculate(matchedDbRuleArgs, dbRuleCtx, outerCtx));
		Map<String, Set<String>> topology = new HashMap<String, Set<String>>(dbValues.size());
		for (Map.Entry<String, Samples> e : dbValues.entrySet()) {
			SamplesCtx tbRuleCtx = new SamplesCtx(e.getValue().subSamples(commonColumn), SamplesCtx.replace);
			Set<String> tbValues = matchedTbRule.calculateNoTrace(matchedTbRuleArgs, tbRuleCtx, outerCtx);
			topology.put(e.getKey(), tbValues);
		}
		return topology;
	}

	private Map<String, Map<String, Field>> crossWithSourceKey1(Rule<String> matchedDbRule,
			Map<String, Comparative> matchedDbRuleArgs, Rule<String> matchedTbRule,
			Map<String, Comparative> matchedTbRuleArgs, String[] commonColumn, Object outerCtx) {
		SamplesCtx dbRuleCtx = null; //���ڱ������������������ͬ���������Ͳ�ͬ���У������ö�ٽ�����������ö�ټ�
		Set<AdvancedParameter> diifTypeInCommon = diifTypeInCommon(matchedDbRule, matchedTbRule, commonColumn);
		if (diifTypeInCommon != null && !diifTypeInCommon.isEmpty()) {
			//�����а�����ö�����Ͳ�ͬ���У��������1_month����ʾ1_day
			Map<String, Set<Object>> tbTypes = RuleUtils.getSamplingField(matchedTbRuleArgs, diifTypeInCommon);
			dbRuleCtx = new SamplesCtx(new Samples(tbTypes), SamplesCtx.merge);
		}
		Map<String, Samples> dbValues = cast(matchedDbRule.calculate(matchedDbRuleArgs, dbRuleCtx, outerCtx));
		Map<String, Map<String, Field>> topology = new HashMap<String, Map<String, Field>>(dbValues.size());
		for (Map.Entry<String, Samples> e : dbValues.entrySet()) {
			SamplesCtx tbRuleCtx = new SamplesCtx(e.getValue().subSamples(commonColumn), SamplesCtx.replace);
			Map<String, Samples> tbValues = cast(matchedTbRule.calculate(matchedTbRuleArgs, tbRuleCtx, outerCtx));
			topology.put(e.getKey(), toMapField(tbValues));
		}
		return topology;
	}

	private Map<String, Set<String>> crossNoSourceKey2(Rule<String> matchedDbRule,
			Map<String, Comparative> matchedDbRuleArgs, Rule<String> matchedTbRule,
			Map<String, Comparative> matchedTbRuleArgs, Set<String> commonSet, Object outerCtx) {
		//�н���
		String[] commonColumn = commonSet == null ? null : commonSet.toArray(new String[commonSet.size()]);
		Set<AdvancedParameter> dbParams = cast(matchedDbRule.getRuleColumnSet());
		Set<AdvancedParameter> tbParams = cast(matchedTbRule.getRuleColumnSet());
		Map<String, Set<Object>> dbEnumerates = RuleUtils.getSamplingField(matchedDbRuleArgs, dbParams);
		Set<AdvancedParameter> diifTypeInCommon = diifTypeInCommon(matchedDbRule, matchedTbRule, commonColumn);
		if (diifTypeInCommon != null && !diifTypeInCommon.isEmpty()) {
			//���������Ͳ�ͬ�Ĺ����еı�ö��ֵ�����ö��ֵ��
			Map<String, Set<Object>> diifTypeTbEnumerates = RuleUtils.getSamplingField(matchedTbRuleArgs,
					diifTypeInCommon);
			for (Map.Entry<String, Set<Object>> e : diifTypeTbEnumerates.entrySet()) {
				dbEnumerates.get(e.getKey()).addAll(e.getValue());
			}
		}
		Set<AdvancedParameter> tbOnly = new HashSet<AdvancedParameter>();
		for (AdvancedParameter param : tbParams) {
			if (!commonSet.contains(param.key)) {
				tbOnly.add(param);
			}
		}

		Map<String, Set<String>> topology = new HashMap<String, Set<String>>();
		if (tbOnly.isEmpty()) {
			//�ֿ�����ȫ�����˷ֱ���
			for (Map<String, Object> dbSample : new Samples(dbEnumerates)) { //�����ѿ�������
				String dbIndex = matchedDbRule.eval(dbSample, outerCtx);
				String tbName = matchedTbRule.eval(dbSample, outerCtx);
				addToTopology(dbIndex, tbName, topology);
			}
		} else {
			Map<String, Set<Object>> tbEnumerates = RuleUtils.getSamplingField(matchedTbRuleArgs, tbOnly);//ֻ�б��ö��
			Samples tbSamples = new Samples(tbEnumerates);
			for (Map<String, Object> dbSample : new Samples(dbEnumerates)) { //������ѿ�������
				String dbIndex = matchedDbRule.eval(dbSample, outerCtx);
				for (Map<String, Object> tbSample : tbSamples) { //�������е����еĵѿ�������
					dbSample.putAll(tbSample);
					String tbName = matchedTbRule.eval(dbSample, outerCtx);
					addToTopology(dbIndex, tbName, topology);
				}
			}
		}
		return topology;
	}

	private Map<String, Map<String, Field>> crossWithSourceKey2(Rule<String> matchedDbRule,
			Map<String, Comparative> matchedDbRuleArgs, Rule<String> matchedTbRule,
			Map<String, Comparative> matchedTbRuleArgs, Set<String> commonSet, Object outerCtx) {
		//�н���
		String[] commonColumn = commonSet == null ? null : commonSet.toArray(new String[commonSet.size()]);
		Set<AdvancedParameter> dbParams = cast(matchedDbRule.getRuleColumnSet());
		Set<AdvancedParameter> tbParams = cast(matchedTbRule.getRuleColumnSet());
		Map<String, Set<Object>> dbEnumerates = RuleUtils.getSamplingField(matchedDbRuleArgs, dbParams);
		Set<AdvancedParameter> diifTypeInCommon = diifTypeInCommon(matchedDbRule, matchedTbRule, commonColumn);
		if (diifTypeInCommon != null && !diifTypeInCommon.isEmpty()) {
			//���������Ͳ�ͬ�Ĺ����еı�ö��ֵ�����ö��ֵ��
			Map<String, Set<Object>> diifTypeTbEnumerates = RuleUtils.getSamplingField(matchedTbRuleArgs,
					diifTypeInCommon);
			for (Map.Entry<String, Set<Object>> e : diifTypeTbEnumerates.entrySet()) {
				dbEnumerates.get(e.getKey()).addAll(e.getValue());
			}
		}
		Set<AdvancedParameter> tbOnly = new HashSet<AdvancedParameter>();
		for (AdvancedParameter param : tbParams) {
			if (!commonSet.contains(param.key)) {
				tbOnly.add(param);
			}
		}

		Map<String, Map<String, Field>> topology = new HashMap<String, Map<String, Field>>();
		if (tbOnly.isEmpty()) {
			//�ֿ�����ȫ�����˷ֱ���
			for (Map<String, Object> dbSample : new Samples(dbEnumerates)) { //�����ѿ�������
				String dbIndex = matchedDbRule.eval(dbSample, outerCtx);
				String tbName = matchedTbRule.eval(dbSample, outerCtx);
				addToTopologyWithSource(dbIndex, tbName, topology, dbSample, tbParams);
			}
		} else {
			Map<String, Set<Object>> tbEnumerates = RuleUtils.getSamplingField(matchedTbRuleArgs, tbOnly);//ֻ�б��ö��
			Samples tbSamples = new Samples(tbEnumerates);
			for (Map<String, Object> dbSample : new Samples(dbEnumerates)) { //������ѿ�������
				String dbIndex = matchedDbRule.eval(dbSample, outerCtx);
				for (Map<String, Object> tbSample : tbSamples) { //�������е����еĵѿ�������
					dbSample.putAll(tbSample);
					String tbName = matchedTbRule.eval(dbSample, outerCtx);
					addToTopologyWithSource(dbIndex, tbName, topology, dbSample, tbParams);
				}
			}
		}
		return topology;
	}

	private static void addToTopology(String dbIndex, String tbName, Map<String, Set<String>> topology) {
		Set<String> tbNames = topology.get(dbIndex);
		if (tbNames == null) {
			tbNames = new HashSet<String>();
			topology.put(dbIndex, tbNames);
		}
		tbNames.add(tbName);
	}

	private static void addToTopologyWithSource(String dbIndex, String tbName,
			Map<String, Map<String, Field>> topology, Map<String, Object> tbSample, Set<AdvancedParameter> tbParams) {
		Map<String, Field> tbNames = topology.get(dbIndex);
		if (tbNames == null) {
			tbNames = new HashMap<String, Field>();
			topology.put(dbIndex, tbNames);
		}
		Field f = tbNames.get(tbName);
		if (f == null) {
			f = new Field(tbParams.size());
			tbNames.put(tbName, f);
		}
		for (AdvancedParameter ap : tbParams) {
			Set<Object> set = f.sourceKeys.get(ap.key);
			if (set == null) {
				set = new HashSet<Object>();
			}
			set.add(tbSample.get(ap.key));
		}
	}

	private Map<String, Field> toMapField(Map<String/*rule������*/, Samples/*�õ��ý��������*/> values) {
		Map<String, Field> res = new HashMap<String, Field>(values.size());
		for (Map.Entry<String, Samples> e : values.entrySet()) {
			Field f = new Field(e.getValue().size());
			f.sourceKeys = e.getValue().getColumnEnumerates();
			res.put(e.getKey(), f);
		}
		return res;
	}

	private Map<String, Field> toMapField(Set<String> values) {
		Map<String, Field> res = new HashMap<String, Field>(values.size());
		for (String valule : values) {
			res.put(valule, null);
		}
		return res;
	}

	private List<TargetDB> buildTargetDbList(Map<String, Set<String>> topology) {
		List<TargetDB> targetDbList = new ArrayList<TargetDB>(topology.size());
		
		for (Map.Entry<String, Set<String>> e : topology.entrySet()) {
			TargetDB db = new TargetDB();
			Map<String, Field> tableNames = new HashMap<String, Field>(e.getValue().size());
			for (String tbName : e.getValue()) {
				tableNames.put(tbName, null);
			}
			db.setDbIndex(e.getKey());
			db.setTableNames(tableNames);
			targetDbList.add(db);
		}
		return targetDbList;
	}

	private List<TargetDB> buildTargetDbListWithSourceKey(Map<String, Map<String, Field>> topology) {
		List<TargetDB> targetDbList = new ArrayList<TargetDB>(topology.size());
		for (Map.Entry<String, Map<String, Field>> e : topology.entrySet()) {
			TargetDB db = new TargetDB();
			db.setDbIndex(e.getKey());
			db.setTableNames(e.getValue());
			targetDbList.add(db);
		}
		return targetDbList;
	}

	private static <T> Rule<T> findMatchedRule(Map<String, Comparative> allRuleColumnArgs, List<Rule<T>> shardRules,
			Map<String, Comparative> matchArgs, ComparativeMapChoicer choicer, List<Object> args,
			VirtualTableRule<String, String> rule) {
		Rule<T> matchedRule = null;
		if (shardRules != null && shardRules.size() != 0) {
			matchedRule = findMatchedRule(allRuleColumnArgs, shardRules, matchArgs, choicer, args);
			if (matchedRule == null) {
				//�зֿ��ֱ���򣬵���û��ƥ�䵽���Ƿ�ִ��ȫ��ɨ��
				if (!rule.isAllowFullTableScan()) {
					List<Set<String>> shardColumns = new LinkedList<Set<String>>();
					for(Rule<T> r : shardRules){
						Set<String> columnSet = new LinkedHashSet<String>();
						for(RuleColumn rc : r.getRuleColumnSet()){
							columnSet.add(rc.key);
						}
						shardColumns.add(columnSet);
					}
					throw new IllegalArgumentException("sql contain no sharding column:" + shardColumns);
				}
			}
		}
		return matchedRule;
	}

	/**
	 * @return ������������Ĺ�����
	 */
	private static Set<String> getCommonColumnSet(Rule<String> matchedDbRule, Rule<String> matchedTbRule) {
		Set<String> res = null;
		for (String key : matchedDbRule.getRuleColumns().keySet()) {
			if (matchedTbRule.getRuleColumns().containsKey(key)) {
				if (res == null) {
					res = new HashSet<String>(1);
				}
				res.add(key);
			}
		}
		return res;
	}

	/**
	 * @return tbRule�к�dbRule������ͬ���������Ͳ��õ�AdvancedParameter����
	 */
	private static Set<AdvancedParameter> diifTypeInCommon(Rule<String> dbRule, Rule<String> tbRule,
			String[] commonColumn) {
		Set<AdvancedParameter> diifTypeInCommon = null;
		for (String common : commonColumn) {
			AdvancedParameter dbap = (AdvancedParameter) dbRule.getRuleColumns().get(common);
			AdvancedParameter tbap = (AdvancedParameter) tbRule.getRuleColumns().get(common);
			if (dbap.atomicIncreateType != tbap.atomicIncreateType) {
				if (diifTypeInCommon == null) {
					diifTypeInCommon = new HashSet<AdvancedParameter>(0);
				}
				diifTypeInCommon.add(tbap);
			}
		}
		return diifTypeInCommon;
	}

	/**
	 * ����һ��#a# #b?#
	 * �������#a# #c?#
	 * ��������#b?# #d?#
	 * ����Ϊ(a��c)����ѡ�����; ����Ϊ(a��d)��ѡ����һ; ����Ϊ(b)��ѡ������
	 * @param <T>
	 * @param allRuleColumnArgs
	 * @param rules
	 * @param matchArgs
	 * @return
	 */
	private static <T> Rule<T> findMatchedRule(Map<String, Comparative> allRuleColumnArgs, List<Rule<T>> rules,
			Map<String, Comparative> matchArgs, ComparativeMapChoicer choicer, List<Object> args) {
		for (Rule<T> r : rules) {
			matchArgs.clear();
			for (RuleColumn ruleColumn : r.getRuleColumns().values()) {
				Comparative comparative = getComparative(ruleColumn.key, allRuleColumnArgs, choicer, args);
				if (comparative == null) {
					break;
				}
				matchArgs.put(ruleColumn.key, comparative);
			}
			if (matchArgs.size() == r.getRuleColumns().size()) {
				return r; //��ȫƥ��
			}
		}

		for (Rule<T> r : rules) {
			matchArgs.clear();
			int mandatoryColumnCount = 0;
			for (RuleColumn ruleColumn : r.getRuleColumns().values()) {
				if (!ruleColumn.needAppear) {
					continue;
				}
				mandatoryColumnCount++;
				Comparative comparative = getComparative(ruleColumn.key, allRuleColumnArgs, choicer, args);
				if (comparative == null) {
					break;
				}
				matchArgs.put(ruleColumn.key, comparative);
			}

			if (mandatoryColumnCount != 0 && matchArgs.size() == mandatoryColumnCount) {
				return r; //��ѡ��ƥ��
			}
		}

		//���û�б�ѡ�еĹ����磺rule=..#a?#..#b?#.. ����ֻ��a����b����sql����
		arule: for (Rule<T> r : rules) {
			matchArgs.clear();
			for (RuleColumn ruleColumn : r.getRuleColumns().values()) {
				if (ruleColumn.needAppear)
					continue arule; //�����ǰ�����б�ѡ�ֱ������,��Ϊ�ߵ������ѡ���Ѿ���ƥ����
				Comparative comparative = getComparative(ruleColumn.key, allRuleColumnArgs, choicer, args);
				if (comparative != null) {
					matchArgs.put(ruleColumn.key, allRuleColumnArgs.get(ruleColumn.key));
				}
			}
			if (matchArgs.size() != 0) {
				return r; //��һ��ȫ�ǿ�ѡ�еĹ��򣬲���args�����ù���Ĳ��ֿ�ѡ��
			}
		}
		return null;
	}

	private static Comparative getComparative(String colName, Map<String, Comparative> allRuleColumnArgs,
			ComparativeMapChoicer comparativeMapChoicer, List<Object> args) {
		Comparative comparative = allRuleColumnArgs.get(colName); //�ȴӻ����л�ȡ
		if (comparative == null) {
			comparative = comparativeMapChoicer.getColumnComparative(args, colName);
			if (comparative != null) {
				allRuleColumnArgs.put(colName, comparative); //���뻺��
			}
		}
		return comparative;
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}
}

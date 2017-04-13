/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WeightRandom {
	
	private static final Log logger = LogFactory.getLog(WeightRandom.class);
	
	public static final int DEFAULT_WEIGHT_NEW_ADD = 0;
	public static final int DEFAULT_WEIGHT_INIT = 10;
	
	private Map<String, Integer> cachedWeightConfig;
	private final RuntimeConfigHolder<Weight> weightHolder = new RuntimeConfigHolder<Weight>();	
	
	/**
	 * ���ֲ������ֻ���ؽ��������޸�
	 */
	private static class Weight{
		public Weight(int[] weights, String[] weightKeys, int[] weightAreaEnds){
			this.weightKeys = weightKeys;
			this.weightValues = weights;
			this.weightAreaEnds = weightAreaEnds;
		}
		public final String[] weightKeys;  //�����߱�֤�����޸���Ԫ��
		public final int[] weightValues;   //�����߱�֤�����޸���Ԫ��
		public final int[] weightAreaEnds; //�����߱�֤�����޸���Ԫ��
	}
	
    public WeightRandom(Map<String, Integer> weightConfigs) {
		this.init(weightConfigs);
	}
    public WeightRandom(String[] keys) {
		Map<String, Integer> weightConfigs = new HashMap<String, Integer>(keys.length);
		for (String key : keys) {
			weightConfigs.put(key, DEFAULT_WEIGHT_INIT);
		}
		this.init(weightConfigs);
	}
	
	private void init(Map<String, Integer> weightConfig) {
    	this.cachedWeightConfig = weightConfig;
    	String[] weightKeys = weightConfig.keySet().toArray(new String[0]);
    	int[] weights = new int[weightConfig.size()];
    	for(int i=0; i<weights.length; i++){
    		weights[i] = weightConfig.get(weightKeys[i]);
    	}
    	int[] weightAreaEnds = genAreaEnds(weights);
    	weightHolder.set(new Weight(weights, weightKeys, weightAreaEnds));
	}
	
	/**
	 * ֧�ֶ�̬�޸�
	 */
	public void setWeightConfig(Map<String, Integer> weightConfig){
		this.init(weightConfig);
	}

	public Map<String, Integer> getWeightConfig(){
		return this.cachedWeightConfig;
	}
	
	/**
	 * ����������Ȩ��    10   9   8
	 * ��ôareaEnds����  10  19  27
	 * �������0~27֮���һ����
	 * 
	 * �ֱ�ȥ����areaEnds���Ԫ�رȡ�
	 * 
	 * ���������С��һ��Ԫ���ˣ����ʾӦ��ѡ�����Ԫ��
	 * 
	 * ע�⣺�÷������ܸı������������
	 */
	private final Random random = new Random(); 
	private String select(int[] areaEnds, String[] keys){
		int sum = areaEnds[areaEnds.length - 1];
		if(sum == 0) {
			logger.error("areaEnds: "+Arrays.toString(areaEnds));
			return null;
		}
		//ѡ��Ĺ�
		//findbugs��Ϊ���ﲻ�Ǻܺ�(ÿ�ζ��½�һ��Random)(guangxia)
		int rand = random.nextInt(sum);
		for(int i = 0; i < areaEnds.length; i++) {
			if(rand < areaEnds[i]) {
				return keys[i];
			}
		}
		return null;
	}
	
	/**
	 * @param excludeKeys ��Ҫ�ų���key�б� 
	 * @return
	 */
	public String select(List<String> excludeKeys) {
		final Weight w = weightHolder.get(); //����ʵ�ֱ�֤���ܸı�w���κ���������ݣ������̲߳���ȫ
		if (excludeKeys == null || excludeKeys.isEmpty()) {
			return select(w.weightAreaEnds, w.weightKeys);
		}
		int[] tempWeights = w.weightValues.clone();
		for (int k = 0; k < w.weightKeys.length; k++) {
			if (excludeKeys.contains(w.weightKeys[k])) {
				tempWeights[k] = 0;
			}
		}
		int[] tempAreaEnd = genAreaEnds(tempWeights);
		return select(tempAreaEnd, w.weightKeys);
	}
	
	public static interface Tryer<T extends Throwable> {
		/**
		 * @return null��ʾ�ɹ������򷵻�һ���쳣
		 */
		public T tryOne(String name);
	}
	
	/**
	 * @return null��ʾ�ɹ������򷵻�һ���쳣�б�
	 */
	public <T extends Throwable> List<T> retry(int times, Tryer<T> tryer) {
		List<T> exceptions = new ArrayList<T>(0);
		List<String> excludeKeys = new ArrayList<String>(0);
		for (int i = 0; i < times; i++) {
			String name = this.select(excludeKeys);
			T e = tryer.tryOne(name);
			if (e != null) {
				exceptions.add(e);
				excludeKeys.add(name);
			} else {
				return null;
			}
		}
		return exceptions;
	}
	
	public <T extends Throwable> List<T> retry(Tryer<T> tryer) {
		return retry(3, tryer);
	}
	
	private static int[] genAreaEnds(int[] weights) {
		if(weights == null) {
			return null;
		}
		int[] areaEnds = new int[weights.length];
		int sum = 0;
		for(int i = 0; i < weights.length; i++) {
			sum += weights[i];
			areaEnds[i] = sum;
		}
		if(logger.isDebugEnabled()) {
			logger.debug("generate "+Arrays.toString(areaEnds)+" from "+Arrays.toString(weights));
		}
		if(sum == 0) {
			logger.warn("generate "+Arrays.toString(areaEnds)+" from "+Arrays.toString(weights));
		}
		return areaEnds;
	}
}

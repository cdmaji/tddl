/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.enumerator;

import java.util.Set;

import com.taobao.tddl.interact.sqljep.Comparative;

public abstract class PartDiscontinousRangeEnumerator implements CloseIntervalFieldsEnumeratorHandler{
	
	@SuppressWarnings("rawtypes")
	protected abstract Comparable getOneStep(Comparable source,Comparable atomIncVal);
	
	/**
	 * ���ݲ�ͬ���ݵ���С��λ��>��Ϊ>=
	 * 
	 * @param to
	 * @return
	 */
	protected abstract Comparative changeGreater2GreaterOrEq(Comparative from);
	/**
	 * ���ݲ�ͬ���ݵ���С��λ��<��Ϊ<=
	 * 
	 * @param to
	 * @return
	 */
	protected abstract Comparative changeLess2LessOrEq(Comparative to);
	/**
	 * �������ķ�Χ����range.size() * atomIncrementvalue��ֵ����ô�Ϳ�������·�Ż�
	 * 
	 * @param from
	 *            ֻ��<=����µ�formֵ
	 * @param to
	 *            ֻ��>=����µ�to ֵ
	 * @param range
	 * @param atomIncrementValue
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected abstract boolean inputCloseRangeGreaterThanMaxFieldOfDifination(
			Comparable from, Comparable to,Integer cumulativeTimes,Comparable<?> atomIncrValue);
	
	/**
	 *  ����ʼֵ��ʼ,������ֵ*�ۼӴ���+��ʼֵ�������ֵ��䶯һ�����ڵ����ж�����ֵ��ö�ٵ㡣
	 * @param begin
	 * @param cumulativeTimes
	 * @param atomicIncreationValue
	 * @return
	 */
	protected abstract Set<Object> getAllPassableFields(Comparative begin,Integer cumulativeTimes,Comparable<?> atomicIncreationValue);
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void mergeFeildOfDefinitionInCloseInterval(Comparative from,
			Comparative to, Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue) {
		if(cumulativeTimes == null||atomIncrValue == null){
			throw new IllegalArgumentException("��ԭ������������Ӳ���Ϊ��ʱ����֧����sql��ʹ�÷�Χѡ����id>? and id<?");
		}
		from = changeGreater2GreaterOrEq(from);
		
		to = changeLess2LessOrEq(to);
		
		Comparable fromComparable = from.getValue();
		Comparable toComparable = to.getValue();
		
		if (inputCloseRangeGreaterThanMaxFieldOfDifination(fromComparable, toComparable,cumulativeTimes,atomIncrValue)) {
			//�����ȡ�÷�Χ���ڷ�����������һ���䶯���ڡ�ֱ�Ӷ�·��,����ȫȡ
			if(retValue != null){
				retValue.addAll(getAllPassableFields(from, cumulativeTimes,atomIncrValue));
				return ;
			}else{
				throw new IllegalArgumentException("��д��Ĳ���setΪnull");
			}
		}
	
		
		if(fromComparable.compareTo(toComparable)==0){
			//���ת��Ϊ>=��<=������£���ֵ����ˣ���ôֱ�ӷ��ء�
			retValue.add(fromComparable);
			return;
		}
		
		int rangeSize =cumulativeTimes;

		retValue.add(fromComparable);
		Comparable enumedFoD = fromComparable; 
		for (int i = 0; i < rangeSize; i++) {
			enumedFoD = getOneStep(enumedFoD, atomIncrValue);
			int compareResult = enumedFoD.compareTo(toComparable);
			if(compareResult == 0){
				//ö��ֵ����to��ֵ���򵥵İ�to��ֵ�ŵ�ö�����������
				retValue.add(toComparable);
				return;
			}else if(compareResult >0){
				//ö��ֵ����to��ֵ,���·ֿ�������Ҳ��Ҫ�����һ���¼��ϣ�������������һ����
				//�������������һ���ʱ����п��ܳ�������ֵ����һ��ֵ����from�������ֵ�ֵ���ڶ�������to������ֵ�������������һ�Σ���Ϊ�˱�֤��ȷ��ʱ������д
				//trace: http://jira.taobao.ali.com/browse/TDDL-38
				retValue.add(toComparable);
				return;
			}else{
				//ö��С��to��ֵ,���ö�ٵ�������
				retValue.add(enumedFoD);
				
			}
		}
		
	}



}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.enumerator;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeAND;
import com.taobao.tddl.interact.sqljep.ComparativeBaseList;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.interact.rule.exception.NotSupportException;
import static com.taobao.tddl.interact.rule.enumerator.EnumeratorUtils.toPrimaryValue;

public class EnumeratorImp implements Enumerator {
	private static final String DEFAULT_ENUMERATOR = "DEFAULT_ENUMERATOR";
	protected static final Map<String,CloseIntervalFieldsEnumeratorHandler> enumeratorMap = new HashMap<String, CloseIntervalFieldsEnumeratorHandler>();
	
	{
		enumeratorMap.put(Integer.class.getName(), new IntegerPartDiscontinousRangeEnumerator());
		enumeratorMap.put(Long.class.getName(), new LongPartDiscontinousRangeEnumerator());
		enumeratorMap.put(BigDecimal.class.getName(), new LongPartDiscontinousRangeEnumerator());
		enumeratorMap.put(Date.class.getName(), new DatePartDiscontinousRangeEnumerator());
		enumeratorMap.put(java.sql.Date.class.getName(), new DatePartDiscontinousRangeEnumerator());
		enumeratorMap.put(java.sql.Timestamp.class.getName(), new DatePartDiscontinousRangeEnumerator());
		enumeratorMap.put(DEFAULT_ENUMERATOR, new DefaultEnumerator());
	}
	private boolean isDebug = false;
	/**
	 * ���ݴ���Ĳ�������ʹ������ö����
	 * 
	 * TODO Ӧ�ý�ö�����޶���Χ��С����ʵö�����ĺ������þ�����a > ? and a < ?�����ı��ʽ��
	 * ��������ʱ���ǲ���Ҫ����������+ö�ٵķ�ʽ����ġ�
	 * @param comp
	 * @param needMergeValueInCloseInterval
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private  CloseIntervalFieldsEnumeratorHandler getCloseIntervalEnumeratorHandlerByComparative(Comparative comp,boolean needMergeValueInCloseInterval){
		if(!needMergeValueInCloseInterval){
			return enumeratorMap.get(DEFAULT_ENUMERATOR);
		}
		if(comp == null){
			throw new IllegalArgumentException("��֪����ǰֵ��ʲô���͵ģ��޷��ҵ���Ӧ��ö����"+comp);
		}
		
		Comparable value = comp.getValue();
		
		if(value instanceof ComparativeBaseList){
			ComparativeBaseList comparativeBaseList = (ComparativeBaseList)value;
			for(Comparative comparative:comparativeBaseList.getList()){
				return getCloseIntervalEnumeratorHandlerByComparative(comparative,needMergeValueInCloseInterval);
			}
			throw new IllegalStateException("should not be here");
		}else if(value instanceof Comparative){
			return getCloseIntervalEnumeratorHandlerByComparative(comp,needMergeValueInCloseInterval);
		}else{
			//������һ��comparative����
			CloseIntervalFieldsEnumeratorHandler enumeratorHandler = enumeratorMap.get(value.getClass().getName());
			if(enumeratorHandler != null){
				return enumeratorHandler;
			}else{
				return enumeratorMap.get(DEFAULT_ENUMERATOR);
			}
		}
	}
	@SuppressWarnings("rawtypes")
	public Set<Object> getEnumeratedValue(Comparable condition,Integer cumulativeTimes,Comparable<?> atomIncrValue,boolean needMergeValueInCloseInterval) {
		Set<Object> retValue = null;
		if (!isDebug) {
			retValue = new HashSet<Object>();
		} else {
			retValue = new TreeSet<Object>();
		}
		try {
			process(condition, retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);
		} catch (EnumerationInterruptException e) {
			processAllPassableFields(e.getComparative(),retValue, cumulativeTimes, atomIncrValue,needMergeValueInCloseInterval);
		}
		return retValue;
	}
	private void process(Comparable<?> condition, Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue
			,boolean needMergeValueInCloseInterval) {

		if (condition == null) {
			retValue.add(null);
		} else if (condition instanceof ComparativeOR) {

			processComparativeOR(condition, retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);

		} else if (condition instanceof ComparativeAND) {

			processComparativeAnd(condition, retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);

		} else if (condition instanceof Comparative) {
			processComparativeOne(condition, retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);
		} else {
			retValue.add(condition);
		}
	}

	private boolean containsEquvilentRelation(Comparative comp) {
		int comparasion = comp.getComparison();

		if (comparasion == Comparative.Equivalent
				|| comparasion == Comparative.GreaterThanOrEqual
				|| comparasion == Comparative.LessThanOrEqual) {
			return true;
		}
		return false;
	}
	@SuppressWarnings("unchecked")
	private void processComparativeAnd(Comparable<?> condition,
			Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue,boolean needMergeValueInCloseInterval) {
		List<Comparative> andList = ((ComparativeAND) condition).getList();
		// ���������о�ûʲôʵ�ʵ����壬�������ٴ���
		if (andList.size() == 2) {
			Comparable<?> arg1 = andList.get(0);
			Comparable<?> arg2 = andList.get(1);

			Comparative compArg1 = valid2varableInAndIsNotComparativeBaseList(arg1);

			Comparative compArg2 = valid2varableInAndIsNotComparativeBaseList(arg2);
//
//			if(compArg1 == null){
//				throw new IllegalArgumentException("and ��������һ��Ϊnull");
//			}
//			if(compArg2 == null){
//				throw new IllegalArgumentException("and ��������һ��Ϊnull");
//			}
			int compResult = 0;
			try {
				compArg1.setValue(toPrimaryValue(compArg1.getValue()));
				compArg2.setValue(toPrimaryValue(compArg2.getValue()));
				compResult = compArg1.getValue().compareTo(compArg2.getValue());
			} catch (NullPointerException e) {
				throw new RuntimeException("and��������һ��ֵΪnull",e);
			}
			

			if (compResult == 0) {
				// ֵ��ȣ����������=��ϵ����ô���и������㣬����һ�������㶼û��
				if (containsEquvilentRelation(compArg1)
						&& containsEquvilentRelation(compArg2)) {

					retValue.add(compArg1.getValue());
				}
				// else{
				// һ�������㶼û�е�������ӵ�
				// }
			} else if (compResult < 0) {
				// arg1 < arg2
				processTwoDifferentArgsInComparativeAnd(retValue, compArg1,
						compArg2,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);
			} else {
				// compResult>0
				// arg1 > arg2
				processTwoDifferentArgsInComparativeAnd(retValue, compArg2,
						compArg1,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);
			}
		} else {
			throw new IllegalArgumentException("Ŀǰֻ֧��һ��and�ڵ����������ӽڵ�");
		}
	}

	/**
	 * ������һ��and�����е�������ͬ��argument
	 * 
	 * @param samplingField
	 * @param from
	 * @param to
	 */
	@SuppressWarnings("rawtypes")
	private void processTwoDifferentArgsInComparativeAnd(Set<Object> retValue,
			Comparative from, Comparative to,Integer cumulativeTimes,Comparable<?> atomIncrValue
			,boolean needMergeValueInCloseInterval) {
		if (isCloseInterval(from, to)) {
			mergeFeildOfDefinitionInCloseInterval(from, to, retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);
		} else {
			Comparable temp = compareAndGetIntersactionOneValue(from, to);
			if (temp != null) {
				retValue.add(temp);
			}else{
				//�������Ѿ��������x >= ? and x = ? ���� x <= ? and x = ?�н���Ҳ������������> �� <�Ѿ���ת��Ϊ >= �Լ�<=
				//������Ҫ����������� x <= 3 and x>=5  ���࣬
				if(from.getComparison() == Comparative.LessThanOrEqual||
						from.getComparison() == Comparative.LessThan){
					if(to.getComparison() == Comparative.LessThanOrEqual
						||to.getComparison() == Comparative.LessThan){
						processAllPassableFields(from, retValue, cumulativeTimes, atomIncrValue,needMergeValueInCloseInterval);
					}else{
						//toΪGreaterThanOrEqual,����ΪEquals ��ô�Ǹ������䡣do nothing.
						
					}
				}else if(to.getComparison() == Comparative.GreaterThanOrEqual||
						to.getComparison() == Comparative.GreaterThan){
					if(from.getComparison() == Comparative.GreaterThanOrEqual||
							from.getComparison() == Comparative.GreaterThan	){
						processAllPassableFields(to, retValue, cumulativeTimes, atomIncrValue,needMergeValueInCloseInterval);
					}else{
						//fromΪLessThanOrEqual������ΪEquals,Ϊ������
					}
				}else{
					throw new IllegalArgumentException("should not be here");
				}
			}
			// else{
			// ����0<x and x=3�������
			// }
		}
	}

	/**
	 * ����һ��and������ x > 1 and x = 3 �����������������Ϊǰ���Ѿ���from �� to ��ȵ�������˴���
	 * �������ֻ��Ҫ�����ȵ�����е��������⡣
	 * ͬʱҲ������x = 1 and x = 2����������Լ�x = 1 and x>2 ��x < 1 and x =2�������
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected static Comparable compareAndGetIntersactionOneValue(
			Comparative from, Comparative to) {
		// x = from and x <= to
		if (from.getComparison() == Comparative.Equivalent) {
			if (to.getComparison() == Comparative.LessThan
					|| to.getComparison() == Comparative.LessThanOrEqual) {
				return from.getValue();
			}
		}
		// x <= from and x = to
		if (to.getComparison() == Comparative.Equivalent) {
			if (from.getComparison() == Comparative.GreaterThan
					|| from.getComparison() == Comparative.GreaterThanOrEqual) {
				return to.getValue();
			}
		}
		return null;
	}

	protected static boolean isCloseInterval(Comparative from, Comparative to) {
		int fromComparasion = from.getComparison();

		int toComparasion = to.getComparison();

		// �������ͨ����ֵ�ȴ�С�����������滹��not in,like����ı�ǣ����Ǳ��ص�д���
		if ((fromComparasion == Comparative.GreaterThan || fromComparasion == Comparative.GreaterThanOrEqual)
				&& (toComparasion == Comparative.LessThan || toComparasion == Comparative.LessThanOrEqual)) {
			return true;
		} else {
			return false;
		}

	}

	private Comparative valid2varableInAndIsNotComparativeBaseList(
			Comparable<?> arg) {
		if (arg instanceof ComparativeBaseList) {

			throw new IllegalArgumentException("��һ��and������ֻ֧��������Χ��ֵ��ͬ�����ֱ���֧��3��");
		}

		if (arg instanceof Comparative) {
			Comparative comp = ((Comparative) arg);
			int comparison = comp.getComparison();

			if (comparison == 0) {

				// 0��ʱ����ζ�������COmparativeBaseList��Comparative�Ǹ�����İ�װ����
				return valid2varableInAndIsNotComparativeBaseList(comp
						.getValue());
			} else {

				// ���������������ֵ������
				return comp;
			}
		} else {
			// ������ǻ�������Ӧ���õ��ڰ�װ
			throw new IllegalArgumentException("input value is not a comparative: "+arg);
			// return new Comparative(Comparative.Equivalent,arg);
		}

	}

	private void processComparativeOne(Comparable<?> condition,
			Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue,boolean needMergeValueInCloseInterval) {
		Comparative comp = (Comparative) condition;
		int comparison = comp.getComparison();
		switch (comparison) {
		case 0:

			// Ϊ0 ��ʱ���ʾ����İ�װ����
			process(comp.getValue(), retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);
			break;
		case Comparative.Equivalent:

			// ���ڹ�ϵ��ֱ�ӷ���collection
			retValue.add(toPrimaryValue(comp.getValue()));
			break;
		case Comparative.GreaterThan:
		case Comparative.GreaterThanOrEqual:
		case Comparative.LessThan:
		case Comparative.LessThanOrEqual:
			//������Ҫȫȡ�����
			throw new EnumerationInterruptException(comp);
		default:
			throw new NotSupportException("not support yet");
		}
	}

	private void processComparativeOR(Comparable<?> condition,
			Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue,boolean needMergeValueInCloseInterval) {
		List<Comparative> orList = ((ComparativeOR) condition).getList();

		for (Comparative comp : orList) {

			process(comp, retValue,cumulativeTimes,atomIncrValue,needMergeValueInCloseInterval);

		}
	}

	/**
	 * ��ٳ���from��to�е�����ֵ����������value
	 * 
	 * @param from
	 * @param to
	 */
	private void mergeFeildOfDefinitionInCloseInterval(
			Comparative from, Comparative to, Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue
			,boolean needMergeValueInCloseInterval){
		if(!needMergeValueInCloseInterval){
			throw new IllegalArgumentException("��򿪹����needMergeValueInCloseIntervalѡ���֧�ַֿ�ֱ�������ʹ��> < >= <=");
		}
		//�ع� �������ּܹ��£�id =? id in (?,?,?)���������·����������ж�� id > ? and id < ? or id> ? and id<? ��Ҫ��map�в��Ρ�������Ϊ��������Ƚ��٣���˿��Ժ���
		CloseIntervalFieldsEnumeratorHandler closeIntervalFieldsEnumeratorHandler = getCloseIntervalEnumeratorHandlerByComparative(from, needMergeValueInCloseInterval);
		closeIntervalFieldsEnumeratorHandler.mergeFeildOfDefinitionInCloseInterval(from, to, retValue, cumulativeTimes, atomIncrValue);
	}
	/**
	 * ������Ŀ���Ƿ���ȫ�����ܵ�ֵ����Ҫ�������޵Ķ�����Ĵ���һ���˵�����ڲ����������ֲ������ĺ������ߡ�
	 * ���ֵӦ���Ǵ�����һ��ֵ��ʼ������ԭ������ֵ�뱶����ٳ��ú�����y��һ���仯������x��Ӧ�ı仯���ڵ����е㼴�ɡ�
	 * @param retValue
	 * @param cumulativeTimes
	 * @param atomIncrValue
	 */
	private void processAllPassableFields(Comparative source ,Set<Object> retValue,Integer cumulativeTimes,Comparable<?> atomIncrValue,
			boolean needMergeValueInCloseInterval){
		if(!needMergeValueInCloseInterval){
			throw new IllegalArgumentException("��򿪹����needMergeValueInCloseIntervalѡ���֧�ַֿ�ֱ�������ʹ��> < >= <=");
		}
		//�ع� �������ּܹ��£�id =? id in (?,?,?)���������·����������ж�� id > ? and id < ? or id> ? and id<? ��Ҫ��map�в��Ρ�������Ϊ��������Ƚ��٣���˿��Ժ���
		CloseIntervalFieldsEnumeratorHandler closeIntervalFieldsEnumeratorHandler = getCloseIntervalEnumeratorHandlerByComparative(source, needMergeValueInCloseInterval);
		closeIntervalFieldsEnumeratorHandler.processAllPassableFields(source, retValue, cumulativeTimes, atomIncrValue);
	}

//	public boolean isNeedMergeValueInCloseInterval() {
//		return needMergeValueInCloseInterval;
//	}
//
//	public void setNeedMergeValueInCloseInterval(
//			boolean needMergeValueInCloseInterval) {
//		this.needMergeValueInCloseInterval = needMergeValueInCloseInterval;
//	}

	public boolean isDebug() {
		return isDebug;
	}

	public void setDebug(boolean isDebug) {
		this.isDebug = isDebug;
	}

}

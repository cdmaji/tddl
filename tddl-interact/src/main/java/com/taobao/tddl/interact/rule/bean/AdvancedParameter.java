/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.bean;

import java.util.Calendar;import java.util.Date;import java.util.HashSet;import java.util.Set;import com.taobao.tddl.interact.rule.Rule.RuleColumn;
/**
 * ��������Ҫ��ÿһ��������ӵ�е�һЩ�������ԣ�����ö��������Ҫ��һЩ��Ϣ
 * 
 * @author shenxun
 * @author junyu
 * 
 */
public class AdvancedParameter extends RuleColumn {
	/**
	 * ��������ö�����õ�
	 */
	public final Comparable<?> atomicIncreateValue;

	/**
	 * ���Ӵ�������ö�����õ�
	 */
	public final Integer cumulativeTimes;

	/**
	 * ������ǰ�����Ƿ�����Χ��ѯ��>= <= ...
	 */
	public final boolean needMergeValueInCloseInterval;

	/**
	 * ���������ͣ�����
	 */
	public final AtomIncreaseType atomicIncreateType;

	/**
	 * ��ʼ�����ֵ�����б�ͨ��"|"�ָ�
	 */
	public final Range[] rangeArray;

	public AdvancedParameter(String key, Comparable<?> atomicIncreateValue, Integer cumulativeTimes,
			boolean needAppear, AtomIncreaseType atomicIncreateType, Range[] rangeObjectArray) {
		super(key, needAppear);
		this.atomicIncreateValue = atomicIncreateValue;
		this.atomicIncreateType = atomicIncreateType;
		this.cumulativeTimes = cumulativeTimes;
		this.rangeArray = rangeObjectArray;

		if (atomicIncreateValue != null) {
			this.needMergeValueInCloseInterval = true;
		} else {
			this.needMergeValueInCloseInterval = false;
		}
	}

	public Set<Object> enumerateRange() {
		Set<Object> values = new HashSet<Object>();
		if (atomicIncreateType.isTime()) {
			Calendar c = Calendar.getInstance();
			for (Range ro : rangeArray) {
				for (int i = ro.start; i <= ro.end; i++) {
					values.add(evalTime(c, i));
				}
			}
		} else {
			for (Range ro : rangeArray) {
				for (int i = ro.start; i <= ro.end; i++) {
					values.add(i);
				}
			}
		}
		return values;
	}

	public Set<Object> enumerateRange(Object basepoint) {
		if (basepoint instanceof Number) {
			return enumerateRange(((Number) basepoint).intValue());
		} else if (basepoint instanceof Calendar) {
			return enumerateRange((Calendar) basepoint);
		} else if (basepoint instanceof Date){
			//add by junyu,��Ϊ����evalTime��ʱ��ѽ��������Date���ͣ��������ҲҪ��������߼�
			Calendar cal=Calendar.getInstance();
			cal.setTime((Date)basepoint);
			return enumerateRange(cal);
		} else {
			throw new IllegalArgumentException(basepoint + " applies on atomicIncreateType: " + atomicIncreateType);
		}
	}

	public Set<Object> enumerateRange(int basepoint) {
		Set<Object> values = new HashSet<Object>();
		if (AtomIncreaseType.NUMBER.equals(atomicIncreateType)) {
			int start = basepoint;
			int end = start + this.cumulativeTimes;
			for (int i = start; i <= end; i++) {
				values.add(i);
			}
		} else {
			throw new IllegalArgumentException("Number applies on atomicIncreateType: " + atomicIncreateType);
		}
		return values;
	}

	public Set<Object> enumerateRange(Calendar basepoint) {
		Set<Object> values = new HashSet<Object>();
		if (atomicIncreateType.isTime()) {
			for (int i = 0; i < this.cumulativeTimes; i++) {
				values.add(evalTime(basepoint, i));
			}
		} else {
			throw new IllegalArgumentException("Calendar applies on atomicIncreateType: " + atomicIncreateType);
		}
		return values;
	}

	private Object evalTime(Calendar base, int i) {
		Calendar c = (Calendar) base.clone();
		if (AtomIncreaseType.YEAR.equals(atomicIncreateType)) {
			c.add(Calendar.YEAR, i);
		} else if (AtomIncreaseType.MONTH.equals(atomicIncreateType)) {
			c.add(Calendar.MONTH, i);
		} else if (AtomIncreaseType.DATE.equals(atomicIncreateType)) {
			c.add(Calendar.DATE, i);
		} else if (AtomIncreaseType.HOUR.equals(atomicIncreateType)){
		   	c.add(Calendar.HOUR_OF_DAY, i);
		} else {
			throw new IllegalArgumentException("atomicIncreateType:" + atomicIncreateType);
		}
//		return c;
		//modify by junyu,��sql��������һ������
		return c.getTime();
	}

	@Override
	public String toString() {
		return "AdvancedParameter [atomicIncreateValue=" + atomicIncreateValue + ", cumulativeTimes=" + cumulativeTimes
				+ ", key=" + key + ", needMergeValueInCloseInterval=" + needMergeValueInCloseInterval + "needAppear="
				+ needAppear + ", atomicIncreateType=" + atomicIncreateType + "]";
	}

	public static final String PARAM_SEGMENT_SPLITOR = ",";
	public static final char NEED_APPEAR_SYMBOL = '?';
	public static final String INCREASE_TYPE_SPLITOR = "_";
	public static final String RANGE_SEGMENT_SPLITOR = "|";
	public static final String RANGE_SEGMENT_START_END_SPLITOR = "_";

	/** 
	 * @param paramToken ��������ķֱ�Ƭ�Σ���ʽ����
	 *                   #gmt_create?,1_month,-12_12# #id,1_number,1024# #name,1_string,a_z#
	 *                   #id,1_number,0_1024|1M_1M+1024#
	 * @param completeConfig ���Ϊtrue,��ôparamToken�������㶺�ŷָ���3����ʽ
	 *                       ���Ϊfalse,��ôparamToken����ֻ���÷ֱ���߷ֱ��
	 *                       2.3.x��2.4.3���Ϲ������øò���Ϊfalse
	 *                       2.4.4��֧�ֵ��¹������øò���Ϊtrue;
	 */
	public static AdvancedParameter getAdvancedParamByParamTokenNew(String paramToken, boolean completeConfig) {
		String key;
		boolean[] needAppear = new boolean[1];
		
		AtomIncreaseType atomicIncreateType = null;
		Comparable<?> atomicIncreateValue = null;
		
		Range[] rangeObjectArray = null;
		Integer cumulativeTimes = null;

		String[] paramTokens = null;		if(paramToken!=null){			paramTokens = paramToken.split(PARAM_SEGMENT_SPLITOR);		}
		switch (paramTokens.length) {
		case 1:
			if (completeConfig) {
				throw new IllegalArgumentException("�������������ȫ����ʽ����:#id,1_number,1024#");
			}
			key = parseKeyPart(paramTokens[0], needAppear);
			break;
		case 2:
			//��ֻ����������������Ĭ��Ϊnumber������ֵĬ��Ϊ1�� ����ͬcase 3
			key = parseKeyPart(paramTokens[0], needAppear);
			
			atomicIncreateType = AtomIncreaseType.NUMBER;
			atomicIncreateValue = 1;

			try {
				rangeObjectArray = parseRangeArray(paramTokens[1]);
				cumulativeTimes = getCumulativeTimes(rangeObjectArray[0]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("����Ĳ�����ΪInteger����,����Ϊ:" + paramToken, e);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
			
			break;
		case 3:
			key = parseKeyPart(paramTokens[0], needAppear);

			try {

				atomicIncreateType = getIncreaseType(paramTokens[1]);
				atomicIncreateValue = getAtomicIncreaseValue(paramTokens[1], atomicIncreateType);

				rangeObjectArray = parseRangeArray(paramTokens[2]);
				//����Ϊ���ض��з�Χ���壬����ֱ���״�
				//�����Χ�ж��("|"�ָ�)����ô�Ե�һ�εĿ��Ϊ��׼
				cumulativeTimes = getCumulativeTimes(rangeObjectArray[0]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("����Ĳ�����ΪInteger����,����Ϊ:" + paramToken, e);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
			break;
		default:
			throw new IllegalArgumentException("����Ĳ�������������Ϊ1������3����3����ʱ��Ϊ����ʹ��" + "ö��ʱ������");
		}
		return new AdvancedParameter(key, atomicIncreateValue, cumulativeTimes, needAppear[0], atomicIncreateType,
				rangeObjectArray);
	}
	
	/**
	 * ColumnName?��ʾ��ѡ
	 * @param keyPart �����ܴ���null
	 */
	private static String parseKeyPart(String keyPart, boolean[] needAppear){
		String key;
		keyPart = keyPart.trim();
		int endIndex = keyPart.length() - 1;
		if(keyPart.charAt(endIndex) == NEED_APPEAR_SYMBOL){
			needAppear[0] = true;
			key = keyPart.substring(0,endIndex);
		}else{
			needAppear[0] = false;
			key = keyPart;
		}
		return key;
	}

	private static AtomIncreaseType getIncreaseType(String paramTokenStr) {
		String[] increase = null;		if(paramTokenStr!=null){			increase=paramTokenStr.trim().split(INCREASE_TYPE_SPLITOR);		}
		if (increase.length == 1) {
			return AtomIncreaseType.NUMBER;
		} else if (increase.length == 2) {
			return AtomIncreaseType.valueOf(increase[1].toUpperCase());
		} else {
			throw new IllegalArgumentException("�������ö������:" + paramTokenStr);
		}
	}

	private static Comparable<?> getAtomicIncreaseValue(String paramTokenStr, AtomIncreaseType type) {
		String[] increase = null;		if(paramTokenStr!=null){			increase=paramTokenStr.trim().split(INCREASE_TYPE_SPLITOR);		}
		// �������Ϊ1,��ôĬ��Ϊ��������
		if (increase.length == 1) {
			return Integer.valueOf(increase[0]);
		} else if (increase.length == 2) {
			switch (type) {
			case NUMBER:
				return Integer.valueOf(increase[0]);
			case DATE:
				return new DateEnumerationParameter(Integer.valueOf(increase[0]), Calendar.DATE);
			case MONTH:
				return new DateEnumerationParameter(Integer.valueOf(increase[0]), Calendar.MONTH);
			case YEAR:
				return new DateEnumerationParameter(Integer.valueOf(increase[0]), Calendar.YEAR);
			case HOUR:
				return new DateEnumerationParameter(Integer.valueOf(increase[0]), Calendar.HOUR_OF_DAY);
			default:
				throw new IllegalArgumentException("��֧�ֵ��������ͣ�" + type);
			}
		} else {
			throw new IllegalArgumentException("�������ö������:" + paramTokenStr);
		}
	}

	private static Range[] parseRangeArray(String paramTokenStr) {
		String[] ranges = null;		if(paramTokenStr!=null){			ranges=paramTokenStr.split(RANGE_SEGMENT_SPLITOR);		}
		Range[] rangeObjArray = new Range[ranges.length];

		for (int i = 0; i < ranges.length; i++) {
			String range = ranges[i].trim();
			String[] startEnd = null;			if(range!=null){				startEnd=range.split(RANGE_SEGMENT_START_END_SPLITOR);			}
			if (startEnd.length == 1) {
				if (i == 0) {
					rangeObjArray[i] = new Range(Integer.valueOf(0), Integer.valueOf(startEnd[0]));
				} else {
					rangeObjArray[i] = new Range(fromReadableInt(startEnd[0]), fromReadableInt(startEnd[0]));
				}
			} else if (startEnd.length == 2) {
				rangeObjArray[i] = new Range(fromReadableInt(startEnd[0]), fromReadableInt(startEnd[1]));
			} else {
				throw new IllegalArgumentException("��Χ�������," + paramTokenStr);
			}
		}
		return rangeObjArray;
	}
	
	/**
	 * 1m = 1,000,000;    2M = 2,000,000
	 * 1g = 1,000,000,000 3G = 3,000,000,000
	 */
	private static int fromReadableInt(String readableInt){
		char c = readableInt.charAt(readableInt.length()-1);
		if(c == 'm' || c == 'M'){
			return Integer.valueOf(readableInt.substring(0,readableInt.length()-1)) * 1000000;
		}else if(c == 'g' || c == 'G'){
			return Integer.valueOf(readableInt.substring(0,readableInt.length()-1)) * 1000000000;
		}else{
			return Integer.valueOf(readableInt);
		}
	}

	private static Integer getCumulativeTimes(Range ro) {
		return ro.end - ro.start;
	}

	/**
	 * �����������ͣ�����֧��4��(#2011-12-5,modify by junyu,add HOUR type)
	 */
	public static enum AtomIncreaseType {
		HOUR,DATE, MONTH, YEAR, NUMBER;

		public boolean isTime() {
			return this.ordinal() < NUMBER.ordinal();
		}
	}

	public static class Range {
		public final Integer start; // ��ʼֵ
		public final Integer end; // ����ֵ

		public Range(Integer start, Integer end) {
			this.start = start;
			this.end = end;
		}
	}
}

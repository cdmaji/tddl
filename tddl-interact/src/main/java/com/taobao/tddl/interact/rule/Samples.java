/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * һ����㼯�ϵĳ���֧���̲߳���ȫ�ĵѿ���������������һ��Sample��ʾһ���ѿ���������
 * �����ж�����ö��ֵ��������ı���������ת��Ϊ�ѿ���������������ͬ��ֵ����ϣ�
 * һ��Sample��һ��Map<String, Object>��ʾ��key�����˸��У�value��Ӧÿ�е�һ��ȡֵ
 * 
 * @author linxuan
 *
 */
public class Samples implements Iterable<Map<String/*����*/, Object/*��ֵ*/>>, Iterator<Map<String, Object>> {
	private final Map<String, Set<Object>> columnEnumerates;
	private final String[] subColums; //ʹ���ļ��У�������sub
	private final Set<String> subColumSet;//��subColums����һ�£�������,ֻ��

	public Samples(Map<String, Set<Object>> columnEnumerates) {
		this.columnEnumerates = columnEnumerates;
		this.subColums = columnEnumerates.keySet().toArray(new String[columnEnumerates.size()]);
		this.subColumSet = columnEnumerates.keySet();//subColumSetֻ��������Ӧ��û����
	}

	public Samples(Map<String, Set<Object>> columnEnumerates, String[] subColumns) {
		this.columnEnumerates = columnEnumerates;
		this.subColums = subColumns;
		this.subColumSet = new HashSet<String>();
		this.subColumSet.addAll(Arrays.asList(subColumns));
		if (subColumSet.size() != subColums.length) {
			throw new IllegalArgumentException(Arrays.toString(subColumns) + " has duplicate columm");
		}
	}

	public Samples(Set<String> columnNames) {
		this.columnEnumerates = new HashMap<String, Set<Object>>();
		for (String name : columnNames) {
			this.columnEnumerates.put(name, new HashSet<Object>(1));
		}
		this.subColums = columnNames.toArray(new String[columnEnumerates.size()]);
		this.subColumSet = Collections.unmodifiableSet(columnNames);//subColumSetֻ��
	}

	/**
	 * TODO ����columnEnumerates����ķ���
	 * @param columns ���columns����������columnEnumerates�в����ڵ�key���������Ԥ��
	 */
	public Samples subSamples(String[] columns) {
		if (columns.length == this.subColums.length)
			return this; //����Ͳ��ж�columns�Ƿ񶼺�thisһ���ˣ���һ������
		return new Samples(this.columnEnumerates, columns);//���ܻ�ʹ������sub��С��󣬵��ǲ�Ӱ��ʹ�á�Ҳû���ж�һ����
	}
	
	/**
	 * @return ���subColums��columnEnumerates��ͬ����ֱ�ӷ��أ������ȡ
	 */
	public Map<String, Set<Object>> getColumnEnumerates() {
		if (this.columnEnumerates.size() == subColums.length) {
			return this.columnEnumerates;
		} else {
			Map<String, Set<Object>> res = new HashMap<String, Set<Object>>(subColums.length);
			for (String column : subColums) {
				res.put(column, this.columnEnumerates.get(column));
			}
			return res;
		}
	}
	
	/**
	 * @return �и���
	 */
	public int size() {
		return this.subColums.length;
	}

	/**
	 * TODO ����columnEnumerates����ķ���
	 * �ϲ�other���������columnEnumerates��other�е��лḲ�Ǳ������е���
	 * @return �µĶ��󣬺ͱ�������columnEnumerates������merge��Ӧ��ʹ�÷��صĶ��󣬶�����ʹ�ñ�����
	 */
	/*public Samples mergeSamples(Samples other) {
		this.columnEnumerates.putAll(other.columnEnumerates);
		this.subColumSet.addAll(other.subColumSet);
		return new Samples(this.columnEnumerates, this.subColums);
	}*/

	/**
	 * ��һ�������ö��ֵ
	 */
	public void addEnumerates(String name, Set<Object> values) {
		if (columnEnumerates.containsKey(name)) {
			columnEnumerates.get(name).addAll(values);
		} else {
			throw new IllegalArgumentException(Arrays.toString(subColums) + ", Samples not contain key:" + name);
		}
	}

	/**
	 * ���һ��Sample��ϡ���ĳ���������ڱ�Samples�У���ֱ���׿�ָ��
	 */
	public void addSample(Map<String, Object> aCartesianSample) {
		for (Map.Entry<String, Object> e : aCartesianSample.entrySet()) {
			columnEnumerates.get(e.getKey()).add(e.getValue());
		}
	}

	/**
	 * �����ǵѿ���������������ʵ��
	 */
	private Map<String, Object> currentCartesianSample; //currentCartesianProduct��ǰ�ĵѿ���ֵ
	private Iterator<Object>[] iterators;//���ַ�ʽβ��iteratorҪ�������´򿪣�KeyIterator����ᴴ���Ƚ϶ࡣ������Object[]���α�
	private int cursor;

	@SuppressWarnings("unchecked")
	public Iterator<Map<String, Object>> iterator() {
		//ÿ�ε���ǰ����ϴε���״̬
		currentCartesianSample = new HashMap<String, Object>(subColums.length);
		iterators = new Iterator[subColums.length];
		int i = cursor = 0;
		for (String name : subColums) {
			iterators[i++] = columnEnumerates.get(name).iterator();
		}
		return this;
	}

	public boolean hasNext() {
		for (Iterator<Object> it : iterators) {
			if (it.hasNext()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * ���ؽ��ֻ�ܶ�ȡ�������޸ĺ������Ԥ�ڡ�
	 * columnSamplesÿ���е�ö��ֵ���ϱ���������һ��Ԫ�ء�
	 */
	public Map<String, Object> next() {
		for (;;) {
			if (iterators[cursor].hasNext()) {
				currentCartesianSample.put(subColums[cursor], iterators[cursor].next());
				if (cursor == subColums.length - 1) {
					break;
				} else {
					cursor++;
				}
			} else {
				if (cursor == 0) {
					break; //ȫ��������
				} else {
					//���´򿪵�ǰ��iterator����һ����
					iterators[cursor] = columnEnumerates.get(subColums[cursor]).iterator();
					cursor--;
				}
			}
		}
		return currentCartesianSample;
	}

	public void remove() {
		throw new UnsupportedOperationException(getClass().getName() + ".remove()");
	}

	/**
	 * columnEnumerates��������keySet���ܺ�subColums��һ�£���������Ҫ���ַ���ֵ����һ��
	 * @return
	 */
	/*public Map<String, Set<Object>> getColumnEnumerates() {
		return columnEnumerates;
	}*/
	public Set<Object> getColumnEnumerates(String name) {
		return columnEnumerates.get(name);
	}

	public Set<String> getSubColumSet() {
		return subColumSet;
	}

	public static class SamplesCtx {
		public final static int merge = 0;
		public final static int replace = 1;
		public final Samples samples;
		public final int dealType;

		public SamplesCtx(Samples commonSamples, int dealType) {
			this.samples = commonSamples;
			this.dealType = dealType;
		}
	}

	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	public String toString() {
		StringBuilder sb = new StringBuilder("Samples{");
		for (String column : this.subColumSet) {
			sb.append(column).append("=[");
			for (Object value : this.columnEnumerates.get(column)) {
				if (value instanceof Calendar) {
					sb.append(df.format(((Calendar) value).getTime())).append(",");
				} else {
					sb.append(value).append(",");
				}
			}
			sb.append("]");
		}
		sb.append("}");
		return sb.toString();
	}

	private static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static Map<Character, Integer> rDigits = new HashMap<Character, Integer>(16);
	static {
		for (int i = 0; i < digits.length; ++i) {
			rDigits.put(digits[i], i);
		}
	}

	/**
	 * ��һ���ֽ�����ת��Ϊ�ɼ����ַ���
	 */
	public static String bytes2string(byte[] bt) {
		int l = bt.length;
		char[] out = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = digits[(0xF0 & bt[i]) >>> 4];
			out[j++] = digits[0x0F & bt[i]];
		}
		return new String(out);
	}

	/**
	 * ���ַ���ת��Ϊbytes
	 */
	public static byte[] string2bytes(String str) {
		if (null == str) {
			throw new NullPointerException("��������Ϊ��");
		}
		char[] chs = str.toCharArray();
		byte[] data = new byte[chs.length/2];
		for (int i = 0; i < data.length; ++i) {
			int h = rDigits.get(chs[i * 2]).intValue();
			int l = rDigits.get(chs[i * 2 + 1]).intValue();
			data[i] = (byte) ((h & 0x0F) << 4 | (l & 0x0F));
		}
		return data;
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		System.out.println(bytes2string("crm_scheme_detail".getBytes("utf-8")));
		byte[] bs = string2bytes("63726D5F736368656D655F64657461696C");
		System.out.println(bytes2string(bs));
		System.out.println(new String(bs,"utf-8"));
		System.out.println("------------------------------");

		bs = string2bytes("63726D5F726566756E645F7472616465");
		System.out.println(new String(bs,"utf-8"));
		bs = string2bytes("63726D5F726566756E645F7472616465");
		System.out.println(new String(bs,"utf-8"));
		
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.config;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * <p> ���ݿ�Ȩ�����ã�Ȩ��Խ�󣬱�ѡ�еĻ���Խ��.
 * 
 * <p> Ȩ������ģʽ:
 * <p> [r|R](\\d*) [w|W](\\d*) [p|P](\\d*) [q|Q](\\d*) [i|I](\\d*)
 * 
 * <p> ��ĸr��R��ʾ���Զ����ݿ���ж�����, �����һ�����ֱ�ʾ��������Ȩ�أ������ĸr��R����û�����֣���Ĭ����10;
 * 
 * <p> ��ĸw��W��ʾ���Զ����ݿ����д����, �����һ�����ֱ�ʾд������Ȩ�أ������ĸw��W����û�����֣���Ĭ����10;<
 * 
 * <p> ��ĸp��P��ʾ�����������ȼ�, ����Խ�����ȼ�Խ�ߣ����������ȴ����ȼ���ߵ����ݿ��ж����ݣ�
 * �����ĸp��P����û�����֣���Ĭ�����ȼ���0;
 * 
 * <p> ��ĸq��Q��ʾд���������ȼ�, ����Խ�����ȼ�Խ�ߣ�д�������ȴ����ȼ���ߵ����ݿ���д���ݣ�
 * �����ĸq��Q����û�����֣���Ĭ�����ȼ���0.
 * 
 * <p> ��ĸi��I��ʾ��̬DBIndex, ���û�ͨ��threadLocalָ����dbIndex��ϣ�ʵ��rw֮�ϸ�����·��
 *     һ��db����ͬʱ���ö��i����ͬ��db����������ͬ��i������ db0:i0i2,db1:i1,db2:i1,db3:i2��
 *         �û�ָ��dbIndex=0��·�ɵ�db0����ֻ��db0��i0��
 *         �û�ָ��dbIndex=1�����·�ɵ�db1��db2����db1��db2����i1��
 *         �û�ָ��dbIndex=2�����·�ɵ�db0��db3����db0��db3����i2��
 *
 * <p> �磺db1: r10w10p2, db2: r20p2, db3: rp3�����Ӧ��������Weight:
 * db1: Weight(r10w10p2)
 * db2: Weight(r20p2)
 * db3: Weight(rp3)
 * 
 * <p> ����������У���db1, db2��db3���������ݿ�Ķ������ֳ����������ȼ�:
 * p3->[db3]
 * p2->[db1, db2]
 * 
 * �����ж�����ʱ����Ϊdb3�����ȼ���ߣ��������ȴ�db3����
 * ���db3�޷����ж��������ٴ�db1, db2�����ѡһ������Ϊdb2�Ķ�Ȩ����20����db1��10������db2��ѡ�еĻ��ʱ�db1����
 * 
 * <p> ��������ݿ�������û������Ȩ���ַ���������ΪȨ���ַ�����null,
 * ��: db1: r10w10, db2, db3�����Ӧ��������Weight:
 * db1: Weight(r10w10)
 * db2: Weight(null)
 * db3: Weight(null)
 * 
 * <p> <b>Ϊ�˼���2.4֮ǰ���ϰ汾����Ȩ���ַ�����nullʱ���൱��"r10w10p0q0",
 * ������������ӣ�ʵ�ʵ����ݿ�Ȩ�������ǣ�db1: r10w10p0q0, db2: r10w10p0q0, db3: r10w10p0q0��<b>
 * 
 * @author yangzhu
 * @author linxuan add indexes i/I at 2011/01/21
 * 
 */
public class Weight {
	private static final Pattern weightPattern_r = Pattern.compile("[R](\\d*)");
	private static final Pattern weightPattern_w = Pattern.compile("[W](\\d*)");
	private static final Pattern weightPattern_p = Pattern.compile("[P](\\d*)");
	private static final Pattern weightPattern_q = Pattern.compile("[Q](\\d*)");
	private static final Pattern weightPattern_i = Pattern.compile("[I](\\d*)");

	/**
	 * ��Ȩ�أ�Ĭ����10
	 */
	public final int r;

	/**
	 * дȨ�أ�Ĭ����10
	 */
	public final int w;

	/**
	 * �����ȼ���Ĭ����0
	 */
	public final int p;

	/**
	 * д���ȼ���Ĭ����0
	 */
	public final int q;

	public final Set<Integer> indexes;

	public Weight(String weightStr) {
		//����2.4֮ǰ���ϰ汾����Ȩ���ַ�����nullʱ���൱��"r10w10p0q0",
		if (weightStr == null) {
			r = 10;
			w = 10;
			p = 0;
			q = 0;
			indexes = null;
		} else {
			weightStr = weightStr.trim().toUpperCase();

			//�����ĸ'R'��weightStr���Ҳ��������Ȩ����0��
			//�����ĸ'R'��weightStr�����ҵ��ˣ���������ĸ'R'����û�����֣��Ƕ�Ȩ����10
			r = getUnitWeight(weightStr, 'R', weightPattern_r, 0, 10);

			w = getUnitWeight(weightStr, 'W', weightPattern_w, 0, 10);

			p = getUnitWeight(weightStr, 'P', weightPattern_p, 0, 0);

			q = getUnitWeight(weightStr, 'Q', weightPattern_q, 0, 0);

			indexes = getUnitWeights(weightStr, 'I', weightPattern_i);

		}
	}

	public String toString() {
		return "Weight[r=" + r + ", w=" + w + ", p=" + p + ", q=" + q + ", indexes=" + indexes + "]";
	}

	//����ַ�c��weightStr���Ҳ������򷵻�defaultValue1��
	//����ַ�c��weightStr���Ѿ��ҵ��ˣ���������ĸc����û�����֣��򷵻�defaultValue2,
	//���򷵻���ĸc���� ������.
	private static int getUnitWeight(String weightStr, char c, Pattern p, int defaultValue1, int defaultValue2) {
		if (weightStr.indexOf(c) == -1) {
			return defaultValue1;
		} else {
			Matcher m = p.matcher(weightStr);
			m.find();

			if (m.group(1).length() == 0) {
				return defaultValue2;
			} else {
				return Integer.parseInt(m.group(1));
			}
		}
	}

	private static Set<Integer> getUnitWeights(String weightStr, char c, Pattern p) {
		if (weightStr.indexOf(c) == -1) {
			return null;
		}
		Set<Integer> is = new HashSet<Integer>();
		int start = 0;
		Matcher m = p.matcher(weightStr);
		while (m.find(start)) {
			if (m.group(1).length() != 0) {
				is.add(Integer.valueOf(m.group(1)));
			}
			start = m.end();
		}
		return is;
	}

	public static void main(String[] args) {
		System.out.println(new Weight("wr0i1"));
		System.out.println(new Weight("wr0i0I1"));
		System.out.println(new Weight("i0w10I1r20"));
		System.out.println(new Weight("i0w10I1r20i3"));
	}
}

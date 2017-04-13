/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule.groovy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.taobao.tddl.interact.rule.util.NestThreadLocalMap;

/**
 * 
 * ��ֱ������groovy�����еı�ݷ���
 * 
 * @author shenxun
 * @author linxuan
 */
public class GroovyStaticMethod {
	public static final String GROOVY_STATIC_METHOD_CALENDAR = "GROOVY_STATIC_METHOD_CALENDAR";
	private final static long[] pow10 = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000,
			10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L,
			10000000000000000L, 100000000000000000L, 1000000000000000000L };

	/**
	 * @return ����4λ���
	 */
	public static int yyyy(Date date) {
		Calendar cal = getCalendar(date);
		return cal.get(Calendar.YEAR);
	}

	public static int yyyy(Calendar cal) {
		return cal.get(Calendar.YEAR);
	}

	/**
	 * @return ����2λ��ݣ���ݵĺ���λ��
	 */
	public static int yy(Date date) {
		Calendar cal = getCalendar(date);
		return cal.get(Calendar.YEAR) % 100;
	}

	public static int yy(Calendar cal) {
		return cal.get(Calendar.YEAR) % 100;
	}

	/**
	 * @return �����·����֣�ע�⣺��1��ʼ��1-12������ Calendar.MONTH��Ӧ��ֵ��1��
	 */
	public static int month(Date date) {
		Calendar cal = getCalendar(date);
		return cal.get(Calendar.MONTH) + 1;
	}

	public static int month(Calendar cal) {
		return cal.get(Calendar.MONTH) + 1;
	}

	/**
	 * @return ����2λ���·��ִ�����01��ʼ��01-12��Calendar.MONTH��Ӧ��ֵ��1��
	 */
	public static String mm(Date date) {
		Calendar cal = getCalendar(date);
		int m = cal.get(Calendar.MONTH) + 1;
		return m < 10 ? "0" + m : String.valueOf(m);
	}

	public static String mm(Calendar cal) {
		int m = cal.get(Calendar.MONTH) + 1;
		return m < 10 ? "0" + m : String.valueOf(m);
	}

	/**
	 * @return ���� Calendar.DAY_OF_WEEK ��Ӧ��ֵ
	 */
	public static int week(Date date) {
		Calendar cal = getCalendar(date);
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	public static int week(Calendar cal) {
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * �ɹ���Ĭ�ϵ�dayofweek : ���offset  = 0;��ôΪĬ��
	 * SUNDAY=1; MONDAY=2; TUESDAY=3; WEDNESDAY=4; THURSDAY=5; FRIDAY=6; SATURDAY=7;
	 */
	public static int dayofweek(Date date, int offset) {
		Calendar cal = getCalendar(date);
		return cal.get(Calendar.DAY_OF_WEEK) + offset;
	}

	public static int dayofweek(Calendar cal, int offset) {
		return cal.get(Calendar.DAY_OF_WEEK) + offset;
	}

	/**
	 * �ɹ����dayofweek.��Ϊ�ɹ��������Ϊ�����±꣬�����������ǿ��ڱ������±꣬�����0��ʼ��
	 * ��˱�����day of week��0 ��ʼ��ͨ��ֱ��offset = -1 �����������=0,����һ=1,...������=6
	 */
	public static int dayofweek(Date date) {
		return dayofweek(date, -1);
	}

	public static int dayofweek(Calendar cal) {
		return dayofweek(cal, -1);
	}

	/**
	 * @return ����4λ��ݺ�2λ�·ݵ��ִ����·ݴ�01��ʼ��01-12
	 */
	public static String yyyymm(Date date) {
		Calendar cal = getCalendar(date);
		return yyyy(cal) + mm(cal);
	}

	public static String yyyymm(Calendar cal) {
		return yyyy(cal) + mm(cal);
	}

	/**
	 * @return ���� 4λ���_2λ�·� ���ִ����·ݴ�01��ʼ��01-12
	 */
	public static String yyyy_mm(Date date) {
		Calendar cal = getCalendar(date);
		return yyyy(cal) + "_" + mm(cal);
	}

	public static String yyyy_mm(Calendar cal) {
		return yyyy(cal) + "_" + mm(cal);
	}

	/**
	 * @return ����2λ��ݺ�2λ�·ݵ��ִ����·ݴ�01��ʼ��01-12
	 */
	public static String yymm(Date date) {
		Calendar cal = getCalendar(date);
		return yy(cal) + mm(cal);
	}

	public static String yymm(Calendar cal) {
		return yy(cal) + mm(cal);
	}

	/**
	 * @return ���� 2λ���_2λ�·� ���ִ����·ݴ�01��ʼ��01-12
	 */
	public static String yy_mm(Date date) {
		Calendar cal = getCalendar(date);
		return yy(cal) + "_" + mm(cal);
	}

	public static String yy_mm(Calendar cal) {
		return yy(cal) + "_" + mm(cal);
	}

	/**
	 * @return ���� Calendar.DATE ��Ӧ��ֵ��ÿ�µ�1��ֵΪ1, 2��ֵΪ2...
	 */
	public static int date(Date date) {
		Calendar cal = getCalendar(date);
		return cal.get(Calendar.DATE);
	}

	public static int date(Calendar cal) {
		return cal.get(Calendar.DATE);
	}

	@SuppressWarnings("unused")
	private static Calendar getCalendar(Calendar c) {
		return c;
	}

	private static Calendar getCalendar(Date date) {
		Calendar cal = (Calendar) NestThreadLocalMap.get(GROOVY_STATIC_METHOD_CALENDAR);
		if (cal == null) {
			cal = Calendar.getInstance();
			NestThreadLocalMap.put(GROOVY_STATIC_METHOD_CALENDAR, cal);
		}
		cal.setTime(date);
		return cal;
	}

	/**
	 * @param bit �����ĳ���
	 * @param table ��ֵ 
	 * @return ����ǰ�油0�ﵽbit���ȵ��ַ��������table���ȴ���bit���򷵻�table��ԭʼֵ
	 */
	public static String placeHolder(int bit, long table) {
		if (bit > 18) {
			throw new IllegalArgumentException("��ȡ��λ�����ܴ���18λ");
		}
		if (table == 0) {
			//bugfix ��0��
			return String.valueOf(pow10[bit]).substring(1);
		}
		if (table >= pow10[bit - 1]) {
			//����ֵ��width >= Ҫ��Ĳ���λ��ʱ��Ӧ��ֱ�ӷ���ԭʼ��ֵ
			return String.valueOf(table);
		}
		long max = pow10[bit];
		long placedNumber = max + table;
		return String.valueOf(placedNumber).substring(1);
	}

	@SuppressWarnings("unused")
	private static long getModRight(long targetID, int size, int bitNumber) {
		if (bitNumber < size) {
			throw new IllegalArgumentException("�����λ����Ҫ���size��С");
		}
		return (size == 0 ? 0 : targetID / pow10[bitNumber - size]);
	}

	/**
	 * ����ʼ��ȡָ�����λ����Ĭ����һ��long�γ��ȵ����ݣ�Ҳ����bitNumber= 19
	 * 
	 * @param targetID Ŀ��id��Ҳ���ǵȴ���decode������
	 * @param st ���Ķ���ʼȡ�������ȡ����ߵ�һλ��ô��������st = 0;ed =1;
	 * @param ed ȡ���Ķ��������ȡ����ߵ���λ����ô��������st = 0;ed = 2;
	 * @return
	 */
	//	public static long leftBetween(long targetID,int st,int ed){
	//		int sizeAll = st + ed - 1;
	//		if(sizeAll >= 19||sizeAll <= 0){
	//			throw new IllegalArgumentException("��ȡ19λ��ֱ��ʹ��Ԫ���ݡ�");
	//		}
	//		if(targetID / pow10[sizeAll] < 1){
	//			throw new IllegalArgumentException(targetID+",С��"+(st+ed)+"λ�����ܽ��м���");
	//		}
	//		long end = getModRight(targetID, ed,19);
	//		return end % pow10[(ed-st)];
	//	}
	public static int quarter(Date date) {
		Calendar cal = getCalendar(date);
		int month = cal.get(Calendar.MONTH);
		return quarter(month);
	}

	public static int quarter(long month) {
		return quarter((int) month);
	}

	public static int halfayear(long month) {
		return halfayear((int) month);
	}

	public static int quarter(int month) {
		if (month > 11 || month < 0) {
			throw new IllegalArgumentException("month range is 1~12");
		}
		return month / 3 + 1;
	}

	public static int halfayear(Date date) {
		Calendar cal = getCalendar(date);
		int month = cal.get(Calendar.MONTH);
		return halfayear(month);
	}

	public static int halfayear(int month) {
		if (month > 11 || month < 0) {
			throw new IllegalArgumentException("month range is 1~12,current value is " + month);
		}
		return month / 6 + 1;
	}

	/**
	 * ���ҿ�ʼ��ȡָ�����λ����
	 * ���������1234567.��ôrightBetwen(1234567,2,3) ���ص������� 345
	 * rightBetween(10000234,2,2) ���ص�������2
	 * rightBetween(10000234,3,2) ���ص�������0
	 * 
	 * @param targetID Ŀ��id��Ҳ���ǵȴ���decode������
	 * @param closeFrom ���Ķ���ʼȡ�������ȡ���ұߵ�һλ��ô��������st = 0;ed =1;
	 * @param openTo ȡ���Ķ��������ȡ���ұߵ���λ����ô��������st = 0;ed = 2;
	 * @throws
	 * 		IllegalArgumentException ���st+ed -1 >= 19,��ʱ���long��˵����Ҫ��ȡ��
	 * 								 ���targetIdС��st+ed��
	 * @return
	 */
	public static long rightCut(long targetID, int closeFrom, int openTo) {
		int sizeAll = closeFrom + openTo - 1;
		if (sizeAll >= 19 || sizeAll < 0) {
			throw new IllegalArgumentException("��ȡ19λ��ֱ��ʹ��Ԫ���ݡ�");
		}

		long right = targetID / pow10[(closeFrom)];
		right = right % pow10[openTo];

		return right;
	}

	public static long right(long targetID, int size) {
		if (size >= 19 || size < 0) {
			throw new IllegalArgumentException("��ȡ19λ��ֱ��ʹ��Ԫ���ݡ�");
		}
		return targetID % pow10[size];
	}

	public static void validate(long targetID, int size) {
		if (targetID / pow10[size - 1] < 1) {
			throw new IllegalArgumentException(targetID + ",С��" + (size) + "λ�����ܽ��м���");
		}
	}

	public static String right(String right, int rightLength) {
		int length = right.length();
		int start = length - rightLength;
		return right.substring(start < 0 ? 0 : start);
	}

	public static void main(String[] args) {
		Calendar cal = Calendar.getInstance();
		System.out.println(quarter(new Date(2010, 0, 1)));
		List<AtomicInteger> li = new ArrayList<AtomicInteger>();
		//		for(int i = 0 ; i < 90; i ++){
		//			li.add(new AtomicInteger(0));
		//		}
		//		for(int i = 0 ; i < 3000; i++){
		//			cal.add(Calendar.DATE, 1);
		//			int wom = getCalendar(cal.getTime()).get(Calendar.DAY_OF_YEAR);
		//			
		//			li.get(wom % 90).incrementAndGet();
		//		}
		//		
		//		System.out.println(li.size());
		//		int i = 0;
		//		for(AtomicInteger inte : li){
		//	
		//			System.out.println(i+"->"+inte.toString());
		//			i++;
		//		}
		cal.setTime(new Date(2012, 0, 1));
		System.out.println(cal.get(Calendar.DAY_OF_YEAR));
	}
}

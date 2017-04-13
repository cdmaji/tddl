/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleNamedMessageFormatTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFormat() {
		SimpleNamedMessageFormat mf0 = new SimpleNamedMessageFormat("һ��34����78��ʮ");
		SimpleNamedMessageFormat mf1 = new SimpleNamedMessageFormat("{one}��34����78��{ten}");
		SimpleNamedMessageFormat mf2 = new SimpleNamedMessageFormat("һ{two}34����78{nine}ʮ");
		SimpleNamedMessageFormat mf3 = new SimpleNamedMessageFormat("һ��{three}4����78��ʮ");
		SimpleNamedMessageFormat mf4 = new SimpleNamedMessageFormat("{one}��{three}{three}4����78��{ten}");
		SimpleNamedMessageFormat mf5 = new SimpleNamedMessageFormat("һ��34����78��{ten}");
		SimpleNamedMessageFormat mf6 = new SimpleNamedMessageFormat("{one}��34����78��ʮ");
		SimpleNamedMessageFormat mf7 = new SimpleNamedMessageFormat("{one}{one}��34����78��{ten}{ten}");
		SimpleNamedMessageFormat mf8 = new SimpleNamedMessageFormat("{one}{two}3{four}{five}��78{nine}{ten}");
		SimpleNamedMessageFormat mf9 = new SimpleNamedMessageFormat("һ{nine}2{nine}3{nine}");
		SimpleNamedMessageFormat mfa = new SimpleNamedMessageFormat("{one}��{thr{one}ee}4����78��{ten}");
		SimpleNamedMessageFormat mfb = new SimpleNamedMessageFormat("{one}��{hasnoargs}4����78��{ten}");
		
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("one", "һ");
		params.put("two", "��");
		params.put("three", 3);
		params.put("four", 4);
		params.put("five", "��");
		params.put("nine", "��");
		params.put("ten", "ʮ");
		
		Assert.assertEquals(mf0.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf1.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf2.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf3.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf4.format(params),"һ��334����78��ʮ");
		Assert.assertEquals(mf5.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf6.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf7.format(params),"һһ��34����78��ʮʮ");
		Assert.assertEquals(mf8.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf9.format(params),"һ��2��3��");
		Assert.assertEquals(mfa.format(params),"һ��{thr{one}ee}4����78��ʮ");
		Assert.assertEquals(mfb.format(params),"һ��{hasnoargs}4����78��ʮ");
	}
	@Test
	public void testFormat2() {
		SimpleNamedMessageFormat mf0 = new SimpleNamedMessageFormat("һ��34����78��ʮ", "${", "}");
		SimpleNamedMessageFormat mf1 = new SimpleNamedMessageFormat("${one}��34����78��${ten}", "${", "}");
		SimpleNamedMessageFormat mf2 = new SimpleNamedMessageFormat("һ${two}34����78${nine}ʮ", "${", "}");
		SimpleNamedMessageFormat mf3 = new SimpleNamedMessageFormat("һ��${three}4����78��ʮ", "${", "}");
		SimpleNamedMessageFormat mf4 = new SimpleNamedMessageFormat("${one}��${three}${three}4����78��${ten}", "${", "}");
		SimpleNamedMessageFormat mf5 = new SimpleNamedMessageFormat("һ��34����78��${ten}", "${", "}");
		SimpleNamedMessageFormat mf6 = new SimpleNamedMessageFormat("${one}��34����78��ʮ", "${", "}");
		SimpleNamedMessageFormat mf7 = new SimpleNamedMessageFormat("${one}${one}��34����78��${ten}${ten}", "${", "}");
		SimpleNamedMessageFormat mf8 = new SimpleNamedMessageFormat("${one}${two}3${four}${five}��78${nine}${ten}", "${", "}");
		SimpleNamedMessageFormat mf9 = new SimpleNamedMessageFormat("һ${nine}2${nine}3${nine}", "${", "}");
		SimpleNamedMessageFormat mfa = new SimpleNamedMessageFormat("һ${nine}2{nine}3${nine}", "${", "}");
		
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("one", "һ");
		params.put("two", "��");
		params.put("three", 3);
		params.put("four", 4);
		params.put("five", "��");
		params.put("nine", "��");
		params.put("ten", "ʮ");
		
		Assert.assertEquals(mf0.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf1.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf2.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf3.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf4.format(params),"һ��334����78��ʮ");
		Assert.assertEquals(mf5.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf6.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf7.format(params),"һһ��34����78��ʮʮ");
		Assert.assertEquals(mf8.format(params),"һ��34����78��ʮ");
		Assert.assertEquals(mf9.format(params),"һ��2��3��");
		Assert.assertEquals(mfa.format(params),"һ��2{nine}3��");
	}

	@Test
	public void testFormatReuse() {
		SimpleNamedMessageFormat mf = new SimpleNamedMessageFormat("{one}��3{four}��6{seven}8��{ten}");
		
		Map<String,Object> params = new HashMap<String,Object>();
		params.put("one", "һ");
		params.put("four", "��");
		params.put("seven", "��");
		params.put("ten", "ʮ");
		Assert.assertEquals(mf.format(params),"һ��3����6��8��ʮ");
		
		params.put("one", "1");
		params.put("four", "4");
		params.put("seven", "7");
		params.put("ten", "a");
		Assert.assertEquals(mf.format(params),"1��34��678��a");

		params.put("one", "��");
		params.put("four", "��");
		params.put("seven", "��");
		params.put("ten", "��");
		Assert.assertEquals(mf.format(params),"�Ķ�3����6��8����");
	}
}

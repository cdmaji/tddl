/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.sample.group;

import java.util.Date;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import com.taobao.tddl.common.GroupDataSourceRouteHelper;
import com.taobao.tddl.sample.base.GroupSampleCase;
import com.taobao.tddl.sample.util.DateUtil;

/**
 * Comment for GroupBaseCRUDTest
 * <p/>
 * Author By: zhuoxue.yll
 * Created Date: 2012-2-29 ����02:26:50 
 */

@SuppressWarnings("rawtypes")
public class GroupSample extends GroupSampleCase {

	/**
	 * ��ָ���ı��в�������
	 *
	 */
	@Test
	public void insertByPreStTest() {
		String sql = "insert into normaltbl_0001 (pk,gmt_create) values (?,?)";
		Object[] arguments = new Object[] { RANDOM_ID, time };
		tddlJT.update(sql, arguments);

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=?", new Object[] { RANDOM_ID });
		Assert.assertEquals(time, String.valueOf(re.get("gmt_create")));
	}

	/**
	 * ��ָ���ı��������
	 *
	 */
	@Test
	public void updateByPreStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		String sql = "update normaltbl_0001 set gmt_create=? where pk=?";
		Object[] arguments = new Object[] { nextDay, RANDOM_ID };
		tddlJT.update(sql, arguments);

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=?", new Object[] { RANDOM_ID });
		Assert.assertEquals(nextDay, String.valueOf(re.get("gmt_create")));
	}

	/**
	 * ��ָ���ı�ɾ������
	 *
	 */
	@Test
	public void queryByPreStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		String sql = "select * from normaltbl_0001 where pk=?";
		Object[] arguments = new Object[] { RANDOM_ID };
		Map re = tddlJT.queryForMap(sql, arguments);
		Assert.assertEquals(time, String.valueOf(re.get("gmt_create")));
	}

	/**
	 * ��ָ���ı��ѯ����
	 *
	 */
	@Test
	public void deleteByPreStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		String sql = "delete from normaltbl_0001 where pk=?";
		Object[] arguments = new Object[] { RANDOM_ID };
		tddlJT.update(sql, arguments);

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		List re = tddlJT.queryForList("select * from normaltbl_0001 where pk=?", new Object[] { RANDOM_ID });
		Assert.assertEquals(0, re.size());
	}

	/**
	 * ��ָ���ı���replace����
	 *
	 */
	@Test
	public void replaceByPreStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		String sql = "replace into normaltbl_0001 (pk,gmt_create) values (?,?)";
		tddlJT.update(sql, new Object[] { RANDOM_ID, nextDay });
		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=?", new Object[] { RANDOM_ID });
		Assert.assertEquals(nextDay, String.valueOf(re.get("gmt_create")));
	}

	/**
	 * ��ָ���ı��в�������
	 *
	 */
	@Test
	public void insertByStTest() {
		String sql = "insert into normaltbl_0001 (pk,gmt_create) values (" + RANDOM_ID + ",CURDATE())";
		tddlJT.update(sql);

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=" + RANDOM_ID);
		Assert.assertEquals(time, DateUtil.formatDate((Date) re.get("gmt_create"), DateUtil.DATE_FULLHYPHEN));
	}

	/**
	 * ��ָ���ı��������
	 *
	 */
	@Test
	public void updateByStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		String sql = "update normaltbl_0001 set gmt_create= ADDDATE(CURDATE(),INTERVAL 1 DAY) where pk=" + RANDOM_ID;
		tddlJT.update(sql);

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=" + RANDOM_ID);
		Assert.assertEquals(nextDay, DateUtil.formatDate((Date) re.get("gmt_create"), DateUtil.DATE_FULLHYPHEN));
	}

	/**
	 * ��ָ���ı�ɾ������
	 *
	 */
	@Test
	public void queryByStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=" + RANDOM_ID);
		Assert.assertEquals(time, DateUtil.formatDate((Date) re.get("gmt_create"), DateUtil.DATE_FULLHYPHEN));
	}

	/**
	 * ��ָ���ı��ѯ����
	 *
	 */
	@Test
	public void deleteByStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });

		String sql = "delete from normaltbl_0001 where pk=" + RANDOM_ID;
		tddlJT.update(sql);

		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		List re = tddlJT.queryForList("select * from normaltbl_0001 where pk=" + RANDOM_ID);
		Assert.assertEquals(0, re.size());
	}

	/**
	 * ��ָ���ı���replace����
	 *
	 */
	@Test
	public void replaceByStTest() {
		prepareData(tddlJT, "insert into normaltbl_0001 (pk,gmt_create) values (?,?)", new Object[] { RANDOM_ID, time });
		String sql = "replace into normaltbl_0001 (gmt_create,pk) values (ADDDATE(CURDATE(),INTERVAL 1 DAY),"
				+ RANDOM_ID + ")";
		tddlJT.update(sql);
		GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
		Map re = tddlJT.queryForMap("select * from normaltbl_0001 where pk=" + RANDOM_ID);
		Assert.assertEquals(nextDay, DateUtil.formatDate((Date) re.get("gmt_create"), DateUtil.DATE_FULLHYPHEN));
	}

}

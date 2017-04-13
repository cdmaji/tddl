/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.integration;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;

import com.taobao.tddl.jdbc.group.TGroupDataSource;
import com.taobao.tddl.jdbc.group.testutil.DBHelper;

/**
 * 
 * @author yangzhu
 *
 */
public class MasterSlaveSwitchingTest {
	public static final String appName = "unitTest";
	public static final String dbGroupKey = "myDbGroupKey2";

	@Before
	public void setUp() throws Exception {
		DBHelper.deleteAll();
	}

	@Test
	public void switching() throws Exception {
		if (true)
			return; //Ҫ�ֹ�����ע�͵���һ��

		TGroupDataSource ds = new TGroupDataSource(dbGroupKey, appName);
		ds.setAutoSelectWriteDataSource(true);
		ds.init();

		Connection conn = ds.getConnection();
		Statement stmt = conn.createStatement();

		//�����������һ����¼
		assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(10,'str')"), 1);

		System.out.println("�����������һ����¼...");

		System.out.println("�������״̬�ĳ�NA����Ϣһ�ְ���..."); //ͬʱ��ر�MySQL���ݿ������
		Thread.sleep(90 * 1000);
		try {
			stmt.executeUpdate("update crud set f2='str2'");
			fail("���⴦��NA״̬���ܽ��и��²���");
		} catch (Exception e) {
			System.out.println("���⵱ǰ����NA״̬���޷����и��²�����Exception: " + e);

			System.out.println("�ؽ�Connection"); //���ܻ���ԭ��Connection�ؽ�Statement
			conn = ds.getConnection();
			System.out.println("�ؽ�Statement");
			stmt = conn.createStatement();
		}

		System.out.println("�ѱ�1��״̬�ĳ�RW����Ϣ�����...");
		Thread.sleep(30 * 1000);

		try {
			assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(10,'str')"), 1);
			System.out.println("������1���в���һ����¼...");
		} catch (Exception e) {
			e.printStackTrace();
			fail("��1��״̬�Ѹĳ�RW�����ǲ��ܽ��и��²���");
		}

		stmt.close();
		conn.close();
	}
}

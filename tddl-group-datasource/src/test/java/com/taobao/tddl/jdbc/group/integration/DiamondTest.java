/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.integration;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.taobao.tddl.jdbc.group.TGroupDataSource;

import com.taobao.tddl.jdbc.group.exception.ConfigException;
import com.taobao.tddl.jdbc.group.exception.TGroupDataSourceException;
import com.taobao.tddl.jdbc.group.testutil.DBHelper;

/**
 * 
 * @author yangzhu
 *
 */
public class DiamondTest {
	public static final String appName = "unitTest";
	public static final String dbGroupKey1 = "myDbGroupKey1";
	public static final String dbGroupKey2 = "myDbGroupKey2";
	public static final String dbGroupKey3 = "myDbGroupKey3";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//DBHelper.deleteAll(); //ɾ����������crud������м�¼
		//System.out.println("user.home=" + System.getProperty("user.home"));
	}
	
	@Before
	public void setUp() throws Exception {
		DBHelper.deleteAll();
	}

	TGroupDataSource ds;

	@Test(expected = TGroupDataSourceException.class)
	public void û������DbGroupKey() {
		ds = new TGroupDataSource();
		ds.init();
	}
	
	@Test(expected = TGroupDataSourceException.class)
	public void DbGroupKey�����ǿհ�() {
		ds = new TGroupDataSource(" ","");
		ds.init();
	}
	
	@Test(expected = TGroupDataSourceException.class)
	public void û������appName() {
		ds = new TGroupDataSource();
		ds.init();
	}
	
	@Test(expected = TGroupDataSourceException.class)
	public void appName�����ǿհ�() {
		ds = new TGroupDataSource("mygroup","  ");
		ds.init();
	}

	@Test(expected = ConfigException.class)
	public void ����DbGroupKey�����������Ҳ���ֵ() {
		ds = new TGroupDataSource("mygroup","myappname");
		ds.init();
	}

	public class MyThread extends Thread {

		public void run() {
			try {
				if (index == 1)
					�������ݿ�();
				else if (index == 2)
					�������ݿ�_����db1�ɶ�д_db2��db3ֻ�ܶ�();
				else if (index == 3)
					�������ݿ�_����db1ֻ��д_db2��db3ֻ�ܶ�();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		int index;

		MyThread(int index) {
			this.index = index;
		}
	}

	@Test
	public void test() throws Exception {
		MyThread t1 = new MyThread(1);
		t1.start();
		t1.join();
		MyThread t2 = new MyThread(2);
		t2.start();
		t2.join();
		MyThread t3 = new MyThread(3);
		t3.start();
		t3.join();
	}

	//@Test
	public void �������ݿ�() throws Exception {
		ds = new TGroupDataSource();
		ds.setDbGroupKey(dbGroupKey1);
		ds.setAppName(appName);
		ds.init();

		Connection conn = ds.getConnection();

		//����Statement��crud
		Statement stmt = conn.createStatement();
		assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(10,'str')"), 1);
		assertEquals(stmt.executeUpdate("update crud set f2='str2'"), 1);
		ResultSet rs = stmt.executeQuery("select f1,f2 from crud");
		rs.next();
		assertEquals(rs.getInt(1), 10);
		assertEquals(rs.getString(2), "str2");
		assertEquals(stmt.executeUpdate("delete from crud"), 1);
		rs.close();
		stmt.close();

		//����PreparedStatement��crud
		String sql = "insert into crud(f1,f2) values(?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setInt(1, 10);
		ps.setString(2, "str");
		assertEquals(ps.executeUpdate(), 1);
		ps.close();

		sql = "update crud set f2=?";
		ps = conn.prepareStatement(sql);
		ps.setString(1, "str2");
		assertEquals(ps.executeUpdate(), 1);
		ps.close();

		sql = "select f1,f2 from crud";
		ps = conn.prepareStatement(sql);
		rs = ps.executeQuery();
		rs.next();
		assertEquals(rs.getInt(1), 10);
		assertEquals(rs.getString(2), "str2");
		rs.close();
		ps.close();

		sql = "delete from crud";
		ps = conn.prepareStatement(sql);
		assertEquals(ps.executeUpdate(), 1);
		ps.close();

		conn.close();
	}

	//@Test
	public void ��ü��ܺ������() throws Exception {
		//��һ����Ϊ�˻������"tddl"���ܺ���ַ�����psswd.properties�ļ��е�encPasswd���Ǵ����������
		System.out.println(com.taobao.datasource.resource.security.SecureIdentityLoginModule.encode("TAtomUnitTest",
				"tddl"));
	}

	//dbGroup: db1:r10w, db2:r20, db3:r30
	//@Test
	public void �������ݿ�_����db1�ɶ�д_db2��db3ֻ�ܶ�() throws Exception {
		//����ʱ���п��ܴ�db3����Ȼ����db2��db1��Ȩ����С
		ds = new TGroupDataSource(dbGroupKey2, appName);
		ds.init();

		Connection conn = ds.getConnection();

		//����Statement��crud
		Statement stmt = conn.createStatement();
		assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(10,'str')"), 1);
		assertEquals(stmt.executeUpdate("update crud set f2='str2'"), 1);
		ResultSet rs = stmt.executeQuery("select f1,f2 from crud");
		rs.next();
		assertEquals(rs.getInt(1), 10);
		assertEquals(rs.getString(2), "str2");
		assertEquals(stmt.executeUpdate("delete from crud"), 1);
		rs.close();
		stmt.close();

		//����PreparedStatement��crud
		String sql = "insert into crud(f1,f2) values(10,'str')";
		PreparedStatement ps = conn.prepareStatement(sql);
		assertEquals(ps.executeUpdate(), 1);
		ps.close();

		sql = "update crud set f2='str2'";
		ps = conn.prepareStatement(sql);
		assertEquals(ps.executeUpdate(), 1);
		ps.close();

		sql = "select f1,f2 from crud";
		ps = conn.prepareStatement(sql);
		rs = ps.executeQuery();
		rs.next();
		assertEquals(rs.getInt(1), 10);
		assertEquals(rs.getString(2), "str2");
		rs.close();
		ps.close();

		sql = "delete from crud";
		ps = conn.prepareStatement(sql);
		assertEquals(ps.executeUpdate(), 1);
		ps.close();

		conn.close();
	}

	//dbGroup: db1:w, db2:r20, db3:r30
	//@Test
	public void �������ݿ�_����db1ֻ��д_db2��db3ֻ�ܶ�() throws Exception {
		ds = new TGroupDataSource(dbGroupKey3, appName);
		ds.init();
		Connection conn = ds.getConnection();

		//����Statement��crud
		Statement stmt = conn.createStatement();
		assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(100,'str')"), 1);
		ResultSet rs = stmt.executeQuery("select f1,f2 from crud where f1=100");
		assertFalse(rs.next());
		//assertTrue(rs.next());
		rs.close();

		assertEquals(stmt.executeUpdate("delete from crud where f1=100"), 1);
		stmt.close();

		conn.close();
	}
}

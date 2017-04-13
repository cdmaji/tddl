/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.integration;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.taobao.diamond.mockserver.MockServer;
import com.taobao.tddl.jdbc.group.TGroupDataSource;

import com.taobao.tddl.jdbc.group.testutil.DBHelper;
import com.taobao.tddl.jdbc.group.testutil.TAtomDataSourceConfigBase;

/**
 * 
 * @author yangzhu
 *
 */
public class CRUD_TAtomDataSourceTest extends TAtomDataSourceConfigBase {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//DBHelper.deleteAll(); //ɾ����������crud������м�¼
		MockServer.setUpMockServer();

		initConfig();
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		MockServer.tearDownMockServer();
	}
	
	@Before
	public void setUp() throws Exception {
		DBHelper.deleteAll();
	}

	TGroupDataSource ds;

	@Test
	public void �������ݿ�() throws Exception {
		MockServer.setConfigInfo(TGroupDataSource.getFullDbGroupKey(dbGroupKey), "db1:rw");
		ds = new TGroupDataSource();
		ds.setDbGroupKey(dbGroupKey);
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
	@Test
	public void �������ݿ�_����db1�ɶ�д_db2��db3ֻ�ܶ�() throws Exception {
		//����ʱ���п��ܴ�db3����Ȼ����db2��db1��Ȩ����С
		MockServer.setConfigInfo(TGroupDataSource.getFullDbGroupKey(dbGroupKey), "db1:r10w, db2:r20, db3:r30");
		ds = new TGroupDataSource(dbGroupKey, appName);
		ds.init();

		Connection conn = ds.getConnection();

		//����Statement��crud
		Statement stmt = conn.createStatement();
		assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(10,'str')"), 1);
		assertEquals(stmt.executeUpdate("update crud set f2='str2'"), 1);
		ResultSet rs = stmt.executeQuery("select f1,f2 from crud");
		rs.next();
		//���Զ���������
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
	@Test
	public void testWRR() throws Exception {
		MockServer.setConfigInfo(TGroupDataSource.getFullDbGroupKey(dbGroupKey), "db1:w, db2:r20, db3:r30");
		ds = new TGroupDataSource(dbGroupKey, appName);
		ds.init();
		Connection conn = ds.getConnection();

		
		Statement stmt = conn.createStatement();
		assertEquals(stmt.executeUpdate("insert into crud(f1,f2) values(100,'str')"), 1);
		
		//��ֻд���ϸ��º󣬻ᱣ��д���ӣ�
		//������Ϊд���Ӷ�Ӧ������Դ�����ó�ֻд�����Խ������Ķ�������������д�����Ͻ���
		//��Ϊdb2,db3��û�����ݣ�����rs.next()����false
		ResultSet rs = stmt.executeQuery("select f1,f2 from crud where f1=100");
		assertFalse(rs.next());
		rs.close();

		assertEquals(stmt.executeUpdate("delete from crud where f1=100"), 1);
		stmt.close();

		conn.close();
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	//package com.taobao.tddl.common.dbroute;
//
//import java.io.Serializable;
//
//import com.alibaba.common.lang.StringUtil;
//
//public class DBRoute implements Serializable {
//	/** Comment for <code>serialVersionUID</code> */
//	private static final long serialVersionUID = 3258135734605854519L;
//
//	public static final int BY_XID = 0;
//
//	public static final int BY_USER = 1;
//	
//	public static final int BY_USER_LONG = 2;
//	private String userId = null;
//	public static final long DEFAULT_USER_ID_LONG=-10000;
//	private long userIDL=DEFAULT_USER_ID_LONG;
//	private String xid = null;
//
//	private int routingStrategy = BY_XID;
//
//	public static DBRoute getDB1Route() {
//		DBRoute back = new DBRoute();
//
//		back.setXid("db1");
//
//		return back;
//	}
//
//	public static DBRoute getDB2Route() {
//		DBRoute back = new DBRoute();
//
//		back.setXid("db2");
//
//		return back;
//	}
//
//	public static DBRoute getDBCRoute() {
//		DBRoute back = new DBRoute();
//
//		back.setXid("dbc");
//
//		return back;
//	}
//
//	public static DBRoute getDBHRoute() {
//		DBRoute back = new DBRoute();
//
//		back.setXid("dbh");
//
//		return back;
//	}
//	
//	/**
//	 * @deprecated ���ֶ��Ѿ��������벻Ҫʹ�á�
//	 * @param id
//	 * @return
//	 */
//	@Deprecated
//	public static DBRoute getDBRouteByUserId(String id) {
//		DBRoute back = new DBRoute();
//
//		back.setUserId(id);
//		return back;
//	}
//	public static DBRoute getDBRouteByUserId(long id){
//		DBRoute back = new DBRoute();
//		back.setUserId(id);
//		return back;
//	}
//
//	/**
//	 * ����userid��ȡDBRoute("db1" || "db2"), ���Ҹ���userid�ĵ�һ���ַ����ú�xid
//	 * ������SqlMapBaseDAOSupport.getDBRouteManager.getTemplates(dr, sqlid)����ȡ
//	 * 
//	 * @param userid
//	 * @return Ĭ��null
//	 */
//	public static final DBRoute getDBRouteWithXidByUserid(String userid) {
//		if (null == userid || 32 != userid.trim().length()) {
//			return null;
//		}
//		userid = userid.trim();
//		DBRoute dr = DBRoute.getDBRouteByUserId(userid);
//		String db1 = "01234567";
//		String db2 = "89abcdef";
//		if (0 == StringUtil.indexOfAny(userid, db1)) {
//			dr.setXid("db1");
//		} else if (0 == StringUtil.indexOfAny(userid, db2)) {
//			dr.setXid("db2");
//		} else {
//			return null;
//		}
//		return dr;
//	}
//
//	public DBRoute() {
//		super();
//	}
//
//	public DBRoute(String xid) {
//		this.xid = xid;
//	}
//
//	/**
//	 * @return Returns the routingStrategy.
//	 */
//	public int getRoutingStrategy() {
//		return routingStrategy;
//	}
//
//	/**
//	 * @param routingStrategy
//	 *            The routingStrategy to set.
//	 */
//	public void setRoutingStrategy(int routingStrategy) {
//		this.routingStrategy = routingStrategy;
//	}
//
//	/**
//	 * @deprecated ���ֶ��Ѿ��������벻Ҫʹ�á�
//	 * @return Returns the userId.
//	 */
//	@Deprecated
//	public String getUserId() {
//		return userId;
//	}
//
//	public long getUserIdLong(){
//		return userIDL;
//	}
//	/**
//	 * @deprecated ���ֶ��Ѿ��������벻Ҫʹ�á�
//	 * @param userId
//	 *            The userId to set.
//	 */
//	@Deprecated
//	public void setUserId(String userId) {
//		this.userId = userId;
//		routingStrategy = BY_USER;
//	}
//	/**
//	 * @param userId Լ��Ϊ����>=0�ſ�������ʹ��
//	 * @exception RuntimeException ��������ֵ<0��
//	 *            The userId to set.
//	 */
//	public void setUserId(long userId) {
//		if(userId<0){
//			throw new RuntimeException("long ��userID����С��0");
//		}
//		this.userIDL = userId;
//		routingStrategy = BY_USER_LONG;
//	}
//
//	/**
//	 * @return Returns the xid.
//	 */
//	public String getXid() {
//		return xid;
//	}
//
//	/**
//	 * @param xid
//	 *            The xid to set.
//	 */
//	public void setXid(String xid) {
//		this.xid = xid;
//		routingStrategy = BY_XID;
//	}
//
//	public String toString() {
//		String str = "";
//
//		if ((routingStrategy == BY_XID) && (xid != null)) {
//			str += (routingStrategy + xid);
//
//			return encode(str);
//		}
//
//		if ((routingStrategy == BY_USER) && (userId != null)) {
//			str += (routingStrategy + userId);
//
//			return encode(str);
//		}
//		if ((routingStrategy == BY_USER_LONG) && (userIDL !=DEFAULT_USER_ID_LONG)) {
//			str += (routingStrategy + ","+userIDL);
//
//			return str;
//		}
//		return "";
//	}
//
////	public static DBRoute parse(String encodedStr) throws DBRouterException {
////		DBRoute pq = new DBRoute();
////
////		if (encodedStr == null) {
////			return pq;
////		}
////
////		String decodedStr = decode(encodedStr);
////		char rs = decodedStr.charAt(0);
////		String value = decodedStr.substring(1);
////
////		if (((rs - '0') == BY_XID) && (value != null)) {
////			pq.setXid(value);
////
////			return pq;
////		}
////
////		if (((rs - '0') == BY_USER) && (value != null)) {
////			pq.setUserId(value);
////
////			return pq;
////		}
////
////		return pq;
////	}
//
//	private static String encode(String str) {
//		return str;
//	}
//
////	private static String decode(String str) throws DBRouterException {
////		return str;
////	}
//
//	public boolean isEmpty() {
//		return ((xid == null) && (userId == null));
//	}
//}

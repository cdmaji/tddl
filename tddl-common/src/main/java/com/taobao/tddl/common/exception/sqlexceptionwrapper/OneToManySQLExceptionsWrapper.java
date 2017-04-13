/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	//package com.taobao.tddl.common.exception.sqlexceptionwrapper;
//
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * ������һ��SQLexception ����Ϊ�˽�һ�Զ�����Դ�в������쳣����Ǳ��װ��Ȼ���Ϊһ����cause��ɵ�SQLExceptionList
// * ���ظ�ҵ��Ӧ�ã������Ϳ���ͨ���Ƚ�ֱ�۵ķ�ʽ��������������쳣���ػ�ȥ��
// * 
// * @author shenxun
// *
// */
//public class OneToManySQLExceptionsWrapper extends SQLException{
//	/**
//	 * serialVersionUID
//	 */
//	private static final long serialVersionUID = -6919830762776532140L;
//	/**
//	 * 
//	 */
//	private SQLException sqlException = null;
//	public OneToManySQLExceptionsWrapper(List<SQLException> exceptions) {
//		if(exceptions == null){
//			throw new IllegalArgumentException("should not be here");
//		}
//		int size = exceptions.size();
//		if(size == 1 ){
//			sqlException = exceptions.get(0);
//		}else if(size == 0){
//			throw new IllegalArgumentException("should not be here");
//		}else{
//			SQLException  next = null;
//			int index = 0;
//			int sizea = exceptions.size();
//			List<SQLException> sqls = new ArrayList<SQLException>(sizea);
//			for(SQLException sqlex : exceptions){
//				sizea--;
//				sqls.add(exceptions.get(sizea));
//			}
//			for(SQLException sqlex : sqls){
//				
//				OneSqlException sql = new OneSqlException(index, sqlex,next);
//				next = sql;
//				index++;
//			}
//			sqlException = next;
//		}
//	}
//	public String getSQLState() {
//		return sqlException.getSQLState();
//	}
//
//	public int getErrorCode() {
//		return sqlException.getErrorCode();
//	}
//
//	public SQLException getNextException() {
//		return sqlException.getNextException();
//	}
//
//	public void setNextException(SQLException ex) {
//		sqlException.setNextException(ex);
//	}
//	
//
//	public Throwable getCause() {
//			return sqlException.getCause();
//		
//	}
//
//	public String getMessage() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("one to many sql Exception wrapper ").append(" : ").append(sqlException.getMessage());
//		return sb.toString();
//	}
//
//	@Override
//	public StackTraceElement[] getStackTrace() {
//		return super.getStackTrace();
//	}
//}

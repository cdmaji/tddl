/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.jdbc.group.util;

import java.sql.SQLException;
import java.util.regex.Pattern;

import com.taobao.tddl.interact.rule.bean.SqlType;

/**
 *
 * ����SQL��䣬�õ�������������
 *
 * @author yangzhu
 */
public class SQLParser {
//	private static final ParserCache globalCache = ParserCache.instance();
	/**
	 * �����ж��Ƿ���һ��select ... for update��sql
	 */
	private static final Pattern SELECT_FOR_UPDATE_PATTERN = Pattern.compile("^select\\s+.*\\s+for\\s+update.*$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * ���SQL�������
	 *
	 * @param sql
	 *            SQL���
	 * @throws SQLException
	 *             ��SQL��䲻��SELECT��INSERT��UPDATE��DELETE���ʱ���׳��쳣��
	 */
	public static SqlType getSqlType(String sql) throws SQLException {
		//#bug 2011-11-24,modify by junyu,�Ȳ��߻��棬����sql�仯�޴󣬻��滻�뻻��̫�࣬gc̫����
//		SqlType sqlType = globalCache.getSqlType(sql);
//		if (sqlType == null) {
		SqlType sqlType=null;
		//#bug 2011-12-8,modify by junyu ,this code use huge cpu resource,and most 
		//sql have no comment,so first simple look for there whether have the comment
		String noCommentsSql=sql;
		if(sql.contains("/*")){
			noCommentsSql = StringUtils.stripComments(sql, "'\"", "'\"", true, false, true, true).trim();
		}
		
		if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "select")) {
			//#bug 2011-12-9,this select-for-update regex has low performance,so
			//first judge this sql whether have ' for ' string.
			if (noCommentsSql.toLowerCase().contains(" for ")&&SELECT_FOR_UPDATE_PATTERN.matcher(noCommentsSql).matches()) {
				sqlType = SqlType.SELECT_FOR_UPDATE;
			} else {
				sqlType = SqlType.SELECT;
			}
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "show")) {
			sqlType = SqlType.SHOW;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "insert")) {
			sqlType = SqlType.INSERT;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "update")) {
			sqlType = SqlType.UPDATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "delete")) {
			sqlType = SqlType.DELETE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "replace")) {
			sqlType = SqlType.REPLACE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"truncate")) {
			sqlType = SqlType.TRUNCATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "create")) {
			sqlType = SqlType.CREATE;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql, "drop")) {
			sqlType = SqlType.DROP;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"load")) {
			sqlType = SqlType.LOAD;
		} else if (StringUtils.startsWithIgnoreCaseAndWs(noCommentsSql,
				"merge")) {
			sqlType = SqlType.MERGE;
		} else {
			throw new SQLException("only select, insert, update, delete,replace,truncate,create,drop,load,merge sql is supported");
		}
//		sqlType = globalCache.setSqlTypeIfAbsent(sql, sqlType);
//		}
		return sqlType;
	}
}

/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * TDDLר�õ��ַ���������
 * 
 * @author linxuan
 * 
 */
public class TStringUtil {
	/**
	 * ��õ�һ��start��end֮����ִ��� ������start��end��������ֵ������trim
	 */
	public static String getBetween(String sql, String start, String end) {
		int index0 = sql.indexOf(start);
		if (index0 == -1) {
			return null;
		}
		int index1 = sql.indexOf(end, index0);
		if (index1 == -1) {
			return null;
		}
		return sql.substring(index0 + start.length(), index1).trim();
	}

	/**
	 * ֻ��һ���з�
	 * @param str
	 * @param splitor
	 * @return
	 */
	public static String[] twoPartSplit(String str, String splitor) {
		if (splitor != null) {
			int index = str.indexOf(splitor);
			if(index!=-1){
			    String first = str.substring(0, index);
			    String sec = str.substring(index + splitor.length());
		        return new String[]{first,sec};
			}else{
				return new String[] { str };
			}
		} else {
			return new String[] { str };
		}
	}
	
	public static List<String> split(String str,String splitor){
		List<String> re=new ArrayList<String>();
		String[] strs=twoPartSplit(str,splitor);
		if(strs.length==2){
			re.add(strs[0]);
			re.addAll(split(strs[1],splitor));
		}else{
			re.add(strs[0]);
		}
		return re;
	}
	
	public static void main(String[] args){
		String test="sdfsdfsdfs liqiangsdfsdfwerfsdfliqiang woshi whaosdf";
		List<String> strs=split(test,"liqiang");
		for(String str:strs){
			System.out.println(str);
		}
	}
	
	/**
	 * ȥ����һ��start,end֮����ַ���������start,end����
	 * 
	 * @param sql
	 * @param start
	 * @param end
	 * @return
	 */
	public static String removeBetweenWithSplitor(String sql, String start,
			String end) {
		int index0 = sql.indexOf(start);
		if (index0 == -1) {
			return sql;
		}
		int index1 = sql.indexOf(end, index0);
		if (index1 == -1) {
			return sql;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(sql.substring(0, index0));
		sb.append(" ");
		sb.append(sql.substring(index1 + end.length()));
		return sb.toString();
	}

	/**
	 * ������/t/s/n�ȿհ׷�ȫ���滻Ϊ�ո񣬲���ȥ������հ� ���ֲ�ͬʵ�ֵıȽϲ��ԣ��μ���TStringUtilTest
	 */
	public static String fillTabWithSpace(String str) {
		if (str == null) {
			return null;
		}

		str = str.trim();
		int sz = str.length();
		StringBuilder buffer = new StringBuilder(sz);

		int index = 0, index0 = -1, index1 = -1;
		for (int i = 0; i < sz; i++) {
			char c = str.charAt(i);
			if (!Character.isWhitespace(c)) {
				if (index0 != -1) {
					// if (!(index0 == index1 && str.charAt(i - 1) == ' ')) {
					if (index0 != index1 || str.charAt(i - 1) != ' ') {
						buffer.append(str.substring(index, index0)).append(" ");
						index = index1 + 1;
					}
				}
				index0 = index1 = -1;
			} else {
				if (index0 == -1) {
					index0 = index1 = i; // ��һ���հ�
				} else {
					index1 = i;
				}
			}
		}

		buffer.append(str.substring(index));

		return buffer.toString();
	}		/**     * �Ƚ������ַ�������Сд���У���     * <pre>     * StringUtil.equals(null, null)   = true     * StringUtil.equals(null, "abc")  = false     * StringUtil.equals("abc", null)  = false     * StringUtil.equals("abc", "abc") = true     * StringUtil.equals("abc", "ABC") = false     * </pre>     *     * @param str1 Ҫ�Ƚϵ��ַ���1     * @param str2 Ҫ�Ƚϵ��ַ���2     *     * @return ��������ַ�����ͬ�����߶���<code>null</code>���򷵻�<code>true</code>     */    public static boolean equals(String str1, String str2) {        if (str1 == null) {            return str2 == null;        }        return str1.equals(str2);    }	/**     * ȡ��ָ���ָ�����ǰ���γ���֮����Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>���򷵻�<code>null</code>��     * <pre>     * StringUtil.substringBetween(null, *)            = null     * StringUtil.substringBetween("", "")             = ""     * StringUtil.substringBetween("", "tag")          = null     * StringUtil.substringBetween("tagabctag", null)  = null     * StringUtil.substringBetween("tagabctag", "")    = ""     * StringUtil.substringBetween("tagabctag", "tag") = "abc"     * </pre>     * </p>     *     * @param str �ַ���     * @param tag Ҫ�����ķָ��Ӵ�     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>��δ�ҵ��ָ��Ӵ����򷵻�<code>null</code>     */    public static String substringBetween(String str, String tag) {        return substringBetween(str, tag, tag, 0);    }    /**     * ȡ�������ָ���֮����Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>���򷵻�<code>null</code>��     * <pre>     * StringUtil.substringBetween(null, *, *)          = null     * StringUtil.substringBetween("", "", "")          = ""     * StringUtil.substringBetween("", "", "tag")       = null     * StringUtil.substringBetween("", "tag", "tag")    = null     * StringUtil.substringBetween("yabcz", null, null) = null     * StringUtil.substringBetween("yabcz", "", "")     = ""     * StringUtil.substringBetween("yabcz", "y", "z")   = "abc"     * StringUtil.substringBetween("yabczyabcz", "y", "z")   = "abc"     * </pre>     * </p>     *     * @param str �ַ���     * @param open Ҫ�����ķָ��Ӵ�1     * @param close Ҫ�����ķָ��Ӵ�2     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>��δ�ҵ��ָ��Ӵ����򷵻�<code>null</code>     */    public static String substringBetween(String str, String open, String close) {        return substringBetween(str, open, close, 0);    }    /**     * ȡ�������ָ���֮����Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>���򷵻�<code>null</code>��     * <pre>     * StringUtil.substringBetween(null, *, *)          = null     * StringUtil.substringBetween("", "", "")          = ""     * StringUtil.substringBetween("", "", "tag")       = null     * StringUtil.substringBetween("", "tag", "tag")    = null     * StringUtil.substringBetween("yabcz", null, null) = null     * StringUtil.substringBetween("yabcz", "", "")     = ""     * StringUtil.substringBetween("yabcz", "y", "z")   = "abc"     * StringUtil.substringBetween("yabczyabcz", "y", "z")   = "abc"     * </pre>     * </p>     *     * @param str �ַ���     * @param open Ҫ�����ķָ��Ӵ�1     * @param close Ҫ�����ķָ��Ӵ�2     * @param fromIndex ��ָ��index������     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>��δ�ҵ��ָ��Ӵ����򷵻�<code>null</code>     */    public static String substringBetween(String str, String open, String close, int fromIndex) {        if ((str == null) || (open == null) || (close == null)) {            return null;        }        int start = str.indexOf(open, fromIndex);        if (start != -1) {            int end = str.indexOf(close, start + open.length());            if (end != -1) {                return str.substring(start + open.length(), end);            }        }        return null;    }    	 /**     * ȡ�ó���Ϊָ���ַ��������ұߵ��Ӵ���     * <pre>     * StringUtil.right(null, *)    = null     * StringUtil.right(*, -ve)     = ""     * StringUtil.right("", *)      = ""     * StringUtil.right("abc", 0)   = ""     * StringUtil.right("abc", 2)   = "bc"     * StringUtil.right("abc", 4)   = "abc"     * </pre>     *     * @param str �ַ���     * @param len �����Ӵ��ĳ���     *     * @return �Ӵ������ԭʼ�ִ�Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String right(String str, int len) {        if (str == null) {            return null;        }        if (len < 0) {            return EMPTY_STRING;        }        if (str.length() <= len) {            return str;        } else {            return str.substring(str.length() - len);        }    }    	  /**     * ȡ�ó���Ϊָ���ַ���������ߵ��Ӵ���     * <pre>     * StringUtil.left(null, *)    = null     * StringUtil.left(*, -ve)     = ""     * StringUtil.left("", *)      = ""     * StringUtil.left("abc", 0)   = ""     * StringUtil.left("abc", 2)   = "ab"     * StringUtil.left("abc", 4)   = "abc"     * </pre>     *     * @param str �ַ���     * @param len �����Ӵ��ĳ���     *     * @return �Ӵ������ԭʼ�ִ�Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String left(String str, int len) {        if (str == null) {            return null;        }        if (len < 0) {            return EMPTY_STRING;        }        if (str.length() <= len) {            return str;        } else {            return str.substring(0, len);        }    }    	/**     * �ж��ַ����Ƿ�ֻ����unicode���֡�     *      * <p>     * <code>null</code>������<code>false</code>�����ַ���<code>""</code>������<code>true</code>��     * </p>     * <pre>     * StringUtil.isNumeric(null)   = false     * StringUtil.isNumeric("")     = true     * StringUtil.isNumeric("  ")   = false     * StringUtil.isNumeric("123")  = true     * StringUtil.isNumeric("12 3") = false     * StringUtil.isNumeric("ab2c") = false     * StringUtil.isNumeric("12-3") = false     * StringUtil.isNumeric("12.3") = false     * </pre>     *     * @param str Ҫ�����ַ���     *     * @return ����ַ�����<code>null</code>����ȫ��unicode������ɣ��򷵻�<code>true</code>     */    public static boolean isNumeric(String str) {        if (str == null) {            return false;        }        int length = str.length();        for (int i = 0; i < length; i++) {            if (!Character.isDigit(str.charAt(i))) {                return false;            }        }        return true;    }    	  /** ���ַ����� */    public static final String EMPTY_STRING = "";        /**     * ȡ�����һ���ķָ��Ӵ�֮ǰ���Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>��δ�ҵ����Ӵ����򷵻�ԭ�ַ�����     * <pre>     * StringUtil.substringBeforeLast(null, *)      = null     * StringUtil.substringBeforeLast("", *)        = ""     * StringUtil.substringBeforeLast("abcba", "b") = "abc"     * StringUtil.substringBeforeLast("abc", "c")   = "ab"     * StringUtil.substringBeforeLast("a", "a")     = ""     * StringUtil.substringBeforeLast("a", "z")     = "a"     * StringUtil.substringBeforeLast("a", null)    = "a"     * StringUtil.substringBeforeLast("a", "")      = "a"     * </pre>     * </p>     *     * @param str �ַ���     * @param separator Ҫ�����ķָ��Ӵ�     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String substringBeforeLast(String str, String separator) {        if ((str == null) || (separator == null) || (str.length() == 0)                    || (separator.length() == 0)) {            return str;        }        int pos = str.lastIndexOf(separator);        if (pos == -1) {            return str;        }        return str.substring(0, pos);    }    /**     * ȡ�����һ���ķָ��Ӵ�֮����Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>��δ�ҵ����Ӵ����򷵻�ԭ�ַ�����     * <pre>     * StringUtil.substringAfterLast(null, *)      = null     * StringUtil.substringAfterLast("", *)        = ""     * StringUtil.substringAfterLast(*, "")        = ""     * StringUtil.substringAfterLast(*, null)      = ""     * StringUtil.substringAfterLast("abc", "a")   = "bc"     * StringUtil.substringAfterLast("abcba", "b") = "a"     * StringUtil.substringAfterLast("abc", "c")   = ""     * StringUtil.substringAfterLast("a", "a")     = ""     * StringUtil.substringAfterLast("a", "z")     = ""     * </pre>     * </p>     *     * @param str �ַ���     * @param separator Ҫ�����ķָ��Ӵ�     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String substringAfterLast(String str, String separator) {        if ((str == null) || (str.length() == 0)) {            return str;        }        if ((separator == null) || (separator.length() == 0)) {            return EMPTY_STRING;        }        int pos = str.lastIndexOf(separator);        if ((pos == -1) || (pos == (str.length() - separator.length()))) {            return EMPTY_STRING;        }        return str.substring(pos + separator.length());    }        /**     * ȡ�õ�һ�����ֵķָ��Ӵ�֮����Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>��δ�ҵ����Ӵ����򷵻�ԭ�ַ�����     * <pre>     * StringUtil.substringAfter(null, *)      = null     * StringUtil.substringAfter("", *)        = ""     * StringUtil.substringAfter(*, null)      = ""     * StringUtil.substringAfter("abc", "a")   = "bc"     * StringUtil.substringAfter("abcba", "b") = "cba"     * StringUtil.substringAfter("abc", "c")   = ""     * StringUtil.substringAfter("abc", "d")   = ""     * StringUtil.substringAfter("abc", "")    = "abc"     * </pre>     * </p>     *     * @param str �ַ���     * @param separator Ҫ�����ķָ��Ӵ�     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String substringAfter(String str, String separator) {        if ((str == null) || (str.length() == 0)) {            return str;        }        if (separator == null) {            return EMPTY_STRING;        }        int pos = str.indexOf(separator);        if (pos == -1) {            return EMPTY_STRING;        }        return str.substring(pos + separator.length());    }    	/* ============================================================================ */    /*  ������ȡ�Ӵ�������                                                          */    /* ============================================================================ */    /**     * ȡ�õ�һ�����ֵķָ��Ӵ�֮ǰ���Ӵ���     *      * <p>     * ����ַ���Ϊ<code>null</code>���򷵻�<code>null</code>�� ����ָ��Ӵ�Ϊ<code>null</code>��δ�ҵ����Ӵ����򷵻�ԭ�ַ�����     * <pre>     * StringUtil.substringBefore(null, *)      = null     * StringUtil.substringBefore("", *)        = ""     * StringUtil.substringBefore("abc", "a")   = ""     * StringUtil.substringBefore("abcba", "b") = "a"     * StringUtil.substringBefore("abc", "c")   = "ab"     * StringUtil.substringBefore("abc", "d")   = "abc"     * StringUtil.substringBefore("abc", "")    = ""     * StringUtil.substringBefore("abc", null)  = "abc"     * </pre>     * </p>     *     * @param str �ַ���     * @param separator Ҫ�����ķָ��Ӵ�     *     * @return �Ӵ������ԭʼ��Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String substringBefore(String str, String separator) {        if ((str == null) || (separator == null) || (str.length() == 0)) {            return str;        }        if (separator.length() == 0) {            return EMPTY_STRING;        }        int pos = str.indexOf(separator);        if (pos == -1) {            return str;        }        return str.substring(0, pos);    }		  /**     * ����ַ������Ƿ����ָ�����ַ���������ַ���Ϊ<code>null</code>��������<code>false</code>��     * <pre>     * StringUtil.contains(null, *)     = false     * StringUtil.contains(*, null)     = false     * StringUtil.contains("", "")      = true     * StringUtil.contains("abc", "")   = true     * StringUtil.contains("abc", "a")  = true     * StringUtil.contains("abc", "z")  = false     * </pre>     *     * @param str Ҫɨ����ַ���     * @param searchStr Ҫ���ҵ��ַ���     *     * @return ����ҵ����򷵻�<code>true</code>     */    public static boolean contains(String str, String searchStr) {        if ((str == null) || (searchStr == null)) {            return false;        }        return str.indexOf(searchStr) >= 0;    }	 /* ============================================================================ */    /*  ȥ�հף���ָ���ַ����ĺ�����                                                */    /*                                                                              */    /*  ���·���������ȥһ���ִ��еĿհ׻�ָ���ַ���                                */    /* ============================================================================ */    /**     * ��ȥ�ַ���ͷβ���Ŀհף�����ַ�����<code>null</code>����Ȼ����<code>null</code>��     *      * <p>     * ע�⣬��<code>String.trim</code>��ͬ���˷���ʹ��<code>Character.isWhitespace</code>���ж��հף�     * ������Գ�ȥӢ���ַ���֮��������հף������Ŀո�     * <pre>     * StringUtil.trim(null)          = null     * StringUtil.trim("")            = ""     * StringUtil.trim("     ")       = ""     * StringUtil.trim("abc")         = "abc"     * StringUtil.trim("    abc    ") = "abc"     * </pre>     * </p>     *     * @param str Ҫ������ַ���     *     * @return ��ȥ�հ׵��ַ��������ԭ�ִ�Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String trim(String str) {        return trim(str, null, 0);    }    	   /**     * ��ȥ�ַ���ͷβ����ָ���ַ�������ַ�����<code>null</code>����Ȼ����<code>null</code>��     * <pre>     * StringUtil.trim(null, *)          = null     * StringUtil.trim("", *)            = ""     * StringUtil.trim("abc", null)      = "abc"     * StringUtil.trim("  abc", null)    = "abc"     * StringUtil.trim("abc  ", null)    = "abc"     * StringUtil.trim(" abc ", null)    = "abc"     * StringUtil.trim("  abcyx", "xyz") = "  abc"     * </pre>     *     * @param str Ҫ������ַ���     * @param stripChars Ҫ��ȥ���ַ������Ϊ<code>null</code>��ʾ��ȥ�հ��ַ�     * @param mode <code>-1</code>��ʾtrimStart��<code>0</code>��ʾtrimȫ����<code>1</code>��ʾtrimEnd     *     * @return ��ȥָ���ַ���ĵ��ַ��������ԭ�ִ�Ϊ<code>null</code>���򷵻�<code>null</code>     */    private static String trim(String str, String stripChars, int mode) {        if (str == null) {            return null;        }        int length = str.length();        int start = 0;        int end   = length;        // ɨ���ַ���ͷ��        if (mode <= 0) {            if (stripChars == null) {                while ((start < end) && (Character.isWhitespace(str.charAt(start)))) {                    start++;                }            } else if (stripChars.length() == 0) {                return str;            } else {                while ((start < end) && (stripChars.indexOf(str.charAt(start)) != -1)) {                    start++;                }            }        }        // ɨ���ַ���β��        if (mode >= 0) {            if (stripChars == null) {                while ((start < end) && (Character.isWhitespace(str.charAt(end - 1)))) {                    end--;                }            } else if (stripChars.length() == 0) {                return str;            } else {                while ((start < end) && (stripChars.indexOf(str.charAt(end - 1)) != -1)) {                    end--;                }            }        }        if ((start > 0) || (end < length)) {            return str.substring(start, end);        }        return str;    }    	 /**     * ����ַ����Ƿ��ǿհף�<code>null</code>�����ַ���<code>""</code>��ֻ�пհ��ַ���     * <pre>     * StringUtil.isBlank(null)      = false     * StringUtil.isBlank("")        = false     * StringUtil.isBlank(" ")       = false     * StringUtil.isBlank("bob")     = true     * StringUtil.isBlank("  bob  ") = true     * </pre>     *     * @param str Ҫ�����ַ���     *     * @return ���Ϊ�հ�, �򷵻�<code>true</code>     */    public static boolean isNotBlank(String str) {        int length;        if ((str == null) || ((length = str.length()) == 0)) {            return false;        }        for (int i = 0; i < length; i++) {            if (!Character.isWhitespace(str.charAt(i))) {                return true;            }        }        return false;    }		  /**     * ����ַ����Ƿ��ǿհף�<code>null</code>�����ַ���<code>""</code>��ֻ�пհ��ַ���     * <pre>     * StringUtil.isBlank(null)      = true     * StringUtil.isBlank("")        = true     * StringUtil.isBlank(" ")       = true     * StringUtil.isBlank("bob")     = false     * StringUtil.isBlank("  bob  ") = false     * </pre>     *     * @param str Ҫ�����ַ���     *     * @return ���Ϊ�հ�, �򷵻�<code>true</code>     */    public static boolean isBlank(String str) {        int length;        if ((str == null) || ((length = str.length()) == 0)) {            return true;        }        for (int i = 0; i < length; i++) {            if (!Character.isWhitespace(str.charAt(i))) {                return false;            }        }        return true;    }		   /**     * ���ַ�����ָ���ַ��ָ     *      * <p>     * �ָ������������Ŀ�������У������ķָ����ͱ�����һ��������ַ���Ϊ<code>null</code>���򷵻�<code>null</code>��     * <pre>     * StringUtil.split(null, *)                = null     * StringUtil.split("", *)                  = []     * StringUtil.split("abc def", null)        = ["abc", "def"]     * StringUtil.split("abc def", " ")         = ["abc", "def"]     * StringUtil.split("abc  def", " ")        = ["abc", "def"]     * StringUtil.split(" ab:  cd::ef  ", ":")  = ["ab", "cd", "ef"]     * StringUtil.split("abc.def", "")          = ["abc.def"]     *  </pre>     * </p>     *     * @param str Ҫ�ָ���ַ���     * @param separatorChars �ָ���     *     * @return �ָ����ַ������飬���ԭ�ַ���Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String[] splitm(String str, String separatorChars) {        return split(str, separatorChars, -1);    }    public static final String[] EMPTY_STRING_ARRAY = new String[0];        /**     * ���ַ�����ָ���ַ��ָ     *      * <p>     * �ָ������������Ŀ�������У������ķָ����ͱ�����һ��������ַ���Ϊ<code>null</code>���򷵻�<code>null</code>��     * <pre>     * StringUtil.split(null, *, *)                 = null     * StringUtil.split("", *, *)                   = []     * StringUtil.split("ab cd ef", null, 0)        = ["ab", "cd", "ef"]     * StringUtil.split("  ab   cd ef  ", null, 0)  = ["ab", "cd", "ef"]     * StringUtil.split("ab:cd::ef", ":", 0)        = ["ab", "cd", "ef"]     * StringUtil.split("ab:cd:ef", ":", 2)         = ["ab", "cdef"]     * StringUtil.split("abc.def", "", 2)           = ["abc.def"]     * </pre>     * </p>     *     * @param str Ҫ�ָ���ַ���     * @param separatorChars �ָ���     * @param max ���ص�����������������С�ڵ���0�����ʾ������     *     * @return �ָ����ַ������飬���ԭ�ַ���Ϊ<code>null</code>���򷵻�<code>null</code>     */    public static String[] split(String str, String separatorChars, int max) {        if (str == null) {            return null;        }        int length = str.length();        if (length == 0) {            return EMPTY_STRING_ARRAY;        }        List    list      = new ArrayList();        int     sizePlus1 = 1;        int     i         = 0;        int     start     = 0;        boolean match     = false;        if (separatorChars == null) {            // null��ʾʹ�ÿհ���Ϊ�ָ���            while (i < length) {                if (Character.isWhitespace(str.charAt(i))) {                    if (match) {                        if (sizePlus1++ == max) {                            i = length;                        }                        list.add(str.substring(start, i));                        match = false;                    }                    start = ++i;                    continue;                }                match = true;                i++;            }        } else if (separatorChars.length() == 1) {            // �Ż��ָ�������Ϊ1������            char sep = separatorChars.charAt(0);            while (i < length) {                if (str.charAt(i) == sep) {                    if (match) {                        if (sizePlus1++ == max) {                            i = length;                        }                        list.add(str.substring(start, i));                        match = false;                    }                    start = ++i;                    continue;                }                match = true;                i++;            }        } else {            // һ������            while (i < length) {                if (separatorChars.indexOf(str.charAt(i)) >= 0) {                    if (match) {                        if (sizePlus1++ == max) {                            i = length;                        }                        list.add(str.substring(start, i));                        match = false;                    }                    start = ++i;                    continue;                }                match = true;                i++;            }        }        if (match) {            list.add(str.substring(start, i));        }        return (String[]) list.toArray(new String[list.size()]);    }
}

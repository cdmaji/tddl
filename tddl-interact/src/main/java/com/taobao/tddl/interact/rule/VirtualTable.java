/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.interact.rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.interact.rule.bean.AdvancedParameter;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.interact.rule.ruleimpl.EnumerativeRule;
import com.taobao.tddl.interact.rule.virtualnode.DBTableMap;
import com.taobao.tddl.interact.rule.virtualnode.TableSlotMap;

/**
 * һ���߼��������ֿ�ֱ�
 * 
 * @author linxuan
 * 
 */
public class VirtualTable implements VirtualTableRule<String, String> {
	private static Log logger = LogFactory.getLog(VirtualTable.class);

	protected String virtualTbName;
	private Map<String, Set<String>> actualTopology;
	//private String[] dbIndexes; //����У����������Ŀ��Ƿ���Ԥ��

	protected String dbNamePattern; //item_{0000}_dbkey
	protected String tbNamePattern; //item_{0000}

	protected List<Rule<String>> dbShardRules;
	protected List<Rule<String>> tbShardRules;
	
	//add by junyu:֧���Զ��庯��package
    protected static String extraPackagesStr;
    
	private Object outerContext;

	private DBType dbType = null; //Oracle|MySql
	private boolean allowReverseOutput; //�Ƿ����������
	private boolean allowFullTableScan; //�Ƿ�����ȫ��ɨ��
	private boolean disableFullTableScan = true; // �Ƿ�ر�ȫ��ɨ��
	
	/**
	 * ����ڵ�ӳ��
	 */
	protected TableSlotMap tableSlotMap;
	protected DBTableMap dbTableMap;
	protected String tableSlotKeyFormat;

	/**
	 * �Ƿ���Ҫ�и��ơ���TDataSource�����и��ƣ����ڳ�ʼ��ʱ����ֵ����Ϊtrue
	 */
	private boolean needRowCopy = false;

	/**
	 * �����и����У���TDataSource�����и��ƣ����ʼ��ʱ����ݸ�����������������ԡ�
	 * 
	 * Ψһ���������ȼ��ġ����ȼ��ߵģ�����List��ǰ�� ÿ�������Ψһ����֧�ֶ�����������������ĸ�����ǣ�����������������Ŀǰ�����Ƕ��Ψһ��
	 * ����List�е�ÿ��Set����ֻ����һ��Ԫ�ء���ÿ������һ��Ψһ��
	 * 
	 * ��һ��SQL����ʱ�������ȼ�������list����һ����sql�ж������ļ������ӦΨһ����������Ϊ�������Ψһ�������������������и����в�ѯ����
	 */
	private List<String> uniqueKeys;

	public void init() {
		if (tbShardRules == null || tbShardRules.size() == 0) {
			if (this.tbNamePattern == null) {
				//�����û�У�tbKeyPatternΪ��
				this.tbNamePattern = this.virtualTbName;
			}
		}
	    
		initActualTopology();
	}
	
	protected void initVnodeMap(){
		if(tableSlotMap!=null){
			tableSlotMap.setTableSlotKeyFormat(tableSlotKeyFormat);
			tableSlotMap.setLogicTable(this.virtualTbName);
			tableSlotMap.init();
		}
		
		if(dbTableMap!=null){
			dbTableMap.setTableSlotKeyFormat(tableSlotKeyFormat);
			dbTableMap.setLogicTable(this.virtualTbName);
			dbTableMap.init();
		}
	}

	private static final String tableNameSepInSpring = ",";

	public void initActualTopology() {
		if (actualTopology != null) {
			//�����ŷָ��ı���ת��ΪSet
			for (Map.Entry<String/*��*/, Set<String/*��*/>> e : this.actualTopology.entrySet()) {
				if (e.getValue().size() == 1) { //���ֻ��һ��Ԫ�أ�����Ϊ���ŷָ���
					Set<String> tables = new LinkedHashSet<String>();
					tables.addAll(Arrays.asList(e.getValue().iterator().next().split(tableNameSepInSpring)));
					e.setValue(tables);
				}
			}
			showTopology(false);
			return; //�û���ʽ���õ�����
		}
		actualTopology = new TreeMap<String, Set<String>>();
		
		if ((dbShardRules == null || dbShardRules.size() == 0) && (tbShardRules == null || tbShardRules.size() == 0)) {
			Set<String> tbs = new TreeSet<String>();
			tbs.add(this.tbNamePattern);
			actualTopology.put(this.dbNamePattern, tbs);
		} else if (dbShardRules == null || dbShardRules.size() == 0) { //û�п����
			Set<String> tbs = new TreeSet<String>();
			for (Rule<String> tbRule : tbShardRules) {
				tbs.addAll(vbvRule(tbRule, getEnumerates(tbRule)));
			}
			actualTopology.put(this.dbNamePattern, tbs);
		} else if (tbShardRules == null || tbShardRules.size() == 0) {//û�б����
			Set<String> tbs = new TreeSet<String>();
			tbs.add(this.tbNamePattern);
			for (Rule<String> dbRule : dbShardRules) {
				for (String dbIndex : vbvRule(dbRule, getEnumerates(dbRule))) {
					actualTopology.put(dbIndex, tbs);
				}
			}
		} else { //��������
			for (Rule<String> dbRule : dbShardRules) {
				for (Rule<String> tbRule : tbShardRules) {
					if(this.tableSlotMap!=null&&this.dbTableMap!=null){
					    valuebyvalue(this.actualTopology, dbRule, tbRule,true);
					}else{
						valuebyvalue(this.actualTopology, dbRule, tbRule,false);
					}
				}
			}
		}
		showTopology(true);
	}

	private static Set<String> vbvRule(Rule<String> rule, Samples samples) {
		Set<String> tbs = new TreeSet<String>();
		for (Map<String, Object> sample : samples) {
			tbs.add(rule.eval(sample, null));
		}
		return tbs;
	}

	private Set<String> vbvRule(Rule<String> rule, Map<String, Set<Object>> enumerates) {
		Set<String> tbs = new TreeSet<String>();
		for (Map<String, Object> sample : new Samples(enumerates)) {
			tbs.add(rule.eval(sample, null));
		}
		return tbs;
	}

	private static Map<String, Samples> vbvTrace(Rule<String> rule, Map<String, Set<Object>> enumerates) {
		Map<String, Samples> db2Samples = new TreeMap<String, Samples>();
		Samples dbSamples = new Samples(enumerates);
		for (Map<String, Object> sample : dbSamples) {
			String v = rule.eval(sample, null);
			Samples s = db2Samples.get(v);
			if (s == null) {
				s = new Samples(sample.keySet());
				db2Samples.put(v, s);
			}
			s.addSample(sample);
		}
		return db2Samples;
	}

	private static void valuebyvalue(Map<String, Set<String>> topology, Rule<String> dbRule, Rule<String> tbRule,boolean isVnode) {
		Map<String/*����*/, Set<Object>> dbEnumerates = getEnumerates(dbRule);
		Map<String/*����*/, Set<Object>> tbEnumerates = getEnumerates(tbRule);
		//Samples dbSamples = new Samples(dbEnumerates);
		//Samples tbSamples = new Samples(tbEnumerates);

		Set<AdvancedParameter> params = cast(tbRule.getRuleColumnSet());
		for (AdvancedParameter tbap : params) {
			if (dbEnumerates.containsKey(tbap.key)) {
				//������Ĺ�����������ö��ֵҪ�������п�ö��ֵ��Խ�ķ�Χ= =!
				Set<Object> tbValuesBasedONdbValue = new HashSet<Object>();
				for (Object dbValue : dbEnumerates.get(tbap.key)) {
					tbValuesBasedONdbValue.addAll(tbap.enumerateRange(dbValue));
				}
				dbEnumerates.get(tbap.key).addAll(tbValuesBasedONdbValue);
			} else {
				dbEnumerates.put(tbap.key, tbEnumerates.get(tbap.key));
			}
		}
        
		//������ڵ�Ļ���������ڵ����
        if(isVnode){
        	Samples tabSamples = new Samples(tbEnumerates);
        	Set<String> tbs = new TreeSet<String>();
        	for (Map<String, Object> sample : tabSamples) {
        		String value = tbRule.eval(sample, null);
        		tbs.add(value);
    		}
        	
        	for(String table:tbs){
        		Map<String,Object> sample=new HashMap<String, Object>(1);
    			sample.put(EnumerativeRule.REAL_TABLE_NAME_KEY, table);
    			String db = dbRule.eval(sample,null);
    			if(topology.get(db)==null){
    				Set<String> tabs=new HashSet<String>();
    				tabs.add(table);
    				topology.put(db, tabs);
    			}else{
    				topology.get(db).add(table);
    			}
        	}
        	
        	return;
		}

        //û������ڵ㰴������
		Map<String, Samples> dbs = vbvTrace(dbRule, dbEnumerates);//�����������õ����������ֵ����
		for (Map.Entry<String/*��ֵ*/, Samples> e : dbs.entrySet()) {
			Set<String> tbs = topology.get(e.getKey());
			if (tbs == null) {
				tbs = vbvRule(tbRule, e.getValue());
				topology.put(e.getKey(), tbs);
			} else {
				tbs.addAll(vbvRule(tbRule, e.getValue()));
			}
		}
	}

	/**
	 * ����#id,1,32|512_64#�е����ζ���ı���ֵ��Χ��ö�ٹ�����ÿ���еı���ֵ
	 */
	@SuppressWarnings("rawtypes")
	private static Map<String/*����*/, Set<Object>/*��ֵ*/> getEnumerates(Rule rule) {
		Set<AdvancedParameter> params = cast(rule.getRuleColumnSet());
		Map<String/*����*/, Set<Object>/*��ֵ*/> enumerates = new HashMap<String, Set<Object>>(params.size());
		for (AdvancedParameter ap : params) {
			enumerates.put(ap.key, ap.enumerateRange());
		}
		return enumerates;
	}

	private static final int showColsPerRow = 5;

	private void showTopology(boolean showMap) {
		int crossIndex, endIndex, maxcolsPerRow = showColsPerRow, maxtbnlen = 1, maxdbnlen = 1;
		for (Map.Entry<String, Set<String>> e : this.actualTopology.entrySet()) {
			int colsPerRow = colsPerRow(e.getValue(), showColsPerRow);
			if (colsPerRow > maxcolsPerRow) {
				maxcolsPerRow = colsPerRow;
			}
			if (e.getKey().length() > maxdbnlen) {
				maxdbnlen = e.getKey().length(); //dbIndex��󳤶�
			}
			for (String tbn : e.getValue()) {
				if (tbn.length() > maxtbnlen) {
					maxtbnlen = tbn.length(); //tableName��󳤶�
				}
			}
		}
		crossIndex = maxdbnlen + 1;
		endIndex = crossIndex + (maxtbnlen + 1) * maxcolsPerRow + 1;
		StringBuilder sb = new StringBuilder("The topology of the virtual table " + this.virtualTbName);
		addLine(sb, crossIndex, endIndex);
		for (Map.Entry<String/*��*/, Set<String/*��*/>> e : this.actualTopology.entrySet()) {
			sb.append("\n|");
			sb.append(fillAfter(e.getKey(), maxdbnlen)).append("|");
			int i = 0, n = e.getValue().size();
			for (String tb : e.getValue()) {
				sb.append(fillAfter(tb, maxtbnlen)).append(",");
				i++;
				if (i % maxcolsPerRow == 0 && i < n) {
					sb.append("|\n|").append(fillAfter(" ", maxdbnlen)).append("|");//���к�ѿ������
				}
			}
			if (i % maxcolsPerRow != 0) {
				int taillen = (maxcolsPerRow - (i % maxcolsPerRow)) * (maxtbnlen + 1) + 1;
				sb.append(fillBefore("|", taillen));
			} else {
				sb.append("|");
			}

			addLine(sb, crossIndex, endIndex);
		}
		sb.append("\n");
		logger.warn(sb);

		if (!showMap) {
			return;
		}
		sb = new StringBuilder("\nYou could add below segement as the actualTopology property to ");
		sb.append(this.virtualTbName + "'s TableRule bean in the rule file\n\n");
		sb.append("        <property name=\"actualTopology\">\n");
		sb.append("          <map>\n");
		for (Map.Entry<String/*��*/, Set<String/*��*/>> e : this.actualTopology.entrySet()) {
			sb.append("            <entry key=\"").append(e.getKey()).append("\" value=\"");
			for (String table : e.getValue()) {
				sb.append(table).append(tableNameSepInSpring);
			}
			if (sb.charAt(sb.length() - 1) == tableNameSepInSpring.charAt(0)) {
				sb.deleteCharAt(sb.length() - 1);
			}
			sb.append("\" />\n");
		}
		sb.append("          </map>\n");
		sb.append("        </property>\n");
		logger.warn(sb);
	}

	private int colsPerRow(Collection<String> c, int maxColPerRow) {
		int n = c.size();
		if (n <= maxColPerRow) {
			return n;
		}
		int maxfiti = maxColPerRow; //��������������£������һ�пհ����ٵ��Ǹ�i
		int minblank = maxColPerRow; //��������������£����һ�еĿհ׸���
		for (int i = maxColPerRow; i > 0; i--) {
			int mod = n % i;
			if (mod == 0) {
				if (n / i <= i) {
					return i; //�������ܴ�������
				} else {
					break;
				}
			} else {
				if (i - mod < minblank) {
					minblank = i - mod;
					maxfiti = i;
				}
			}
		}

		return maxfiti;
	}

	private String fillAfter(String str, int len) {
		if (str.length() < len) {
			for (int i = 0, n = len - str.length(); i < n; i++) {
				str = str + " ";
			}
		}
		return str;
	}

	private String fillBefore(String str, int len) {
		if (str.length() < len) {
			for (int i = 0, n = len - str.length(); i < n; i++) {
				str = " " + str;
			}
		}
		return str;
	}

	private void addLine(StringBuilder sb, int crossIndex, int endIndex) {
		sb.append("\n+");
		for (int i = 1; i <= endIndex; i++) {
			if (i == crossIndex || i == endIndex) {
				sb.append("+");
			} else {
				sb.append("-");
			}
		}
	}

	/*
	@Override
	public String mapDbKey(String value) {
		return mapValue(dbKeyPrefix, dbKeySuffix, dbKeyAlignLen, value);
	}

	@Override
	public String mapTbKey(String value) {
		return mapValue(tbKeyPrefix, tbKeySuffix, tbKeyAlignLen, value);
	}
	*/

	public VirtualTable clone() throws CloneNotSupportedException {
		return (VirtualTable) super.clone();
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}

	/**
	 * ���߼���getter/setter
	 */
	public void setDbNamePattern(String dbKeyPattern) {
		this.dbNamePattern = dbKeyPattern;
	}
	
	public String getTbNamePattern() {
		return tbNamePattern;
	}

	public void setTbNamePattern(String tbKeyPattern) {
		this.tbNamePattern = tbKeyPattern;
	}

	public List<Rule<String>> getDbShardRules() {
		return dbShardRules;
	}

	public void setDbShardRules(List<Rule<String>> dbShardRules) {
		this.dbShardRules = dbShardRules;
	}

	public List<Rule<String>> getTbShardRules() {
		return tbShardRules;
	}

	public void setTbShardRules(List<Rule<String>> tbShardRules) {
		this.tbShardRules = tbShardRules;
	}

	public DBType getDbType() {
		return dbType;
	}

	public void setDbType(DBType dbType) {
		this.dbType = dbType;
	}

	public boolean isAllowReverseOutput() {
		return allowReverseOutput;
	}

	public void setAllowReverseOutput(boolean allowReverseOutput) {
		this.allowReverseOutput = allowReverseOutput;
	}

	public boolean isDisableFullTableScan() {
		return disableFullTableScan;
	}

	public void setDisableFullTableScan(boolean disableFullTableScan) {
		this.disableFullTableScan = disableFullTableScan;
	}

	public boolean isNeedRowCopy() {
		return needRowCopy;
	}

	public void setNeedRowCopy(boolean needRowCopy) {
		this.needRowCopy = needRowCopy;
	}

	public List<String> getUniqueKeys() {
		return uniqueKeys;
	}

	public void setUniqueKeys(List<String> uniqueKeys) {
		this.uniqueKeys = uniqueKeys;
	}

	public boolean isAllowFullTableScan() {
		return allowFullTableScan;
	}

	public void setAllowFullTableScan(boolean allowFullTableScan) {
		this.allowFullTableScan = allowFullTableScan;
	}

	public Map<String, Set<String>> getActualTopology() {
		return actualTopology;
	}

	public void setActualTopology(Map<String, Set<String>> actualTopology) {
		this.actualTopology = actualTopology;
	}

	public String getVirtualTbName() {
		return virtualTbName;
	}

	public void setVirtualTbName(String virtualTbName) {
		this.virtualTbName = virtualTbName;
	}
	
	public void setExtraPackagesStr(List<String> extraPackages) {
		StringBuilder ep=new StringBuilder("");
        if(extraPackages!=null){
        	int packNum=extraPackages.size();
			for(int i=0;i<packNum;i++){
				ep.append("import ");
				ep.append(extraPackages.get(i));
			    ep.append(";");
			}
		}
        extraPackagesStr=ep.toString();
	}

	/*public String getVirtualDbName() {
		return virtualDbName;
	}

	public void setVirtualDbName(String virtualDbName) {
		this.virtualDbName = virtualDbName;
	}*/

	public Object getOuterContext() {
		return outerContext;
	}

	public void setOuterContext(Map<Object, Object> outerContext) {
		this.outerContext = outerContext;
	}

	public void setTableSlotMap(TableSlotMap tableSlotMap) {
		this.tableSlotMap = tableSlotMap;
	}

	public void setDbTableMap(DBTableMap dbTableMap) {
		this.dbTableMap = dbTableMap;
	}

	public TableSlotMap getTableSlotMap() {
		return tableSlotMap;
	}

	public DBTableMap getDbTableMap() {
		return dbTableMap;
	}

	public void setTableSlotKeyFormat(String tableSlotKeyFormat) {
		this.tableSlotKeyFormat = tableSlotKeyFormat;
	}
}

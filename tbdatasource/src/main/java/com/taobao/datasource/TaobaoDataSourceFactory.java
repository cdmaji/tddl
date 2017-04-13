/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.datasource;

import org.apache.commons.lang.StringUtils;

import com.taobao.datasource.resource.adapter.jdbc.local.LocalTxDataSource;
import com.taobao.datasource.resource.connectionmanager.CachedConnectionManager;
import com.taobao.datasource.resource.security.SecureIdentityLoginModule;
import com.taobao.datasource.tm.TxManager;

/**����Դ���������࣬�ṩ����Դ�Ĵ���������
 * @author qihao
 *
 */
public class TaobaoDataSourceFactory {
	
	private static final LoginConfigFinder loginConfigFinder = new LoginConfigFinder();
	
	private static final TxManager defaultTransactionManager = TxManager.getInstance();
	
	private static final CachedConnectionManager defaultCachedConnectionManager = new CachedConnectionManager();
	
	public static LocalTxDataSource createLocalTxDataSource(LocalTxDataSourceDO dataSourceDO) throws Exception{
		return TaobaoDataSourceFactory.createLocalTxDataSource(dataSourceDO,null,null);
	}
	
	public static LocalTxDataSource createLocalTxDataSource(LocalTxDataSourceDO dataSourceDO,TxManager transactionManager,CachedConnectionManager cachedConnectionManager) throws Exception{
		if(null==dataSourceDO){
			throw new Exception("dataSource config is Empty!");
		}
		LocalTxDataSource localTxDataSource = new LocalTxDataSource();
		//�������ӻ�������������������ʹ�ø����ģ����ûָ����Ĭ�ϸ�һ��
		if(null!=cachedConnectionManager){
			localTxDataSource.setCachedConnectionManager(cachedConnectionManager);
		}else{
			localTxDataSource.setCachedConnectionManager(defaultCachedConnectionManager);
		}
		//������������������������ʹ�ø����ģ����û����Ĭ�ϸ��Ը�
		if(null!=transactionManager){
			localTxDataSource.setTransactionManager(transactionManager);
		}else{
			localTxDataSource.setTransactionManager(defaultTransactionManager);
		}
		localTxDataSource.setBeanName(dataSourceDO.getJndiName());
		localTxDataSource.setUseJmx(dataSourceDO.isUseJmx());
		localTxDataSource.setBackgroundValidation(dataSourceDO.isBackgroundValidation());
		localTxDataSource.setBackGroundValidationMinutes(dataSourceDO.getBackgroundValidationMinutes());
		localTxDataSource.setBlockingTimeoutMillis(dataSourceDO.getBlockingTimeoutMillis());
		localTxDataSource.setCheckValidConnectionSQL(dataSourceDO.getCheckValidConnectionSQL());
		localTxDataSource.setConnectionProperties(dataSourceDO.getConnectionProperties());
		localTxDataSource.setConnectionURL(dataSourceDO.getConnectionURL());
		localTxDataSource.setDriverClass(dataSourceDO.getDriverClass());
		localTxDataSource.setExceptionSorterClassName(dataSourceDO.getExceptionSorterClassName());
		localTxDataSource.setIdleTimeoutMinutes(dataSourceDO.getIdleTimeoutMinutes());
		localTxDataSource.setMaxSize(dataSourceDO.getMaxPoolSize());
		localTxDataSource.setMinSize(dataSourceDO.getMinPoolSize());
		localTxDataSource.setNewConnectionSQL(dataSourceDO.getNewConnectionSQL());
		localTxDataSource.setNoTxSeparatePools(dataSourceDO.isNoTxSeparatePools());
		localTxDataSource.setPassword(dataSourceDO.getPassword());
		localTxDataSource.setPrefill(dataSourceDO.isPrefill());
		localTxDataSource.setPreparedStatementCacheSize(dataSourceDO.getPreparedStatementCacheSize());
		localTxDataSource.setQueryTimeout(dataSourceDO.getQueryTimeout());
		localTxDataSource.setSharePreparedStatements(dataSourceDO.isSharePreparedStatements());
		localTxDataSource.setTrackStatements(dataSourceDO.getTrackStatements());
		localTxDataSource.setTransactionIsolation(dataSourceDO.getTransactionIsolation());
		localTxDataSource.setTxQueryTimeout(dataSourceDO.isTxQueryTimeout());
		localTxDataSource.setUseFastFail(dataSourceDO.isUseFastFail());
		localTxDataSource.setUserName(dataSourceDO.getUserName());
		localTxDataSource.setValidateOnMatch(dataSourceDO.isValidateOnMatch());
		localTxDataSource.setValidConnectionCheckerClassName(dataSourceDO.getValidConnectionCheckerClassName());
		//���ð�ȫ��
		String securityDomainName = dataSourceDO.getSecurityDomain();
		if (StringUtils.isNotBlank(securityDomainName)) {
			SecureIdentityLoginModule securityDomain = loginConfigFinder
					.get(securityDomainName);
			if (securityDomain != null) {
				localTxDataSource.setSecurityDomain(securityDomain);
			}
		}
		localTxDataSource.setCriteria(dataSourceDO.getCriteria());
		//��ʼ������Դ
		localTxDataSource.init();
		return localTxDataSource;
	}
	
	public static void destroy(LocalTxDataSource localTxDataSource) throws Exception {
		if(null!=localTxDataSource){
			localTxDataSource.destroy();
		}
	}
}

/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.scheduler.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.SpringUtils;

public class CheckReloadJob extends JobDef {
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = AppConstants.getInstance().getBoolean("configurations.database.autoLoad", false);

	@Override
	public void execute(IbisManager ibisManager) {
		if (ibisManager.getIbisContext().isLoadingConfigs()) {
			String msg = "skipping checkReload because one or more configurations are currently loading";
			getMessageKeeper().add(msg, MessageKeeperLevel.INFO);
			log.info(getLogPrefix() + msg);
			return;
		}

		String dataSource = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		List<String> configNames = new ArrayList<String>();
		List<String> configsToReload = new ArrayList<String>();

		FixedQuerySender qs = SpringUtils.createBean(getApplicationContext(), FixedQuerySender.class);
		qs.setDatasourceName(dataSource);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		String booleanValueTrue = qs.getDbmsSupport().getBooleanValue(true);
		String selectQuery = "SELECT VERSION FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG = '"+booleanValueTrue+"' and AUTORELOAD = '"+booleanValueTrue+"'";
		try {
			qs.configure();
			qs.open();
			try (Connection conn = qs.getConnection(); PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
				for (Configuration configuration : ibisManager.getConfigurations()) {
					String configName = configuration.getName();
					configNames.add(configName);
					if ("DatabaseClassLoader".equals(configuration.getClassLoaderType())) {
						stmt.setString(1, configName);
						try (ResultSet rs = stmt.executeQuery()) {
							if (rs.next()) {
								String ibisConfigVersion = rs.getString(1);
								String configVersion = configuration.getVersion(); //DatabaseClassLoader configurations always have a version
								if(StringUtils.isEmpty(configVersion) && configuration.getClassLoader() != null) { //If config hasn't loaded yet, don't skip it!
									log.warn(getLogPrefix()+"skipping autoreload for configuration ["+configName+"] unable to determine [configuration.version]");
								}
								else if (!StringUtils.equalsIgnoreCase(ibisConfigVersion, configVersion)) {
									log.info(getLogPrefix()+"configuration ["+configName+"] with version ["+configVersion+"] will be reloaded with new version ["+ibisConfigVersion+"]");
									configsToReload.add(configName);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			getMessageKeeper().add("error while executing query [" + selectQuery + "] (as part of scheduled job execution)", e);
		} finally {
			qs.close();
		}

		if (!configsToReload.isEmpty()) {
			for (String configToReload : configsToReload) {
				ibisManager.getIbisContext().reload(configToReload);
			}
		}

		if (CONFIG_AUTO_DB_CLASSLOADER) {
			// load new (activated) configs
			List<String> dbConfigNames = null;
			try {
				dbConfigNames = ConfigurationUtils.retrieveConfigNamesFromDatabase(ibisManager.getIbisContext());
			} catch (ConfigurationException e) {
				getMessageKeeper().add("error while retrieving configuration names from database", e);
			}
			if (dbConfigNames != null && !dbConfigNames.isEmpty()) {
				for (String currentDbConfigurationName : dbConfigNames) {
					if (!configNames.contains(currentDbConfigurationName)) {
						ibisManager.getIbisContext().load(currentDbConfigurationName);
					}
				}
				// unload old (deactivated) configurations
				for (String currentConfigurationName : configNames) {
					if (!dbConfigNames.contains(currentConfigurationName) && "DatabaseClassLoader".equals(ibisManager.getConfiguration(currentConfigurationName).getClassLoaderType())) {
						ibisManager.getIbisContext().unload(currentConfigurationName);
					}
				}
			}
		}
	}
}

/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.cli;

import java.io.Console;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;

public abstract class AbstractInitCommand extends Command {
	protected Console console;
	
	public AbstractInitCommand() {
		console = System.console();
	}	
	
	protected ConfigTO createConfigTO(File localDir, SaltedSecretKey masterKey, ConnectionTO connectionTO) throws Exception {
		ConfigTO configTO = new ConfigTO();
		
		configTO.setMachineName(getDefaultMachineName());
		configTO.setMasterKey(masterKey); // can be null

		configTO.setConnection(connectionTO);
		
		return configTO;
	}		

	protected ConnectionTO initPluginWithOptions(OptionSet options, OptionSpec<String> optionPlugin, OptionSpec<String> optionPluginOpts) throws Exception {
		ConnectionTO connectionTO = new ConnectionTO();
		
		String pluginStr = null;
		Map<String, String> pluginSettings = new HashMap<String, String>();

		if (options.has(optionPlugin)) {
			pluginStr = initPlugin(options.valueOf(optionPlugin));
		}
		else {
			pluginStr = askPlugin();
		}
		
		if (options.has(optionPluginOpts)) {
			pluginSettings = initPluginSettings(pluginStr, options.valuesOf(optionPluginOpts));			
		}
		else {
			pluginSettings = askPluginSettings(pluginStr);
		}		
		
		connectionTO.setType(pluginStr);
		connectionTO.setSettings(pluginSettings);
		
		return connectionTO;
	}
	
	protected Map<String, String> initPluginSettings(String pluginStr, List<String> pluginSettingsOptList) throws Exception {		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		
		// Fill settings map
		for (String pluginSettingKeyValue : pluginSettingsOptList) {
			String[] keyValue = pluginSettingKeyValue.split("=", 2);
			
			if (keyValue.length != 2) {
				throw new Exception("Invalid setting: "+pluginSettingKeyValue);
			}
			
			pluginSettings.put(keyValue[0], keyValue[1]);
		}
		
		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists
		Connection connection = plugin.createConnection();
		
		// Check if all mandatory are set
		for (String mandatorySetting : connection.getMandatorySettings()) {
			if (!pluginSettings.containsKey(mandatorySetting)) {
				throw new Exception("Not all mandatory settings are set ("+StringUtil.join(connection.getMandatorySettings(), ", ")+"). Use -Psettingname=.. to set it.");
			}
		}	
				
		connection.init(pluginSettings); // Only to test for exceptions!
		
		return pluginSettings;
	}

	protected String initPlugin(String pluginStr) throws Exception {
		Plugin plugin = Plugins.get(pluginStr);
		
		if (plugin == null) {
			throw new Exception("ERROR: Plugin '"+pluginStr+"' does not exist.");
		}
		
		return pluginStr;
	}

	protected Map<String, String> askPluginSettings(String pluginStr) throws StorageException {
		Map<String, String> pluginSettings = new HashMap<String, String>();

		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists
		Connection connection = plugin.createConnection();
		
		pluginSettings = new HashMap<String, String>();
		
		String[] mandatorySettings = connection.getMandatorySettings();
		String[] optionalSettings = connection.getOptionalSettings();
		
		out.println();
		out.println("Connection details for "+plugin.getName()+" connection:");
		
		for (String settingKey : mandatorySettings) {
			String settingValue = null;
			
			while (settingValue == null) {
				out.print("- "+settingKey+": ");
				settingValue = console.readLine();
				
				if ("".equals(settingValue)) {
					out.println("ERROR: This setting is mandatory.");
					out.println();
					
					settingValue = null;
				}
			}
			
			pluginSettings.put(settingKey, settingValue);			
		}

		for (String settingKey : optionalSettings) {
			out.print("- "+settingKey+" (optional): ");
			String settingValue = console.readLine();
			
			if (!"".equals(settingValue)) {
				pluginSettings.put(settingKey, settingValue);
			}
		}

		connection.init(pluginSettings); // To check for exceptions
		
		return pluginSettings;
	}

	protected String askPlugin() {
		String pluginStr = null;
		
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		String pluginsList = "";
		
		for (int i=0; i<plugins.size(); i++) {
			pluginsList += plugins.get(i).getId();
			if (i < plugins.size()-1) { pluginsList += ", "; }			
		}
		
		while (pluginStr == null) {
			out.println("Choose a storage plugin. Available plugins are: "+pluginsList);
			out.print("Plugin: ");			
			pluginStr = console.readLine();
			
			Plugin plugin = Plugins.get(pluginStr);
			
			if (plugin == null) {
				out.println("ERROR: Plugin '"+pluginStr+"' does not exist.");
				out.println();
				
				pluginStr = null;
			}
		}
		
		return pluginStr;
	}

	protected String getDefaultMachineName() throws UnknownHostException {
		return new String(
			  InetAddress.getLocalHost().getHostName() 
			+ System.getProperty("user.name")
			+ Math.abs(new Random().nextInt())
		).replaceAll("[^a-zA-Z0-9]", "");		
	}
	
}

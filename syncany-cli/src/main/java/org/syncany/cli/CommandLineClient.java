/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import static java.util.Arrays.asList;
import static org.syncany.cli.CommandScope.INITIALIZED_LOCALDIR;
import static org.syncany.cli.CommandScope.UNINITIALIZED_LOCALDIR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.io.IOUtils;
import org.syncany.Client;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.config.LogFormatter;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

/**
 * The command line client implements a typical CLI. It represents the first entry
 * point for the Syncany command line application and can be used to run all of the
 * supported commands. 
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CommandLineClient extends Client {
	private static final String HELP_TEXT_SKEL_RESOURCE = "/help.skel";
	private static final String HELP_TEXT_VAR_VERSION= "%VERSION%";
	private static final String HELP_TEXT_VAR_PLUGINS = "%PLUGINS%";
	private static final String HELP_TEXT_VAR_LOGFORMATS = "%LOGFORMATS%";
	
	private String[] args;	
	private File localDir;
	
	private PrintStream out;
		
	static {
		Logging.init();
		Logging.disableLogging();			
	}
	
	public CommandLineClient(String[] args) {
		this.args = args;		
		this.out = System.out;
	}
	
	public void setOut(OutputStream out) {
		this.out = new PrintStream(out);
	}
	
	public int start() throws Exception {
		try {
			// Define global options
			OptionParser parser = new OptionParser();
			parser.allowsUnrecognizedOptions();
			
			OptionSpec<Void> optionHelp = parser.acceptsAll(asList("h", "help"));
			OptionSpec<File> optionLocalDir = parser.acceptsAll(asList("l", "localdir")).withRequiredArg().ofType(File.class);
			OptionSpec<String> optionLog = parser.acceptsAll(asList("log")).withRequiredArg();
			OptionSpec<Void> optionLogPrint = parser.acceptsAll(asList("print"));		
			OptionSpec<String> optionLogLevel = parser.acceptsAll(asList("loglevel")).withOptionalArg();
			OptionSpec<Void> optionDebug = parser.acceptsAll(asList("D", "debug"));
			
			// Parse global options and operation name
			OptionSet options = parser.parse(args);
			
			// Evaluate options
			// WARNING: Do not re-order unless you know what you are doing!
			initHelpOption(options, optionHelp, options.nonOptionArguments());
			initConfigOption(options, optionLocalDir);
			initLogOption(options, optionLog, optionLogLevel, optionLogPrint, optionDebug);
	
			// Run!
			return runCommand(options, options.nonOptionArguments());
		}
		catch (OptionException e) {
			return showErrorAndExit(e.getMessage());
		}
	}	

	private void initHelpOption(OptionSet options, OptionSpec<Void> optionHelp, List<?> nonOptions) throws IOException {
		if (options.has(optionHelp) || nonOptions.size() == 0) {
			showUsageAndExit();
		}
	}

	private void initLogOption(OptionSet options, OptionSpec<String> optionLog, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionLogPrint, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
		initLogHandlers(options, optionLog, optionLogPrint, optionDebug);		
		initLogLevel(options, optionDebug, optionLogLevel);		
	}

	private void initLogLevel(OptionSet options, OptionSpec<Void> optionDebug, OptionSpec<String> optionLogLevel) {
		Level newLogLevel = null;

		// --debug
		if (options.has(optionDebug)) {
			out.println("debug");
			newLogLevel = Level.ALL;			
		}
		
		// --loglevel=<level>
		else if (options.has(optionLogLevel)) {
			String newLogLevelStr = options.valueOf(optionLogLevel);

			try {
				newLogLevel = Level.parse(newLogLevelStr);
			}
			catch (IllegalArgumentException e) {
				showErrorAndExit("Invalid log level given "+newLogLevelStr+"'");
			}
		}		
		else {
			newLogLevel = Level.INFO;
		}
		
		// Add handler to existing loggers, and future ones
		Logging.setGlobalLogLevel(newLogLevel);	
	}

	private void initLogHandlers(OptionSet options, OptionSpec<String> optionLog, OptionSpec<Void> optionLogPrint, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
		// --log=<file>
		String logFilePattern = null;
				
		if (options.has(optionLog)) {
			if (!"-".equals(options.valueOf(optionLog))) {
				logFilePattern = options.valueOf(optionLog);
			}			
		}
		else if (config != null && config.getLogDir().exists()) {
			logFilePattern = config.getLogDir()+File.separator+new SimpleDateFormat("yyMMdd").format(new Date())+".log";
		}
		
		if (logFilePattern != null) {	
			Handler fileLogHandler = new FileHandler(logFilePattern, true);			
			fileLogHandler.setFormatter(new LogFormatter());
	
			Logging.addGlobalHandler(fileLogHandler);
		}
				
		// --debug, add console handler
		if (options.has(optionDebug) || options.has(optionLogPrint) || (options.has(optionLog) && "-".equals(options.valueOf(optionLog)))) {
			Handler consoleLogHandler = new ConsoleHandler();
			consoleLogHandler.setFormatter(new LogFormatter());
			
			Logging.addGlobalHandler(consoleLogHandler);								
		}		
	}

	private void initConfigOption(OptionSet options, OptionSpec<File> optionLocalDir) throws ConfigException, Exception {
		// Find config or use --config option
		if (options.has(optionLocalDir)) {
			localDir = options.valueOf(optionLocalDir);
		}
		else {
			localDir = ConfigHelper.findLocalDirInPath(new File("."));
		}			
		
		// Load config
		config = ConfigHelper.loadConfig(localDir);
	}					
	
	private int runCommand(OptionSet options, List<?> nonOptions) throws Exception {
		if (nonOptions.size() == 0) {
			showUsageAndExit();
		}
		
		List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
		String operationName = (String) nonOptionsCopy.remove(0); 
		String[] operationArgs = nonOptionsCopy.toArray(new String[nonOptionsCopy.size()]);
		
		// Find command
		Command command = CommandFactory.getInstance(operationName);

		if (command == null) {
			showErrorAndExit("Given command is unknown: "+operationName);			
		}
		
		command.setClient(this);
		command.setOut(out);
		command.setLocalDir(localDir);
		
		// Pre-init operations
		if (command.getRequiredCommandScope() == INITIALIZED_LOCALDIR) { 
			if (config == null) {
				showErrorAndExit("No repository found in path. Use 'init' command to create one.");			
			}			
		}
		else if (command.getRequiredCommandScope() == UNINITIALIZED_LOCALDIR) {
			if (config != null) {
				showErrorAndExit("Repository found in path. Command can only be used outside a repository.");			
			}
		}
		
		// Run!
		int exitCode = command.execute(operationArgs);		
		return exitCode;	
	}
	
	private void showUsageAndExit() throws IOException {
		// Application Version
		String versionStr = Client.getApplicationVersion();
		
		if (!Client.isApplicationRelease()) {
			if (Client.getApplicationRevision() != null && !"".equals(Client.getApplicationRevision())) {
				versionStr += ", rev. "+Client.getApplicationRevision();
			}
			else {
				versionStr += ", no rev.";
			}
		}
				
		// Plugins
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		
		String pluginsStr = StringUtil.join(plugins, ", ", new StringJoinListener<Plugin>() {
			@Override
			public String getString(Plugin plugin) {
				return plugin.getId();
			}			
		});		
		
		// Log formats
		String logCommandFormatsStr = StringUtil.join(LogCommand.getSupportedFormats(), ", ");
		
		// Print help text		
		InputStream helpTextInputStream = CommandLineClient.class.getResourceAsStream(HELP_TEXT_SKEL_RESOURCE);
		
		for (String line : IOUtils.readLines(helpTextInputStream)) {
			line = line.replace(HELP_TEXT_VAR_VERSION, versionStr);
			line = line.replace(HELP_TEXT_VAR_PLUGINS, pluginsStr);
			line = line.replace(HELP_TEXT_VAR_LOGFORMATS, logCommandFormatsStr);
			
			out.println(line.replaceAll("\\s$", ""));			
		}
		
		out.close();		
		System.exit(0);
	}

	private int showErrorAndExit(String errorMessage) {
		out.println("Syncany: "+errorMessage);
		out.println("         Refer to help page using '--help'.");
		out.println();
		
		out.close();		
		System.exit(0);
		
		return 0;
	}
	
}

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
package org.syncany.operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.VectorClock;
import org.syncany.database.mem.MemDatabase;

public class LsRemoteOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LsRemoteOperation.class.getSimpleName());	
	private MemDatabase loadedDatabase;
	private TransferManager loadedTransferManager;
	private Set<String> alreadyDownloadedRemoteDatabases;
	
	public LsRemoteOperation(Config config) {
		this(config, null, null);
	}	
	
	public LsRemoteOperation(Config config, MemDatabase database, TransferManager transferManager) {
		super(config);		
		
		this.loadedDatabase = database;
		this.loadedTransferManager = transferManager;
		this.alreadyDownloadedRemoteDatabases = new HashSet<String>();
	}	
	
	@Override
	public RemoteStatusOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Remote Status' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		MemDatabase database = (loadedDatabase != null) ? loadedDatabase : loadLocalDatabaseFromSQL();		
		
		TransferManager transferManager = (loadedTransferManager != null)
				? loadedTransferManager
				: config.getConnection().createTransferManager();
		
		alreadyDownloadedRemoteDatabases = readAlreadyDownloadedDatabasesListFromFile();
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(database, transferManager, alreadyDownloadedRemoteDatabases);
		
		
		return new RemoteStatusOperationResult(unknownRemoteDatabases);
	}		

	private Set<String> readAlreadyDownloadedDatabasesListFromFile() throws IOException {
		// TODO [low] This is dirty!
		alreadyDownloadedRemoteDatabases.clear();
		
		if (config.getKnownDatabaseListFile().exists()) {
			BufferedReader br = new BufferedReader(new FileReader(config.getKnownDatabaseListFile()));
			
			String line = null;
			while (null != (line = br.readLine())) {
				alreadyDownloadedRemoteDatabases.add(line);
			}
			
			br.close();
		}		
		
		return alreadyDownloadedRemoteDatabases;
	}

	private List<RemoteFile> listUnknownRemoteDatabases(MemDatabase db, TransferManager transferManager, Set<String> alreadyDownloadedRemoteDatabases2) throws StorageException {
		logger.log(Level.INFO, "Retrieving remote database list.");
		
		List<RemoteFile> unknownRemoteDatabasesList = new ArrayList<RemoteFile>();

		Map<String, DatabaseRemoteFile> remoteDatabaseFiles = transferManager.list(DatabaseRemoteFile.class);
		
		// No local database yet
		if (db.getLastDatabaseVersion() == null) {
			return new ArrayList<RemoteFile>(remoteDatabaseFiles.values());
		}
		
		// At least one local database version exists
		else {
			VectorClock knownDatabaseVersions = db.getLastDatabaseVersion().getVectorClock();
			
			for (DatabaseRemoteFile remoteDatabaseFile : remoteDatabaseFiles.values()) {
				String clientName = remoteDatabaseFile.getClientName();
				Long knownClientVersion = knownDatabaseVersions.get(clientName);
						
				if (knownClientVersion != null) {
					if (remoteDatabaseFile.getClientVersion() <= knownClientVersion) {
						logger.log(Level.INFO, "- Remote database {0} is already known. Ignoring.", remoteDatabaseFile.getName());
					}
					else if (alreadyDownloadedRemoteDatabases.contains(remoteDatabaseFile.getName())) {
						logger.log(Level.INFO, "- Remote database {0} is already known (in knowndbs.list). Ignoring.", remoteDatabaseFile.getName());
					}
					else {
						logger.log(Level.INFO, "- Remote database {0} is new.", remoteDatabaseFile.getName());
						unknownRemoteDatabasesList.add(remoteDatabaseFile);
					}
				}
				
				else {
					logger.log(Level.INFO, "- Remote database {0} is new.", remoteDatabaseFile.getName());
					unknownRemoteDatabasesList.add(remoteDatabaseFile);
				}				
			}
			
			return unknownRemoteDatabasesList;			
		}
	}
	
	public class RemoteStatusOperationResult implements OperationResult {
		private List<RemoteFile> unknownRemoteDatabases;
		
		public RemoteStatusOperationResult(List<RemoteFile> unknownRemoteDatabases) {
			this.unknownRemoteDatabases = unknownRemoteDatabases;
		}

		public List<RemoteFile> getUnknownRemoteDatabases() {
			return unknownRemoteDatabases;
		}
	}
}

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
package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.XmlDatabaseSerializer;
import org.syncany.operations.LsRemoteOperation.LsRemoteOperationResult;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult.UpResultCode;
import org.syncany.operations.WatchEvent.WatchEventType;

/**
 * The up operation implements a central part of Syncany's business logic. It analyzes the local
 * folder, deduplicates new or changed files and uploads newly packed multichunks to the remote
 * storage. The up operation is the complement to the {@link DownOperation}.
 * 
 * <p>The general operation flow is as follows:
 * <ol>
 *   <li>Load local database (if not already loaded)</li>
 *   <li>Analyze local directory using the {@link StatusOperation} to determine any changed/new/deleted files</li>
 *   <li>Determine if there are unknown remote databases using the {@link LsRemoteOperation}, and skip the rest if there are</li>
 *   <li>If there are changes, use the {@link Deduper} and {@link Indexer} to create a new {@link DatabaseVersion} 
 *       (including new chunks, multichunks, file contents and file versions).</li>
 *   <li>Upload new multichunks (if any) using a {@link TransferManager}</li>
 *   <li>Save new {@link DatabaseVersion} to a new (delta) {@link MemoryDatabase} and upload it</li>
 *   <li>Add delta database to local database and store it locally</li>
 * </ol>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(UpOperation.class.getSimpleName());

	public static final int MIN_KEEP_DATABASE_VERSIONS = 5;
	public static final int MAX_KEEP_DATABASE_VERSIONS = 15;

	private UpOperationOptions options;
	private TransferManager transferManager;
	private SqlDatabase localDatabase;
	private WatchEventListener watchEventListener;
	
	public UpOperation(Config config) {
		this(config, new UpOperationOptions(), null);
	}
	
	public UpOperation(Config config, WatchEventListener watchEventListener) {
		this(config, new UpOperationOptions(), watchEventListener);
	}

	public UpOperation(Config config, UpOperationOptions options, WatchEventListener watchEventListener) {
		super(config);

		this.watchEventListener = watchEventListener;
		this.options = options;
		this.transferManager = config.getConnection().createTransferManager();
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public UpOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync up' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		UpOperationResult result = new UpOperationResult();
		
		// Find local changes
		StatusOperation statusOperation = new StatusOperation(config, options.getStatusOptions());
		StatusOperationResult statusOperationResult = statusOperation.execute();
		ChangeSet localChanges = statusOperationResult.getChangeSet();
		
		result.getStatusResult().setChangeSet(localChanges);

		if (!localChanges.hasChanges()) {
			logger.log(Level.INFO, "Local database is up-to-date (change set). NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);

			disconnectTransferManager();

			return result;
		}

		// Find remote changes (unless --force is enabled)
		if (!options.forceUploadEnabled()) {
			LsRemoteOperationResult lsRemoteOperationResult = new LsRemoteOperation(config, transferManager).execute();
			List<DatabaseRemoteFile> unknownRemoteDatabases = lsRemoteOperationResult.getUnknownRemoteDatabases();

			if (unknownRemoteDatabases.size() > 0) {
				logger.log(Level.INFO, "There are remote changes. Call 'down' first or use --force you must, Luke!");
				result.setResultCode(UpResultCode.NOK_UNKNOWN_DATABASES);

				disconnectTransferManager();

				return result;
			}
			else {
				logger.log(Level.INFO, "No remote changes, ready to upload.");
			}
		}
		else {
			logger.log(Level.INFO, "Force (--force) is enabled, ignoring potential remote changes.");
		}

		List<File> locallyUpdatedFiles = extractLocallyUpdatedFiles(localChanges);
		localChanges = null; // allow GC to clean up

		// Index
		// produces WatchEventType.INDEXING events
		DatabaseVersion newDatabaseVersion = index(locallyUpdatedFiles);

		if (newDatabaseVersion.getFileHistories().size() == 0) {
			logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);

			disconnectTransferManager();

			return result;
		}		

		// Upload multichunks
		// produces WatchEventType.UPLOADING events
		logger.log(Level.INFO, "Uploading new multichunks ...");
		uploadMultiChunks(newDatabaseVersion);

		// Create delta database
		writeAndUploadDeltaDatabase(newDatabaseVersion);

		// Save local database
		logger.log(Level.INFO, "Adding newest database version " + newDatabaseVersion.getHeader() + " to local database ...");
		
		logger.log(Level.INFO, "Persisting local SQL database (new database version {0}) ...", newDatabaseVersion.getHeader().toString());
		localDatabase.persistDatabaseVersion(newDatabaseVersion);

		if (options.cleanupEnabled()) {
			new CleanupOperation(config).execute(); 
		}
		
		removeUnreferencedData();		
		disconnectTransferManager();

		logger.log(Level.INFO, "Sync up done.");

		// Result
		addNewDatabaseChangesToResultChanges(newDatabaseVersion,result.getChangeSet());
		result.setResultCode(UpResultCode.OK_APPLIED_CHANGES);
		
		return result;
	}

	private void writeAndUploadDeltaDatabase(DatabaseVersion newDatabaseVersion) throws InterruptedException, StorageException, IOException {
		// Clone database version (necessary, because the original must not be touched)
		DatabaseVersion deltaDatabaseVersion = newDatabaseVersion.clone();		
		
		// Add dirty data (if existent)
		addDirtyData(deltaDatabaseVersion);		
		
		// New delta database
		MemoryDatabase deltaDatabase = new MemoryDatabase();
		deltaDatabase.addDatabaseVersion(deltaDatabaseVersion);		
				
		// Save delta database locally
		long newestLocalDatabaseVersion = deltaDatabaseVersion.getVectorClock().getClock(config.getMachineName());
		DatabaseRemoteFile remoteDeltaDatabaseFile = new DatabaseRemoteFile(config.getMachineName(), newestLocalDatabaseVersion);
		File localDeltaDatabaseFile = config.getCache().getDatabaseFile(remoteDeltaDatabaseFile.getName());

		logger.log(Level.INFO, "Saving local delta database, version {0} to file {1} ... ", new Object[] {
				deltaDatabaseVersion.getHeader(), localDeltaDatabaseFile });
		
		saveDeltaDatabase(deltaDatabase, localDeltaDatabaseFile);				

		// Upload delta database
		logger.log(Level.INFO, "- Uploading local delta database file ...");
		uploadLocalDatabase(localDeltaDatabaseFile, remoteDeltaDatabaseFile);
	}

	protected void saveDeltaDatabase(MemoryDatabase db, File localDatabaseFile) throws IOException {	
		logger.log(Level.INFO, "- Saving database to "+localDatabaseFile+" ...");
		
		XmlDatabaseSerializer dao = new XmlDatabaseSerializer(config.getTransformer());
		dao.save(db, localDatabaseFile);		
	}			
	
	private void addDirtyData(DatabaseVersion newDatabaseVersion) {
		Iterator<DatabaseVersion> dirtyDatabaseVersions = localDatabase.getDirtyDatabaseVersions();
		
		if (!dirtyDatabaseVersions.hasNext()) {
			logger.log(Level.INFO, "No DIRTY data found in database (no dirty databases); Nothing to do here.");
		}
		else {
			logger.log(Level.INFO, "Adding DIRTY data to new database version: ");
		
			while (dirtyDatabaseVersions.hasNext()) {
				DatabaseVersion dirtyDatabaseVersion = dirtyDatabaseVersions.next();
				
				logger.log(Level.INFO, "- Adding chunks/multichunks/filecontents from database version "+dirtyDatabaseVersion.getHeader());
				
				for (ChunkEntry chunkEntry : dirtyDatabaseVersion.getChunks()) {
					newDatabaseVersion.addChunk(chunkEntry);
				}
				
				for (MultiChunkEntry multiChunkEntry : dirtyDatabaseVersion.getMultiChunks()) {
					newDatabaseVersion.addMultiChunk(multiChunkEntry);
				}
				
				for (FileContent fileContent : dirtyDatabaseVersion.getFileContents()) {
					newDatabaseVersion.addFileContent(fileContent);
				}			
			}
		}
	}

	private void removeUnreferencedData() {
		logger.log(Level.INFO, "- Removing unreferenced dirty data from database ...");	
		localDatabase.removeDirtyDatabaseVersions();		
	}

	private List<File> extractLocallyUpdatedFiles(ChangeSet localChanges) {
		List<File> locallyUpdatedFiles = new ArrayList<File>();

		for (String relativeFilePath : localChanges.getNewFiles()) {
			locallyUpdatedFiles.add(new File(config.getLocalDir() + File.separator + relativeFilePath));
		}

		for (String relativeFilePath : localChanges.getChangedFiles()) {
			locallyUpdatedFiles.add(new File(config.getLocalDir() + File.separator + relativeFilePath));
		}

		return locallyUpdatedFiles;
	}

	private void addNewDatabaseChangesToResultChanges(DatabaseVersion newDatabaseVersion, ChangeSet resultChanges) {
		for (PartialFileHistory partialFileHistory : newDatabaseVersion.getFileHistories()) {
			FileVersion lastFileVersion = partialFileHistory.getLastVersion();

			switch (lastFileVersion.getStatus()) {
			case NEW:
				resultChanges.getNewFiles().add(lastFileVersion.getPath());
				break;

			case CHANGED:
			case RENAMED:
				resultChanges.getChangedFiles().add(lastFileVersion.getPath());
				break;

			case DELETED:
				resultChanges.getDeletedFiles().add(lastFileVersion.getPath());
				break;
			}
		}
	}

	private void uploadMultiChunks(DatabaseVersion newDatabaseVersion) throws InterruptedException, StorageException {
		Collection<MultiChunkEntry> multiChunksEntries = newDatabaseVersion.getMultiChunks();
		List<MultiChunkId> dirtyMultiChunkIds = localDatabase.getDirtyMultiChunkIds();
		int i = 0;
		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			if (dirtyMultiChunkIds.contains(multiChunkEntry.getId())) {
				logger.log(Level.INFO, "- Ignoring multichunk (from dirty database, already uploaded), " + multiChunkEntry.getId() + " ...");
			}
			else {
				File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId().getRaw());
				MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(multiChunkEntry.getId().getRaw());

				logger.log(Level.INFO, "- Uploading multichunk " + multiChunkEntry.getId() + " from " + localMultiChunkFile + " to "
						+ remoteMultiChunkFile + " ...");
				transferManager.upload(localMultiChunkFile, remoteMultiChunkFile);
				i++;
				if (watchEventListener != null) {
					watchEventListener.update(new WatchEvent(remoteMultiChunkFile.getName(), WatchEventType.UPLOADING, i, multiChunksEntries.size()));
				}

				logger.log(Level.INFO, "  + Removing " + multiChunkEntry.getId() + " locally ...");
				localMultiChunkFile.delete();
			}
		}
	}

	private void uploadLocalDatabase(File localDatabaseFile, DatabaseRemoteFile remoteDatabaseFile) throws InterruptedException, StorageException {
		logger.log(Level.INFO, "- Uploading " + localDatabaseFile + " to " + remoteDatabaseFile + " ...");
		transferManager.upload(localDatabaseFile, remoteDatabaseFile);
	}

	private DatabaseVersion index(List<File> localFiles) throws FileNotFoundException, IOException {
		// Get last vector clock
		DatabaseVersionHeader lastDatabaseVersionHeader = localDatabase.getLastDatabaseVersionHeader();
		VectorClock lastVectorClock = (lastDatabaseVersionHeader != null) ? lastDatabaseVersionHeader.getVectorClock() : new VectorClock();

		// New vector clock
		VectorClock newVectorClock = findNewVectorClock(lastVectorClock);

		// Index
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer(), watchEventListener);
		Indexer indexer = new Indexer(config, deduper);

		DatabaseVersion newDatabaseVersion = indexer.index(localFiles);

		newDatabaseVersion.setVectorClock(newVectorClock);
		newDatabaseVersion.setTimestamp(new Date());
		newDatabaseVersion.setClient(config.getMachineName());

		return newDatabaseVersion;
	}
	
	private VectorClock findNewVectorClock(VectorClock lastVectorClock) {
		VectorClock newVectorClock = lastVectorClock.clone();

		Long lastLocalValue = lastVectorClock.getClock(config.getMachineName());
		Long lastDirtyLocalValue = localDatabase.getMaxDirtyVectorClock(config.getMachineName());

		Long newLocalValue = null;

		if (lastDirtyLocalValue != null) {
			// TODO [medium] Does this lead to problems? C-1 does not exist! Possible problems with DatabaseReconciliator?
			newLocalValue = lastDirtyLocalValue + 1; 
		}
		else {
			if (lastLocalValue != null) {
				newLocalValue = lastLocalValue + 1;
			}
			else {
				newLocalValue = 1L;
			}
		}

		newVectorClock.setClock(config.getMachineName(), newLocalValue);

		return newVectorClock;
	}

	private void disconnectTransferManager() {
		try {
			transferManager.disconnect();
		}
		catch (StorageException e) {
			// Don't care!
		}
	}

	public static class UpOperationOptions implements OperationOptions {
		private StatusOperationOptions statusOptions = new StatusOperationOptions();
		private boolean forceUploadEnabled = false;
		private boolean cleanupEnabled = true;

		public StatusOperationOptions getStatusOptions() {
			return statusOptions;
		}

		public void setStatusOptions(StatusOperationOptions statusOptions) {
			this.statusOptions = statusOptions;
		}

		public boolean forceUploadEnabled() {
			return forceUploadEnabled;
		}

		public void setForceUploadEnabled(boolean forceUploadEnabled) {
			this.forceUploadEnabled = forceUploadEnabled;
		}

		public boolean cleanupEnabled() {
			return cleanupEnabled;
		}

		public void setCleanupEnabled(boolean cleanupEnabled) {
			this.cleanupEnabled = cleanupEnabled;
		}
	}

	public static class UpOperationResult implements OperationResult {
		public enum UpResultCode {
			OK_APPLIED_CHANGES, OK_NO_CHANGES, NOK_UNKNOWN_DATABASES
		};

		private UpResultCode resultCode;
		private StatusOperationResult statusResult = new StatusOperationResult();
		private ChangeSet uploadChangeSet = new ChangeSet();

		public UpResultCode getResultCode() {
			return resultCode;
		}

		public void setResultCode(UpResultCode resultCode) {
			this.resultCode = resultCode;
		}

		public void setStatusResult(StatusOperationResult statusResult) {
			this.statusResult = statusResult;
		}

		public void setUploadChangeSet(ChangeSet uploadChangeSet) {
			this.uploadChangeSet = uploadChangeSet;
		}

		public StatusOperationResult getStatusResult() {
			return statusResult;
		}

		public ChangeSet getChangeSet() {
			return uploadChangeSet;
		}
	}
}

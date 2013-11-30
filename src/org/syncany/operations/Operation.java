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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.database.XmlDatabaseDAO;
import org.syncany.database.dao.DAO;
import org.syncany.database.persistence.ChunkEntity;
import org.syncany.database.persistence.DatabaseVersionEntity;
import org.syncany.database.persistence.DatabaseVersionHeaderEntity;
import org.syncany.database.persistence.FileContentEntity;
import org.syncany.database.persistence.FileVersionEntity;
import org.syncany.database.persistence.IChunkEntry;
import org.syncany.database.persistence.IDatabaseVersion;
import org.syncany.database.persistence.IDatabaseVersionHeader;
import org.syncany.database.persistence.IFileContent;
import org.syncany.database.persistence.IFileVersion;
import org.syncany.database.persistence.IMultiChunkEntry;
import org.syncany.database.persistence.IPartialFileHistory;
import org.syncany.database.persistence.MultiChunkEntity;
import org.syncany.database.persistence.PartialFileHistoryEntity;

/**
 * Operations represent and implement Syncany's business logic. They typically
 * correspond to a command or an action initiated by either a user, or by a 
 * periodic action. 
 * 
 * <p>Each operation might be configured using an operation-specific implementation of
 * the {@link OperationOptions} interface and is run using the {@link #execute()} method.
 * While the input options are optional, it must return a corresponding {@link OperationResult}
 * object.  
 *  
 * @see OperationOptions
 * @see OperationResult
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Operation {
	private static final Logger logger = Logger.getLogger(Operation.class.getSimpleName());
	protected Config config;

	public Operation(Config config) {
		this.config = config;
	}

	/**
	 * Executes the operation synchronously and returns a result when 
	 * the operation exits. Using covariance is recommend, that is OperationFoo should
	 * override execute so as to return a OperationFooResult rather than OperationResult.   
	 *   
	 * @return Returns an operation-specific operation result
	 * @throws Exception If the operation fails
	 */
	public abstract OperationResult execute() throws Exception;

	protected void saveLocalDatabase(Database db, File localDatabaseFile) throws IOException {
		saveLocalDatabase(db, null, null, localDatabaseFile);
	}

	protected void saveLocalDatabase(Database db, DatabaseVersion fromVersion, DatabaseVersion toVersion, File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "- Saving database to " + localDatabaseFile + " ...");

		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		dao.save(db, fromVersion, toVersion, localDatabaseFile);
	}

	// FIXME move to a butter place; SQL-DatabaseDAO
	protected void saveLocalDatabaseToSQL(Database db) throws IOException {
		saveLocalDatabaseToSQL(db, null, null);
	}

	// FIXME move to a butter place
	protected void saveLocalDatabaseToSQL(Database db, IDatabaseVersion fromVersion, IDatabaseVersion toVersion) throws IOException {
		DAO<DatabaseVersionEntity> dao = new DAO<DatabaseVersionEntity>(DatabaseVersionEntity.class);

		for (IDatabaseVersion databaseVersion : db.getDatabaseVersions()) {
			boolean databaseVersionInSaveRange = databaseVersionInRange(databaseVersion, fromVersion, toVersion);

			if (!databaseVersionInSaveRange) {
				continue;
			}

			DatabaseVersionEntity entity = mapIDatabaseVersionToDatabaseVersionEntity(databaseVersion);

			dao.save(entity);
		}
	}

	private DatabaseVersionEntity mapIDatabaseVersionToDatabaseVersionEntity(IDatabaseVersion databaseVersion) {
		if (databaseVersion instanceof DatabaseVersionEntity) {
			return (DatabaseVersionEntity) databaseVersion;
		}
		else {
			DatabaseVersionEntity entity = new DatabaseVersionEntity();
			entity.setHeader(mapIDatabaseVersionHeaderToDatabaseVersionHeaderEntity(databaseVersion.getHeader()));
			entity.addChunks(mapIChunkEntryMapToChunkEntityMap(databaseVersion.getChunks()));

			for (IMultiChunkEntry multiChunk : databaseVersion.getMultiChunks()) {
				entity.addMultiChunk(mapIMultiChunkEntryToMultiChunkEntity(multiChunk));
			}

			for (IFileContent fileContent : databaseVersion.getFileContents()) {
				entity.addFileContent(mapIFileContentToFileContentEntity(fileContent));
			}

			for (IPartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
				entity.addFileHistory(mapIPartialFileHistoryToPartialFileHistoryEntity(fileHistory));
			}

			return entity;
		}
	}

	private PartialFileHistoryEntity mapIPartialFileHistoryToPartialFileHistoryEntity(IPartialFileHistory fileHistory) {
		PartialFileHistoryEntity entity = new PartialFileHistoryEntity();
		entity.setFileId(fileHistory.getFileId());
		for (IFileVersion fileVersion : fileHistory.getFileVersions().values()) {
			entity.addFileVersion(mapIFileVersionToFileVersionEntity(fileVersion));
		}
		return entity;
	}

	private FileVersionEntity mapIFileVersionToFileVersionEntity(IFileVersion fileVersion) {
		FileVersionEntity entity = new FileVersionEntity();
		entity.setChecksum(fileVersion.getChecksum());
		entity.setCreatedBy(fileVersion.getCreatedBy());
		entity.setDosAttributes(fileVersion.getDosAttributes());
		entity.setLastModified(fileVersion.getLastModified());
		entity.setLinkTarget(fileVersion.getLinkTarget());
		entity.setPath(fileVersion.getPath());
		entity.setPosixPermissions(fileVersion.getPosixPermissions());
		entity.setSize(fileVersion.getSize());
		entity.setStatus(fileVersion.getStatus());
		entity.setType(fileVersion.getType());
		entity.setUpdated(fileVersion.getUpdated());
		entity.setVersion(fileVersion.getVersion());
		return entity;
	}

	private FileContentEntity mapIFileContentToFileContentEntity(IFileContent fileContent) {
		FileContentEntity entity = new FileContentEntity();
		entity.setChecksum(fileContent.getChecksum());
		entity.setSize(fileContent.getSize());
		return entity;
	}

	private MultiChunkEntity mapIMultiChunkEntryToMultiChunkEntity(IMultiChunkEntry multiChunk) {
		MultiChunkEntity entity = new MultiChunkEntity();
		entity.setId(multiChunk.getId());
		entity.setChunks(mapChunkEntryIdsToChunkEntities(multiChunk.getChunks()));
		return entity;
	}

	private List<ChunkEntity> mapChunkEntryIdsToChunkEntities(List<ChunkEntryId> chunks) {
		List<ChunkEntity> entities = new ArrayList<ChunkEntity>();
		for (ChunkEntryId chunkEntryId : chunks) {
			entities.add(new ChunkEntity(chunkEntryId.getArray()));
		}
		return entities;
	}

	private List<IChunkEntry> mapIChunkEntryMapToChunkEntityMap(Collection<IChunkEntry> chunks) {
		List<IChunkEntry> chunkEntities = new ArrayList<IChunkEntry>();
		for (IChunkEntry iChunkEntry : chunks) {
			chunkEntities.add(mapIChunkEntryToChunkEntity(iChunkEntry));
		}
		return chunkEntities;
	}

	private ChunkEntity mapIChunkEntryToChunkEntity(IChunkEntry iChunkEntry) {
		if (iChunkEntry instanceof ChunkEntity) {
			return (ChunkEntity) iChunkEntry;
		}

		ChunkEntity entity = new ChunkEntity();
		entity.setChecksum(iChunkEntry.getChecksum());
		entity.setSize(iChunkEntry.getSize());

		return entity;
	}

	private DatabaseVersionHeaderEntity mapIDatabaseVersionHeaderToDatabaseVersionHeaderEntity(IDatabaseVersionHeader header) {
		if (header instanceof DatabaseVersionHeaderEntity) {
			return (DatabaseVersionHeaderEntity) header;
		}
		else {
			DatabaseVersionHeaderEntity entity = new DatabaseVersionHeaderEntity();
			entity.setVectorClock(header.getVectorClock());
			entity.setClient(header.getClient());
			entity.setDate(header.getDate());
			return entity;
		}
	}

	private boolean databaseVersionInRange(IDatabaseVersion databaseVersion, IDatabaseVersion databaseVersionFrom, IDatabaseVersion databaseVersionTo) {
		VectorClock vectorClock = databaseVersion.getVectorClock();
		VectorClock vectorClockRangeFrom = (databaseVersionFrom != null) ? databaseVersionFrom.getVectorClock() : null;
		VectorClock vectorClockRangeTo = (databaseVersionTo != null) ? databaseVersionTo.getVectorClock() : null;

		return vectorClockInRange(vectorClock, vectorClockRangeFrom, vectorClockRangeTo);
	}

	private boolean vectorClockInRange(VectorClock vectorClock, VectorClock vectorClockRangeFrom, VectorClock vectorClockRangeTo) {
		// Determine if: versionFrom < databaseVersion
		boolean greaterOrEqualToVersionFrom = false;

		if (vectorClockRangeFrom == null) {
			greaterOrEqualToVersionFrom = true;
		}
		else {
			VectorClockComparison comparison = VectorClock.compare(vectorClockRangeFrom, vectorClock);

			if (comparison == VectorClockComparison.EQUAL || comparison == VectorClockComparison.SMALLER) {
				greaterOrEqualToVersionFrom = true;
			}
		}

		// Determine if: databaseVersion < versionTo
		boolean lowerOrEqualToVersionTo = false;

		if (vectorClockRangeTo == null) {
			lowerOrEqualToVersionTo = true;
		}
		else {
			VectorClockComparison comparison = VectorClock.compare(vectorClock, vectorClockRangeTo);

			if (comparison == VectorClockComparison.EQUAL || comparison == VectorClockComparison.SMALLER) {
				lowerOrEqualToVersionTo = true;
			}
		}

		return greaterOrEqualToVersionFrom && lowerOrEqualToVersionTo;
	}

	protected Database loadLocalDatabase() throws IOException {
		return loadLocalDatabase(config.getDatabaseFile());
	}

	protected Database loadLocalDatabaseFromSQL() throws IOException {
		Database db = new Database();

		DAO<DatabaseVersionEntity> dao = new DAO<DatabaseVersionEntity>(DatabaseVersionEntity.class);

		List<DatabaseVersionEntity> databaseVersions = dao.getAll();

		List<IDatabaseVersion> databaseVersionsInterims = new ArrayList<IDatabaseVersion>();
		databaseVersionsInterims.addAll(databaseVersions);

		db.addDatabaseVersions(databaseVersionsInterims);

		return db;
	}

	protected Database loadLocalDatabase(File localDatabaseFile) throws IOException {
		Database db = new Database();
		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());

		if (localDatabaseFile.exists()) {
			logger.log(Level.INFO, "- Loading database from " + localDatabaseFile + " ...");
			dao.load(db, localDatabaseFile);
		}
		else {
			logger.log(Level.INFO, "- NOT loading. File does not exist.");
		}

		return db;
	}
}

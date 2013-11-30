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
import org.syncany.database.DatabaseDAO;
import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.database.mem.MemDatabase;
import org.syncany.database.mem.MemDatabaseVersion;
import org.syncany.database.mem.XmlDatabaseDAO;
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.database.sql.SqlChunkEntry;
import org.syncany.database.sql.ChunkIdEntity;
import org.syncany.database.sql.DAO;
import org.syncany.database.sql.SqlDatabaseVersionEntity;
import org.syncany.database.sql.SqlDatabaseVersionHeaderEntity;
import org.syncany.database.sql.SqlFileContentEntity;
import org.syncany.database.sql.SqlFileVersionEntity;
import org.syncany.database.sql.SqlMultiChunkEntry;
import org.syncany.database.sql.SqlPartialFileHistoryEntity;

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

	protected void saveLocalDatabase(MemDatabase db, File localDatabaseFile) throws IOException {
		saveLocalDatabase(db, null, null, localDatabaseFile);
	}

	protected void saveLocalDatabase(MemDatabase db, MemDatabaseVersion fromVersion, MemDatabaseVersion toVersion, File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "- Saving database to " + localDatabaseFile + " ...");

		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		dao.save(db, fromVersion, toVersion, localDatabaseFile);
	}

	// FIXME move to a butter place; SQL-DatabaseDAO
	protected void saveLocalDatabaseToSQL(MemDatabase db) throws IOException {
		saveLocalDatabaseToSQL(db, null, null);
	}

	// FIXME move to a butter place
	protected void saveLocalDatabaseToSQL(MemDatabase db, DatabaseVersion fromVersion, DatabaseVersion toVersion) throws IOException {
		DAO<SqlDatabaseVersionEntity> dao = new DAO<SqlDatabaseVersionEntity>(SqlDatabaseVersionEntity.class);

		for (DatabaseVersion databaseVersion : db.getDatabaseVersions()) {
			boolean databaseVersionInSaveRange = databaseVersionInRange(databaseVersion, fromVersion, toVersion);

			if (!databaseVersionInSaveRange) {
				continue;
			}

			SqlDatabaseVersionEntity entity = mapIDatabaseVersionToDatabaseVersionEntity(databaseVersion);

			dao.save(entity);
		}
	}

	private SqlDatabaseVersionEntity mapIDatabaseVersionToDatabaseVersionEntity(DatabaseVersion databaseVersion) {
		if (databaseVersion instanceof SqlDatabaseVersionEntity) {
			return (SqlDatabaseVersionEntity) databaseVersion;
		}
		else {
			SqlDatabaseVersionEntity entity = new SqlDatabaseVersionEntity();
			entity.setHeader(mapIDatabaseVersionHeaderToDatabaseVersionHeaderEntity(databaseVersion.getHeader()));
			entity.addChunks(mapIChunkEntryMapToChunkEntityMap(databaseVersion.getChunks()));

			for (MultiChunkEntry multiChunk : databaseVersion.getMultiChunks()) {
				entity.addMultiChunk(mapIMultiChunkEntryToMultiChunkEntity(multiChunk));
			}

			for (FileContent fileContent : databaseVersion.getFileContents()) {
				entity.addFileContent(mapIFileContentToFileContentEntity(fileContent));
			}

			for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
				entity.addFileHistory(mapIPartialFileHistoryToPartialFileHistoryEntity(fileHistory));
			}

			return entity;
		}
	}

	private SqlPartialFileHistoryEntity mapIPartialFileHistoryToPartialFileHistoryEntity(PartialFileHistory fileHistory) {
		SqlPartialFileHistoryEntity entity = new SqlPartialFileHistoryEntity();
		entity.setFileId(fileHistory.getFileId());
		for (FileVersion fileVersion : fileHistory.getFileVersions().values()) {
			entity.addFileVersion(mapIFileVersionToFileVersionEntity(fileVersion));
		}
		return entity;
	}

	private SqlFileVersionEntity mapIFileVersionToFileVersionEntity(FileVersion fileVersion) {
		SqlFileVersionEntity entity = new SqlFileVersionEntity();
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

	private SqlFileContentEntity mapIFileContentToFileContentEntity(FileContent fileContent) {
		SqlFileContentEntity entity = new SqlFileContentEntity();
		entity.setChecksum(fileContent.getChecksum());
		entity.setSize(fileContent.getSize());
		return entity;
	}

	private SqlMultiChunkEntry mapIMultiChunkEntryToMultiChunkEntity(MultiChunkEntry multiChunk) {
		SqlMultiChunkEntry entity = new SqlMultiChunkEntry();
		entity.setId(multiChunk.getId());
		entity.setChunks(mapChunkEntryIdsToChunkEntities(multiChunk.getChunks()));
		return entity;
	}

	private List<ChunkIdEntity> mapChunkEntryIdsToChunkEntities(List<ChunkEntryId> chunks) {
		List<ChunkIdEntity> entities = new ArrayList<ChunkIdEntity>();
		for (ChunkEntryId chunkEntryId : chunks) {
			entities.add(new ChunkIdEntity(chunkEntryId.getArray()));
		}
		return entities;
	}

	private List<ChunkEntry> mapIChunkEntryMapToChunkEntityMap(Collection<ChunkEntry> chunks) {
		List<ChunkEntry> chunkEntities = new ArrayList<ChunkEntry>();
		for (ChunkEntry iChunkEntry : chunks) {
			chunkEntities.add(mapIChunkEntryToChunkEntity(iChunkEntry));
		}
		return chunkEntities;
	}

	private SqlChunkEntry mapIChunkEntryToChunkEntity(ChunkEntry iChunkEntry) {
		if (iChunkEntry instanceof SqlChunkEntry) {
			return (SqlChunkEntry) iChunkEntry;
		}

		SqlChunkEntry entity = new SqlChunkEntry();
		entity.setChecksum(iChunkEntry.getChecksum());
		entity.setSize(iChunkEntry.getSize());

		return entity;
	}

	private SqlDatabaseVersionHeaderEntity mapIDatabaseVersionHeaderToDatabaseVersionHeaderEntity(DatabaseVersionHeader header) {
		if (header instanceof SqlDatabaseVersionHeaderEntity) {
			return (SqlDatabaseVersionHeaderEntity) header;
		}
		else {
			SqlDatabaseVersionHeaderEntity entity = new SqlDatabaseVersionHeaderEntity();
			entity.setVectorClock(header.getVectorClock());
			entity.setClient(header.getClient());
			entity.setDate(header.getDate());
			return entity;
		}
	}

	private boolean databaseVersionInRange(DatabaseVersion databaseVersion, DatabaseVersion databaseVersionFrom, DatabaseVersion databaseVersionTo) {
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

	protected MemDatabase loadLocalDatabase() throws IOException {
		return loadLocalDatabase(config.getDatabaseFile());
	}

	protected MemDatabase loadLocalDatabaseFromSQL() throws IOException {
		MemDatabase db = new MemDatabase();

		DAO<SqlDatabaseVersionEntity> dao = new DAO<SqlDatabaseVersionEntity>(SqlDatabaseVersionEntity.class);

		List<SqlDatabaseVersionEntity> databaseVersions = dao.getAll();

		List<DatabaseVersion> databaseVersionsInterims = new ArrayList<DatabaseVersion>();
		databaseVersionsInterims.addAll(databaseVersions);

		db.addDatabaseVersions(databaseVersionsInterims);

		return db;
	}

	protected MemDatabase loadLocalDatabase(File localDatabaseFile) throws IOException {
		MemDatabase db = new MemDatabase();
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

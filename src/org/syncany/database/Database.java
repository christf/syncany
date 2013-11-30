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
package org.syncany.database;

import java.util.Collection;
import java.util.List;

import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.database.mem.MemDatabaseVersion;

/**
 * The database represents the internal file and chunk index of the application. It
 * can be used to reference or load a full local database (local client) or a 
 * remote database (from a delta database file of another clients).
 * 
 * <p>A database consists of a sorted list of {@link MemDatabaseVersion}s, i.e. it is a
 * collection of changes to the local file system. 
 * 
 * <p>For convenience, the class also offers a set of functionality to select objects
 * from the current accumulated database. Examples include {@link #getChunk(byte[]) getChunk()},
 * {@link #getContent(byte[]) getContent()} and {@link #getMultiChunk(byte[]) getMultiChunk()}.
 * 
 * <p>To allow this convenience, a few caches are kept in memory, and updated whenever a
 * database version is added or removed.
 * 
 * @see MemDatabaseVersion
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface Database {
	public DatabaseVersion getLastDatabaseVersion();
	
	public DatabaseVersion getFirstDatabaseVersion();
		
	public List<DatabaseVersion> getDatabaseVersions();

	public DatabaseVersion getDatabaseVersion(VectorClock vectorClock);

	public FileContent getContent(byte[] checksum);
	
	public ChunkEntry getChunk(byte[] checksum);
	
	public MultiChunkEntry getMultiChunk(byte[] id);
	
	/**
     * Get a multichunk that this chunk is contained in.
     */
	public MultiChunkEntry getMultiChunkForChunk(ChunkEntryId chunk);
	
	public PartialFileHistory getFileHistory(String relativeFilePath);
	
	public List<PartialFileHistory> getFileHistories(byte[] fileContentChecksum);
	
	public PartialFileHistory getFileHistory(long fileId);

	public Collection<PartialFileHistory> getFileHistories();
	 
	public Branch getBranch();
	
	public void addDatabaseVersion(DatabaseVersion databaseVersion);
	
	public void addDatabaseVersions(List<DatabaseVersion> databaseVersions);

	public void removeDatabaseVersion(DatabaseVersion databaseVersion);
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.persistence.IChunkEntry;
import org.syncany.database.persistence.IDatabaseVersion;
import org.syncany.database.persistence.IFileContent;
import org.syncany.database.persistence.IFileVersion;
import org.syncany.database.persistence.IFileVersion.FileStatus;
import org.syncany.database.persistence.IMultiChunkEntry;
import org.syncany.database.persistence.IPartialFileHistory;
import org.syncany.util.ByteArray;

/**
 * The database represents the internal file and chunk index of the application. It
 * can be used to reference or load a full local database (local client) or a 
 * remote database (from a delta database file of another clients).
 * 
 * <p>A database consists of a sorted list of {@link DatabaseVersion}s, i.e. it is a
 * collection of changes to the local file system. 
 * 
 * <p>For convenience, the class also offers a set of functionality to select objects
 * from the current accumulated database. Examples include {@link #getChunk(byte[]) getChunk()},
 * {@link #getContent(byte[]) getContent()} and {@link #getMultiChunk(byte[]) getMultiChunk()}.
 * 
 * <p>To allow this convenience, a few caches are kept in memory, and updated whenever a
 * database version is added or removed.
 * 
 * @see DatabaseVersion
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Database {
    private List<IDatabaseVersion> databaseVersions;    
	
    // Caches
    private DatabaseVersion fullDatabaseVersionCache;
    private Map<String, IPartialFileHistory> filenameHistoryCache;
    private Map<VectorClock, IDatabaseVersion> databaseVersionIdCache;
    private Map<ByteArray, List<IPartialFileHistory>> contentChecksumFileHistoriesCache;

    public Database() {
    	databaseVersions = new ArrayList<IDatabaseVersion>();    	
        
    	// Caches
    	fullDatabaseVersionCache = new DatabaseVersion();    	
    	filenameHistoryCache = new HashMap<String, IPartialFileHistory>();
    	databaseVersionIdCache = new HashMap<VectorClock, IDatabaseVersion>();
    	contentChecksumFileHistoriesCache = new HashMap<ByteArray, List<IPartialFileHistory>>();
    }   	
	
	public IDatabaseVersion getLastDatabaseVersion() {
		if (databaseVersions.size() == 0) {
			return null;
		}
		
		return databaseVersions.get(databaseVersions.size()-1);
	}
	
	public IDatabaseVersion getFirstDatabaseVersion() {
		if (databaseVersions.size() == 0) {
			return null;
		}
		
		return databaseVersions.get(0);
	}		
		
	public List<IDatabaseVersion> getDatabaseVersions() {
		return Collections.unmodifiableList(databaseVersions);
	}	

	public IDatabaseVersion getDatabaseVersion(VectorClock vectorClock) {
		return databaseVersionIdCache.get(vectorClock);
	}	

	public IFileContent getContent(byte[] checksum) {
		return (checksum != null) ? fullDatabaseVersionCache.getFileContent(checksum) : null;
	}
	
	public IChunkEntry getChunk(byte[] checksum) {
		return fullDatabaseVersionCache.getChunk(checksum);
	}
	
	public IMultiChunkEntry getMultiChunk(byte[] id) {
		return fullDatabaseVersionCache.getMultiChunk(id);
	}	
	
	/**
     * Get a multichunk that this chunk is contained in.
     */
	public IMultiChunkEntry getMultiChunkForChunk(ChunkEntryId chunk) {
		return fullDatabaseVersionCache.getMultiChunk(chunk);
	}	
	
	public IPartialFileHistory getFileHistory(String relativeFilePath) {
		return filenameHistoryCache.get(relativeFilePath); 
	}
	
	public List<IPartialFileHistory> getFileHistories(byte[] fileContentChecksum) {
		return contentChecksumFileHistoriesCache.get(new ByteArray(fileContentChecksum));
	}	
	
	public IPartialFileHistory getFileHistory(long fileId) {
		return fullDatabaseVersionCache.getFileHistory(fileId); 
	}	

	public Collection<IPartialFileHistory> getFileHistories() {
		return fullDatabaseVersionCache.getFileHistories();		
	}
	
	// TODO [low] Database and branch very closely related. The type hierarchy should reflect that. 
	public Branch getBranch() {
		Branch branch = new Branch();
		
		for (IDatabaseVersion databaseVersion : databaseVersions) {
			branch.add(databaseVersion.getHeader());
		}
		
		return branch;
	}
	
	public void addDatabaseVersion(IDatabaseVersion databaseVersion) {		
		databaseVersions.add(databaseVersion);
		
		// Populate caches
		// WARNING: Do NOT reorder, order important!!
		updateDatabaseVersionIdCache(databaseVersion);
		updateFullDatabaseVersionCache(databaseVersion);
		updateFilenameHistoryCache();
		updateContentChecksumCache();
	} 	
	
	public void addDatabaseVersions(List<IDatabaseVersion> databaseVersions) {		
		for (IDatabaseVersion databaseVersion : databaseVersions) {
			addDatabaseVersion(databaseVersion);
		}
	} 	

	public void removeDatabaseVersion(IDatabaseVersion databaseVersion) {
		databaseVersions.remove(databaseVersion);
		
		// Populate caches
		// WARNING: Do NOT reorder, order important!!
		updateFullDatabaseVersionCache();
		updateDatabaseVersionIdCache();
		updateFilenameHistoryCache();
		updateContentChecksumCache();
	}

	// TODO [medium] Very inefficient. Always updates whole cache
	private void updateContentChecksumCache() {
		contentChecksumFileHistoriesCache.clear();
		
		for (IPartialFileHistory fullFileHistory : fullDatabaseVersionCache.getFileHistories()) {
			byte[] lastVersionChecksum = fullFileHistory.getLastVersion().getChecksum();
			
			if (lastVersionChecksum != null) {
				ByteArray lastVersionChecksumByteArray = new ByteArray(lastVersionChecksum);
				List<IPartialFileHistory> historiesWithVersionsWithSameChecksum = contentChecksumFileHistoriesCache.get(lastVersionChecksumByteArray);
				
				// Create if it does not exist
				if (historiesWithVersionsWithSameChecksum == null) {
					historiesWithVersionsWithSameChecksum = new ArrayList<IPartialFileHistory>();
				}
				
				// Add to cache
				historiesWithVersionsWithSameChecksum.add(fullFileHistory);
				contentChecksumFileHistoriesCache.put(lastVersionChecksumByteArray, historiesWithVersionsWithSameChecksum);
			}
		}
				
	}
	
	/*private void updateContentChecksumCache(DatabaseVersion databaseVersion) {
		int i=1;
		
		for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
			byte[] lastVersionChecksum = fileHistory.getLastVersion().getChecksum();
			
			if (lastVersionChecksum != null) {
				ByteArray lastVersionChecksumByteArray = new ByteArray(lastVersionChecksum);
				List<PartialFileHistory> historiesWithVersionsWithSameChecksum = contentChecksumFileHistoriesCache.get(lastVersionChecksumByteArray);
				
				// Create if it does not exist
				if (historiesWithVersionsWithSameChecksum == null) {
					historiesWithVersionsWithSameChecksum = new ArrayList<PartialFileHistory>();
				}
				
				// TODO [low] Throw out old file histories
				XXXXXXXXX
				
				// Add to cache
				historiesWithVersionsWithSameChecksum.add(fileHistory);
				contentChecksumFileHistoriesCache.put(lastVersionChecksumByteArray, historiesWithVersionsWithSameChecksum);
			}
		}
		
		return; // for breakpoint
	}	*/
		
	private void updateFilenameHistoryCache() {
		// TODO [medium] Performance: This throws away the unchanged entries. It should only update new database version
		filenameHistoryCache.clear(); 
		 
		for (IPartialFileHistory cacheFileHistory : fullDatabaseVersionCache.getFileHistories()) {
			IFileVersion lastVersion = cacheFileHistory.getLastVersion();
			String fileName = lastVersion.getPath();
			
			if (lastVersion.getStatus() != FileStatus.DELETED) {
				filenameHistoryCache.put(fileName, cacheFileHistory);				
			}
		}		
	}
	
	private void updateDatabaseVersionIdCache(IDatabaseVersion newDatabaseVersion) {
		databaseVersionIdCache.put(newDatabaseVersion.getVectorClock(), newDatabaseVersion);
	}
	
	private void updateDatabaseVersionIdCache() {
		databaseVersionIdCache.clear();
		
		for (IDatabaseVersion databaseVersion : databaseVersions) {
			updateDatabaseVersionIdCache(databaseVersion);
		}
	}
	
	private void updateFullDatabaseVersionCache() {
		fullDatabaseVersionCache = new DatabaseVersion();
		
		for (IDatabaseVersion databaseVersion : databaseVersions) {
			updateFullDatabaseVersionCache(databaseVersion);
		}
	}
	
	private void updateFullDatabaseVersionCache(IDatabaseVersion newDatabaseVersion) {
		// Chunks
		for (IChunkEntry sourceChunk : newDatabaseVersion.getChunks()) {
			if (fullDatabaseVersionCache.getChunk(sourceChunk.getChecksum()) == null) {
				fullDatabaseVersionCache.addChunk(sourceChunk);
			}
		}
		
		// Multichunks
		for (IMultiChunkEntry sourceMultiChunk : newDatabaseVersion.getMultiChunks()) {
			if (fullDatabaseVersionCache.getMultiChunk(sourceMultiChunk.getId()) == null) {
				fullDatabaseVersionCache.addMultiChunk(sourceMultiChunk);
			}
		}
		
		// Contents
		for (IFileContent sourceFileContent : newDatabaseVersion.getFileContents()) {
			if (fullDatabaseVersionCache.getFileContent(sourceFileContent.getChecksum()) == null) {
				fullDatabaseVersionCache.addFileContent(sourceFileContent);
			}
		}		
		
		// Histories
		for (IPartialFileHistory sourceFileHistory : newDatabaseVersion.getFileHistories()) {
			IPartialFileHistory targetFileHistory = fullDatabaseVersionCache.getFileHistory(sourceFileHistory.getFileId());
			
			if (targetFileHistory == null) {
				fullDatabaseVersionCache.addFileHistory((IPartialFileHistory) sourceFileHistory.clone());
			}
			else {
				for (IFileVersion sourceFileVersion : sourceFileHistory.getFileVersions().values()) {
					if (targetFileHistory.getFileVersion(sourceFileVersion.getVersion()) == null) {
						targetFileHistory.addFileVersion(sourceFileVersion);
					}
				}
			}
		}		
	}
	
}

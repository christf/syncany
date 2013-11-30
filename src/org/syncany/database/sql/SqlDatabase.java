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
package org.syncany.database.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.database.Branch;
import org.syncany.database.ChunkEntry;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.util.ByteArray;


public class SqlDatabase implements Database {
    public SqlDatabase() {
    	        
    }   	
	
	public DatabaseVersion getLastDatabaseVersion() {
		return null;
	}
	
	public DatabaseVersion getFirstDatabaseVersion() {
		return null;
	}		
		
	public List<DatabaseVersion> getDatabaseVersions() {
		return null;
	}	

	public DatabaseVersion getDatabaseVersion(VectorClock vectorClock) {
		return null;
	}	

	public FileContent getContent(byte[] checksum) {
		return null;
	}
	
	public ChunkEntry getChunk(byte[] checksum) {
		return null;
	}
	
	public MultiChunkEntry getMultiChunk(byte[] id) {
		return null;
	}	
	
	/**
     * Get a multichunk that this chunk is contained in.
     */
	public MultiChunkEntry getMultiChunkForChunk(ChunkEntryId chunk) {
		return null;
	}	
	
	public PartialFileHistory getFileHistory(String relativeFilePath) {
		return null;
	}
	
	public List<PartialFileHistory> getFileHistories(byte[] fileContentChecksum) {
		return null;
	}	
	
	public PartialFileHistory getFileHistory(long fileId) {
		return null;
	}	

	public Collection<PartialFileHistory> getFileHistories() {
		return null;	
	}
	
	// TODO [low] Database and branch very closely related. The type hierarchy should reflect that. 
	public Branch getBranch() {
		return null;
	}
	
	public void addDatabaseVersion(DatabaseVersion databaseVersion) {		
		
	} 	
	
	public void addDatabaseVersions(List<DatabaseVersion> databaseVersions) {		
		
	} 	

	public void removeDatabaseVersion(DatabaseVersion databaseVersion) {
		
	}

}

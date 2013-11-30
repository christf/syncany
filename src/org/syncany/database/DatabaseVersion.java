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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.persistence.IChunkEntry;
import org.syncany.database.persistence.IDatabaseVersion;
import org.syncany.database.persistence.IFileContent;
import org.syncany.database.persistence.IFileVersion;
import org.syncany.database.persistence.IMultiChunkEntry;
import org.syncany.database.persistence.IPartialFileHistory;
import org.syncany.util.ByteArray;

public class DatabaseVersion implements IDatabaseVersion {
    private DatabaseVersionHeader header; 
    
    // Full DB in RAM
    private Map<ByteArray, IChunkEntry> chunks;
    private Map<ByteArray, IMultiChunkEntry> multiChunks;
    private Map<ByteArray, IFileContent> fileContents;
    private Map<Long, IPartialFileHistory> fileHistories;

    // Quick access cache
    private Map<ChunkEntryId, IMultiChunkEntry> chunkMultiChunkCache;    

    public DatabaseVersion() {
    	header = new DatabaseVersionHeader();

        // Full DB in RAM
        chunks = new HashMap<ByteArray, IChunkEntry>();
        multiChunks = new HashMap<ByteArray, IMultiChunkEntry>();
        fileContents = new HashMap<ByteArray, IFileContent>();
        fileHistories = new HashMap<Long, IPartialFileHistory>();          

        // Quick access cache
        chunkMultiChunkCache = new HashMap<ChunkEntryId, IMultiChunkEntry>();
    }
    
	public DatabaseVersionHeader getHeader() {
		return header;
	}

	public Date getTimestamp() {
		return header.getDate();
	}

	public void setTimestamp(Date timestamp) {
		this.header.setDate(timestamp);
	}    
	
	public VectorClock getVectorClock() {
		return header.getVectorClock();
	}

	public void setVectorClock(VectorClock vectorClock) {
		this.header.setVectorClock(vectorClock);
	}
	
	public void setClient(String client) {
		this.header.setClient(client);
	}
	
	public String getClient() {
		return header.getClient();
	}

    // Chunk
    
    public IChunkEntry getChunk(byte[] checksum) {
        return chunks.get(new ByteArray(checksum));
    }    
    
    public void addChunk(IChunkEntry chunk) {
        chunks.put(new ByteArray(chunk.getChecksum()), chunk);        
    }
    
	public void addChunks(List<IChunkEntry> chunks) {
		for (IChunkEntry chunk : chunks) {
			addChunk(chunk);	
		}
	}    
    
    public Collection<IChunkEntry> getChunks() {
        return chunks.values();
    }
    
    // Multichunk    
    
    public void addMultiChunk(IMultiChunkEntry multiChunk) {
        multiChunks.put(new ByteArray(multiChunk.getId()), multiChunk);
        
        // Populate cache
        for (ChunkEntryId chunkChecksum : multiChunk.getChunks()) {
        	chunkMultiChunkCache.put(chunkChecksum, multiChunk);
        }
    }
    
    public IMultiChunkEntry getMultiChunk(byte[] multiChunkId) {
    	return multiChunks.get(new ByteArray(multiChunkId));
    }
    
    /**
     * Get a multichunk that this chunk is contained in.
     */
    public IMultiChunkEntry getMultiChunk(ChunkEntryId chunk) {
    	return chunkMultiChunkCache.get(chunk);
    }
    
    /**
     * Get all multichunks in this database version.
     */
    public Collection<IMultiChunkEntry> getMultiChunks() {
        return multiChunks.values();
    }
	
	// Content

	public IFileContent getFileContent(byte[] checksum) {
		return fileContents.get(new ByteArray(checksum));
	}

	public void addFileContent(IFileContent content) {
		fileContents.put(new ByteArray(content.getChecksum()), content);
	}

	public Collection<IFileContent> getFileContents() {
		return fileContents.values();
	}
	
    // History
    
    public void addFileHistory(IPartialFileHistory history) {
        fileHistories.put(history.getFileId(), history);
    }
    
    public IPartialFileHistory getFileHistory(long fileId) {
        return fileHistories.get(fileId);
    }
        
    public Collection<IPartialFileHistory> getFileHistories() {
        return fileHistories.values();
    }  
    
    public void addFileVersionToHistory(long fileHistoryID, IFileVersion fileVersion) {
    	fileHistories.get(fileHistoryID).addFileVersion(fileVersion);
    }  
    
    @Override
  	public int hashCode() {
  		final int prime = 31;
  		int result = 1;
  		result = prime * result + ((header == null) ? 0 : header.hashCode());
  		return result;
  	}

  	@Override
  	public boolean equals(Object obj) {
  		if (this == obj)
  			return true;
  		if (obj == null)
  			return false;
  		if (getClass() != obj.getClass())
  			return false;
  		DatabaseVersion other = (DatabaseVersion) obj;
  		if (header == null) {
  			if (other.header != null) 
  				return false;
  		} else if (!header.equals(other.header))
  			return false;
  		return true;
  	}

	@Override
	public String toString() {
		return "DatabaseVersion [header=" + header + ", chunks=" + chunks.size() + ", multiChunks=" + multiChunks.size() + ", fileContents=" + fileContents.size()
				+ ", fileHistories=" + fileHistories.size() + "]";
	}

}

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
package org.syncany.database.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.persistence.ChunkEntry.ChunkEntryId;
import org.syncany.util.ByteArray;

public class DatabaseVersion implements IDatabaseVersion {
    private DatabaseVersionHeader header; 
    
    // Full DB in RAM
    private Map<ByteArray, IChunkEntry> chunks;
    private Map<ByteArray, IMultiChunkEntry> multiChunks;
    private Map<ByteArray, IFileContent> fileContents;
    private Map<Long, PartialFileHistory> fileHistories;

    // Quick access cache
    private Map<ChunkEntryId, MultiChunkEntry> chunkMultiChunkCache;    

    public DatabaseVersion() {
    	header = new DatabaseVersionHeader();

        // Full DB in RAM
        chunks = new HashMap<ByteArray, IChunkEntry>();
        multiChunks = new HashMap<ByteArray, IMultiChunkEntry>();
        fileContents = new HashMap<ByteArray, IFileContent>();
        fileHistories = new HashMap<Long, PartialFileHistory>();          

        // Quick access cache
        chunkMultiChunkCache = new HashMap<ChunkEntryId, MultiChunkEntry>();
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
	
	public void setPreviousClient(String previousClient) {
		this.header.setPreviousClient(previousClient);
	}
	
	public String getPreviousClient() {
		return this.header.getPreviousClient();
	}
	
	public VectorClock getPreviousVectorClock() {
		return this.header.getPreviousVectorClock();
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
    
    public Collection<IChunkEntry> getChunks() {
		Collection<? extends IChunkEntry> chunkEntries = chunks.values();
		return Collections.unmodifiableCollection(chunkEntries);   
	}
    
    // Multichunk    
    
    public void addMultiChunk(IMultiChunkEntry multiChunk) {
		multiChunks.put(new ByteArray(multiChunk.getId()), multiChunk);
		
        // Populate cache
        for (ChunkEntryId chunkChecksum : multiChunk.getChunks()) {
        	chunkMultiChunkCache.put(chunkChecksum,  (MultiChunkEntry) multiChunk);
        }
    }
    
    public IMultiChunkEntry getMultiChunk(byte[] multiChunkId) {
    	return multiChunks.get(new ByteArray(multiChunkId));
    }
    
    /**
     * Get a multichunk that this chunk is contained in.
     */
    public MultiChunkEntry getMultiChunk(ChunkEntryId chunk) {
    	return chunkMultiChunkCache.get(chunk);
    }
    
    /**
     * Get all multichunks in this database version.
     */
    public Collection<IMultiChunkEntry> getMultiChunks() {
		Collection<? extends IMultiChunkEntry> multiChunkEntries = multiChunks.values();
		return Collections.unmodifiableCollection(multiChunkEntries);
    }
	
	// Content

	public IFileContent getFileContent(byte[] checksum) {
		return fileContents.get(new ByteArray(checksum));
	}

	public void addFileContent(IFileContent content) {
		fileContents.put(new ByteArray(content.getChecksum()), content);
	}

	public Collection<IFileContent> getFileContents() {
		Collection<? extends IFileContent> fileContentEntries = fileContents.values();
		return Collections.unmodifiableCollection(fileContentEntries);
	}
	
    // History
    
    public void addFileHistory(PartialFileHistory history) {
        fileHistories.put(history.getFileId(), history);
    }
    
    public PartialFileHistory getFileHistory(long fileId) {
        return fileHistories.get(fileId);
    }
        
    public Collection<PartialFileHistory> getFileHistories() {
        return fileHistories.values();
    }  
    
    public void addFileVersionToHistory(long fileHistoryID, FileVersion fileVersion) {
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

	@Override
	public void addChunks(List<IChunkEntry> chunk) {
	
	}

}

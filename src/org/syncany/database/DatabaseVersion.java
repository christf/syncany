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
import java.util.List;

import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;

public interface DatabaseVersion {
  
	public DatabaseVersionHeader getHeader();

	public Date getTimestamp();

	public void setTimestamp(Date timestamp);
	
	public VectorClock getVectorClock();

	public void setVectorClock(VectorClock vectorClock);
	
	public void setClient(String client);
		
	public String getClient();

    public ChunkEntry getChunk(byte[] checksum);
    
    public void addChunk(ChunkEntry chunk);

    public void addChunks(List<ChunkEntry> chunks);

    public Collection<ChunkEntry> getChunks();

    public void addMultiChunk(MultiChunkEntry multiChunk);
    
    public MultiChunkEntry getMultiChunk(byte[] multiChunkId);
    
    public MultiChunkEntry getMultiChunk(ChunkEntryId chunk);
    
    public Collection<MultiChunkEntry> getMultiChunks();
    
	public FileContent getFileContent(byte[] checksum);

	public void addFileContent(FileContent content);

	public Collection<FileContent> getFileContents();
	
    public void addFileHistory(PartialFileHistory history);
    
    public PartialFileHistory getFileHistory(long fileId);
        
    public Collection<PartialFileHistory> getFileHistories();
    
    public void addFileVersionToHistory(long fileHistoryID, FileVersion fileVersion);
 
}

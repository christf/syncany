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
import java.util.Date;
import java.util.List;

import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.VectorClock;

public interface IDatabaseVersion {
  
	public IDatabaseVersionHeader getHeader();

	public Date getTimestamp();

	public void setTimestamp(Date timestamp);
	
	public VectorClock getVectorClock();

	public void setVectorClock(VectorClock vectorClock);
	
	public void setClient(String client);
		
	public String getClient();

    public IChunkEntry getChunk(byte[] checksum);
    
    public void addChunk(IChunkEntry chunk);

    public void addChunks(List<IChunkEntry> chunks);

    public Collection<IChunkEntry> getChunks();

    public void addMultiChunk(IMultiChunkEntry multiChunk);
    
    public IMultiChunkEntry getMultiChunk(byte[] multiChunkId);
    
    public IMultiChunkEntry getMultiChunk(ChunkEntryId chunk);
    
    public Collection<IMultiChunkEntry> getMultiChunks();
    
	public IFileContent getFileContent(byte[] checksum);

	public void addFileContent(IFileContent content);

	public Collection<IFileContent> getFileContents();
	
    public void addFileHistory(IPartialFileHistory history);
    
    public IPartialFileHistory getFileHistory(long fileId);
        
    public Collection<IPartialFileHistory> getFileHistories();
    
    public void addFileVersionToHistory(long fileHistoryID, IFileVersion fileVersion);
 
}

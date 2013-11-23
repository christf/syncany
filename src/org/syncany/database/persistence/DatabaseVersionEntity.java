package org.syncany.database.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.persistence.ChunkEntry.ChunkEntryId;
import org.syncany.util.StringUtil;


@Entity
@Table(name = "DatabaseVersionEntity")
public class DatabaseVersionEntity implements IDatabaseVersion {
		

    @EmbeddedId
    private DatabaseVersionHeaderEntity header;
    
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Map<String, ChunkEntity> chunks;
    
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Map<String, MultiChunkEntity> multiChunks;
     
	public DatabaseVersionEntity() {
		chunks = new HashMap<String, ChunkEntity>();
		multiChunks = new HashMap<String, MultiChunkEntity>();
	}

	/**
	 * @param header the header to set
	 */
	public void setHeader(DatabaseVersionHeaderEntity header) {
		this.header = header;
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
		DatabaseVersionEntity other = (DatabaseVersionEntity) obj;
		if (header == null) {
			if (other.header != null)
				return false;
		} else if (!header.equals(other.header))
			return false;
		return true;
	}

	@Override
	public Date getTimestamp() {
		return header.getDate();
	}

	@Override
	public void setTimestamp(Date timestamp) {
		header.setDate(timestamp);
	}

	@Override
	public VectorClock getVectorClock() {
		return header.getVectorClock();
	}

	@Override
	public void setVectorClock(VectorClock vectorClock) {
		header.setVectorClock(vectorClock);
	}

	@Override
	public void setClient(String client) {
		header.setClient(client);
	}

	@Override
	public VectorClock getPreviousVectorClock() {
		return header.getPreviousVectorClock();
	}

	@Override
	public String getClient() {
		return header.getClient();
	}

	@Override
	public ChunkEntity getChunk(byte[] checksum) {
		return chunks.get(StringUtil.toHex(checksum));
	}

	@Override
	public void addChunk(IChunkEntry chunk) {
		if(chunk instanceof ChunkEntity) {
			chunks.put(StringUtil.toHex(chunk.getChecksum()), (ChunkEntity) chunk);
		} else {
			throw new RuntimeException("Invalid subclass for chunk");
		}
	}
	
	@Override
	public void addChunks(List<IChunkEntry> chunks) {
		for (IChunkEntry chunk : chunks) {
			addChunk(chunk);	
		}
	}

	@Override
	public Collection<IChunkEntry> getChunks() {
		Collection<? extends IChunkEntry> chunkEntries = chunks.values();
		return Collections.unmodifiableCollection(chunkEntries);
	}
	
	/**
	 * @param chunks the chunks to set
	 */
	public void setChunks(Map<String, ChunkEntity> chunks) {
		this.chunks = chunks;
	}

	@Override
	public void addMultiChunk(IMultiChunkEntry multiChunk) {
		if(multiChunk instanceof MultiChunkEntity) {
			multiChunks.put(StringUtil.toHex(multiChunk.getId()), (MultiChunkEntity) multiChunk);
		} else {
			throw new RuntimeException("Invalid subclass for multichunk");
		}	
	}

	@Override
	public MultiChunkEntity getMultiChunk(byte[] multiChunkId) {
		return multiChunks.get(StringUtil.toHex(multiChunkId));
	}

	@Override
	public IMultiChunkEntry getMultiChunk(ChunkEntryId chunk) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<IMultiChunkEntry> getMultiChunks() {
		Collection<? extends IMultiChunkEntry> multiChunkEntries = multiChunks.values();
		return Collections.unmodifiableCollection(multiChunkEntries);
	}

	@Override
	public FileContent getFileContent(byte[] checksum) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFileContent(FileContent content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<FileContent> getFileContents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFileHistory(PartialFileHistory history) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PartialFileHistory getFileHistory(long fileId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<PartialFileHistory> getFileHistories() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFileVersionToHistory(long fileHistoryID, FileVersion fileVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IDatabaseVersionHeader getHeader() {
		return header;
	}
	
}

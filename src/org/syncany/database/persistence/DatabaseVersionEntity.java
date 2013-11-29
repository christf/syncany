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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.syncany.database.VectorClock;
import org.syncany.database.persistence.ChunkEntry.ChunkEntryId;
import org.syncany.util.StringUtil;


@Entity
@Table(name = "DatabaseVersionEntity")
public class DatabaseVersionEntity implements IDatabaseVersion {
		

    @EmbeddedId
    private DatabaseVersionHeaderEntity header;
    
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, ChunkEntity> chunks;
    
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, MultiChunkEntity> multiChunks;
     
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, FileContentEntity> fileContents;
    
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<Long, PartialFileHistoryEntity> fileHistories;
    
	public DatabaseVersionEntity() {
		chunks = new HashMap<String, ChunkEntity>();
		multiChunks = new HashMap<String, MultiChunkEntity>();
		fileContents = new HashMap<String, FileContentEntity>();
		fileHistories = new HashMap<Long, PartialFileHistoryEntity>();
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
		return multiChunks.get(StringUtil.toHex(chunk.getArray()));
	}

	@Override
	public Collection<IMultiChunkEntry> getMultiChunks() {
		Collection<? extends IMultiChunkEntry> multiChunkEntries = multiChunks.values();
		return Collections.unmodifiableCollection(multiChunkEntries);
	}

	@Override
	public IFileContent getFileContent(byte[] checksum) {
		return fileContents.get(StringUtil.toHex(checksum));
	}

	@Override
	public void addFileContent(IFileContent content) {
		if(content instanceof FileContentEntity) {
			fileContents.put(StringUtil.toHex(content.getChecksum()), (FileContentEntity) content);
		} else {
			throw new RuntimeException("Invalid subclass for multichunk");
		}	
	}

	@Override
	public Collection<IFileContent> getFileContents() {
		Collection<? extends IFileContent> fileContentEntries = fileContents.values();
		return Collections.unmodifiableCollection(fileContentEntries);
	}

	@Override
	public IDatabaseVersionHeader getHeader() {
		return header;
	}

	@Override
	public void addFileHistory(IPartialFileHistory history) {
		if(history instanceof PartialFileHistoryEntity) {
			fileHistories.put(history.getFileId(), (PartialFileHistoryEntity)history);     
		} else {
			throw new RuntimeException("Invalid subclass for file version. Implement mapping to Entity.");
		}
	}

	@Override
	public IPartialFileHistory getFileHistory(long fileId) {
		return fileHistories.get(fileId);
	}

	@Override
	public Collection<IPartialFileHistory> getFileHistories() {
		Collection<? extends IPartialFileHistory> fileHistories = this.fileHistories.values();
        return Collections.unmodifiableCollection(fileHistories);
	}

	@Override
	public void addFileVersionToHistory(long fileHistoryID, IFileVersion fileVersion) {
    	fileHistories.get(fileHistoryID).addFileVersion(fileVersion);
	}
	
}

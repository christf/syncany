package org.syncany.database.sql;

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
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.util.StringUtil;


@Entity
@Table(name = "DatabaseVersionEntity")
public class SqlDatabaseVersionEntity implements DatabaseVersion {
		

    @EmbeddedId
    private SqlDatabaseVersionHeaderEntity header;
    
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, SqlChunkEntry> chunks;
    
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, SqlMultiChunkEntry> multiChunks;
     
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, SqlFileContentEntity> fileContents;
    
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<Long, SqlPartialFileHistoryEntity> fileHistories;
    
	public SqlDatabaseVersionEntity() {
		chunks = new HashMap<String, SqlChunkEntry>();
		multiChunks = new HashMap<String, SqlMultiChunkEntry>();
		fileContents = new HashMap<String, SqlFileContentEntity>();
		fileHistories = new HashMap<Long, SqlPartialFileHistoryEntity>();
	}

	/**
	 * @param header the header to set
	 */
	public void setHeader(SqlDatabaseVersionHeaderEntity header) {
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
		SqlDatabaseVersionEntity other = (SqlDatabaseVersionEntity) obj;
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
	public String getClient() {
		return header.getClient();
	}

	@Override
	public SqlChunkEntry getChunk(byte[] checksum) {
		return chunks.get(StringUtil.toHex(checksum));
	}

	@Override
	public void addChunk(ChunkEntry chunk) {
		if(chunk instanceof SqlChunkEntry) {
			chunks.put(StringUtil.toHex(chunk.getChecksum()), (SqlChunkEntry) chunk);
		} else {
			throw new RuntimeException("Invalid subclass for chunk");
		}
	}
	
	@Override
	public void addChunks(List<ChunkEntry> chunks) {
		for (ChunkEntry chunk : chunks) {
			addChunk(chunk);	
		}
	}

	@Override
	public Collection<ChunkEntry> getChunks() {
		Collection<? extends ChunkEntry> chunkEntries = chunks.values();
		return Collections.unmodifiableCollection(chunkEntries);
	}
	
	/**
	 * @param chunks the chunks to set
	 */
	public void setChunks(Map<String, SqlChunkEntry> chunks) {
		this.chunks = chunks;
	}

	@Override
	public void addMultiChunk(MultiChunkEntry multiChunk) {
		if(multiChunk instanceof SqlMultiChunkEntry) {
			multiChunks.put(StringUtil.toHex(multiChunk.getId()), (SqlMultiChunkEntry) multiChunk);
		} else {
			throw new RuntimeException("Invalid subclass for multichunk");
		}	
	}

	@Override
	public SqlMultiChunkEntry getMultiChunk(byte[] multiChunkId) {
		return multiChunks.get(StringUtil.toHex(multiChunkId));
	}

	@Override
	public MultiChunkEntry getMultiChunk(ChunkEntryId chunk) {
		return null;
	}

	@Override
	public Collection<MultiChunkEntry> getMultiChunks() {
		Collection<? extends MultiChunkEntry> multiChunkEntries = multiChunks.values();
		return Collections.unmodifiableCollection(multiChunkEntries);
	}

	@Override
	public FileContent getFileContent(byte[] checksum) {
		return fileContents.get(StringUtil.toHex(checksum));
	}

	@Override
	public void addFileContent(FileContent content) {
		if(content instanceof SqlFileContentEntity) {
			fileContents.put(StringUtil.toHex(content.getChecksum()), (SqlFileContentEntity) content);
		} else {
			throw new RuntimeException("Invalid subclass for FileContentEntity");
		}	
	}

	@Override
	public Collection<FileContent> getFileContents() {
		Collection<? extends FileContent> fileContentEntries = fileContents.values();
		return Collections.unmodifiableCollection(fileContentEntries);
	}

	@Override
	public DatabaseVersionHeader getHeader() {
		return header;
	}

	@Override
	public void addFileHistory(PartialFileHistory history) {
		if(history instanceof SqlPartialFileHistoryEntity) {
			fileHistories.put(history.getFileId(), (SqlPartialFileHistoryEntity)history);     
		} else {
			throw new RuntimeException("Invalid subclass for file version. Implement mapping to Entity.");
		}
	}

	@Override
	public PartialFileHistory getFileHistory(long fileId) {
		return fileHistories.get(fileId);
	}

	@Override
	public Collection<PartialFileHistory> getFileHistories() {
		Collection<? extends PartialFileHistory> fileHistories = this.fileHistories.values();
        return Collections.unmodifiableCollection(fileHistories);
	}

	@Override
	public void addFileVersionToHistory(long fileHistoryID, FileVersion fileVersion) {
    	fileHistories.get(fileHistoryID).addFileVersion(fileVersion);
	}
	
}

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.classic.Lifecycle;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.util.StringUtil;

@Entity
@Table(name = "FileContentEntity")
public class FileContentEntity implements IFileContent, Lifecycle {
	
	@Transient
    private byte[] checksum;
	
	@Id
	@Column(name = "checksumEncoded")
	private String checksumEncoded;
	
	@Column(name = "contentSize")
    private long contentSize;
    
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ChunkEntity> chunks;
    
    public FileContentEntity() {
        this.chunks = new ArrayList<ChunkEntity>();
    }
    
    public FileContentEntity(byte[] checksum, long contentSize) {
        this();
        this.contentSize = contentSize;
        this.checksum = checksum;
        this.checksumEncoded = StringUtil.toHex(checksum);
    }
       
    public void addChunk(ChunkEntryId chunk) {
        chunks.add(new ChunkEntity(chunk.getArray()));        
    }    
    
    public void addChunk(ChunkEntity chunk) {
    	chunks.add(chunk);
    }  

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
        this.checksumEncoded = StringUtil.toHex(checksum);
    }

    public long getSize() {
        return contentSize;
    }

    public void setSize(long contentSize) {
        this.contentSize = contentSize;
    }

    public Collection<ChunkEntryId> getChunks() {
    	List<ChunkEntryId> chunkEntryIds = new ArrayList<ChunkEntryId>();
    	for (ChunkEntity chunk : chunks) {
    		chunkEntryIds.add(new ChunkEntryId(chunk.getChecksum()));
		}
        return Collections.unmodifiableCollection(chunkEntryIds);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(checksum);
		result = prime * result + ((chunks == null) ? 0 : chunks.hashCode());
		result = prime * result + (int) (contentSize ^ (contentSize >>> 32));
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
		FileContentEntity other = (FileContentEntity) obj;
		if (!Arrays.equals(checksum, other.checksum))
			return false;
		if (chunks == null) {
			if (other.chunks != null)
				return false;
		} else if (!chunks.equals(other.chunks))
			return false;
		if (contentSize != other.contentSize)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileContent [checksum=" + StringUtil.toHex(checksum) + ", contentSize=" + contentSize + ", chunks=" + chunks + "]";
	}

	@Override
	public boolean onSave(Session s) throws CallbackException {
		this.checksumEncoded = StringUtil.toHex(checksum);
		return false;
	}

	@Override
	public boolean onUpdate(Session s) throws CallbackException {
		this.checksumEncoded = StringUtil.toHex(checksum);
		return false;
	}

	@Override
	public boolean onDelete(Session s) throws CallbackException {
		return false;
	}

	@Override
	public void onLoad(Session s, Serializable id) {
		this.checksum = StringUtil.fromHex(checksumEncoded);
	}
            
}

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.util.StringUtil;

/**
 *
 * @author lum
 */
@Entity
@Table(name = "MultiChunkEntity")
public class SqlMultiChunkEntry implements MultiChunkEntry, Lifecycle{
	
	@Transient
    private byte[] id;
	
	@Id
	@Column(name = "idEncoded")
    private String idEncoded;
	
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ChunkIdEntity> chunks;
        
	public SqlMultiChunkEntry() {
        this.chunks = new ArrayList<ChunkIdEntity>();
	}

	public SqlMultiChunkEntry(byte[] id) {
        this.chunks = new ArrayList<ChunkIdEntity>();
        this.id = id;
		this.idEncoded = StringUtil.toHex(id);
    }
    
    public void addChunk(ChunkEntryId chunk) {
    	chunks.add(new ChunkIdEntity(chunk.getArray()));
    }    
    
    public void addChunk(ChunkIdEntity chunk) {
    	chunks.add(chunk);
    }  

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
        this.idEncoded = StringUtil.toHex(id);
    }

    public List<ChunkEntryId> getChunks() {
    	List<ChunkEntryId> chunkEntryIds = new ArrayList<ChunkEntryId>();
    	for (ChunkIdEntity chunk : chunks) {
    		chunkEntryIds.add(new ChunkEntryId(chunk.getChecksum()));
		}
        return chunkEntryIds;
    }
    
	/**
	 * @param chunks the chunks to set
	 */
	public void setChunks(List<ChunkIdEntity> chunks) {
		this.chunks = chunks;
	}

	/**
	 * @return the idEncoded
	 */
	public String getIdEncoded() {
		return idEncoded;
	}

	/**
	 * @param idEncoded the idEncoded to set
	 */
	public void setIdEncoded(String idEncoded) {
		this.idEncoded = idEncoded;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chunks == null) ? 0 : chunks.hashCode());
		result = prime * result + Arrays.hashCode(id);
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
		SqlMultiChunkEntry other = (SqlMultiChunkEntry) obj;
		if (chunks == null) {
			if (other.chunks != null)
				return false;
		} else if (!chunks.equals(other.chunks))
			return false;
		if (!Arrays.equals(id, other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MultiChunkEntry [id=" + StringUtil.toHex(id) + ", chunks=" + chunks + "]";
	}

	@Override
	public boolean onSave(Session s) throws CallbackException {
		this.idEncoded = StringUtil.toHex(id);
		return false;
	}

	@Override
	public boolean onUpdate(Session s) throws CallbackException {
		this.idEncoded = StringUtil.toHex(id);
		return false;
	}

	@Override
	public boolean onDelete(Session s) throws CallbackException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLoad(Session s, Serializable id) {
		this.id = StringUtil.fromHex(idEncoded);		
	}

}

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
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.classic.Lifecycle;
import org.syncany.util.StringUtil;

/**
 * @author lum
 *
 */
@Entity
@Table(name = "ChunkIdEntity")
public class ChunkIdEntity implements Lifecycle{
	@Transient
	private byte[] checksum;  
	
	@Id
	@Column(name = "checksumEncoded")
	private String checksumEncoded; 
	
	public ChunkIdEntity() {
		
	}
	
	public ChunkIdEntity(byte[] checksum) {
        this.checksum = checksum;
        this.checksumEncoded = StringUtil.toHex(checksum);
	}
    
    public byte[] getChecksum() {
    	if(this.checksum == null) {
    		this.checksum = StringUtil.fromHex(checksumEncoded);	
    	}
    	
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
        this.checksumEncoded = StringUtil.toHex(checksum);
    }
    
	/**
	 * @return the checksumEncoded
	 */
	public String getChecksumEncoded() {
		return checksumEncoded;
	}

	/**
	 * @param checksumEncoded the checksumEncoded to set
	 */
	public void setChecksumEncoded(String checksumEncoded) {
		this.checksumEncoded = checksumEncoded;
		this.checksum = StringUtil.fromHex(checksumEncoded);	
	}
	
	@Override
	public String toString() {
		return "ChunkEntry [checksum=" + StringUtil.toHex(checksum) + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(checksum);
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
		ChunkIdEntity other = (ChunkIdEntity) obj;
		if (!Arrays.equals(checksum, other.checksum))
			return false;
		return true;
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

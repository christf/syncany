package org.syncany.database;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Table;

@DynamicUpdate
@Table(appliesTo = "CHUNK")
@Entity
public class ChunkEntity implements Serializable{

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="checksum")
    private byte[] checksum;  
	
	@Column(name="size")
    private int size;    
    
    public ChunkEntity() {
    	
    }
    
    public ChunkEntity(byte[] checksum, int size) {
    	this.checksum = checksum;
    	this.size = size;
    }

	/**
	 * @return the checksum
	 */
	public byte[] getChecksum() {
		return checksum;
	}

	/**
	 * @param checksum the checksum to set
	 */
	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}
    
}

package org.syncany.database.persistence;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.syncany.database.VectorClock;

@Embeddable
public class DatabaseVersionHeaderEntity implements Serializable, IDatabaseVersionHeader {

	private static final long serialVersionUID = 1L;
	
    // DB Version and versions of other users (= DB basis)
	@Column(name = "date", nullable = false)
    private Date date;
	
	@Transient
	private VectorClock vectorClock; 

	@Column(name = "vectorclock", nullable = false)
	private String vectorClockEncoded; 
		
	@Column(name = "client", nullable = false)
	private String client;
        
    public DatabaseVersionHeaderEntity() {
    	this.date = new Date();
    	this.vectorClock = new VectorClock();
    	this.vectorClockEncoded = "";
    	this.client = "UnknownMachine";
    }    

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date timestamp) {
		this.date = timestamp;
	}
	
	public VectorClock getVectorClock() {
		if (vectorClock == null) {
			try {
				vectorClock = VectorClock.fromString(vectorClockEncoded);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		return vectorClock;
	}	
	
	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
		this.vectorClockEncoded = vectorClock.toString();
	}
	
	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((client == null) ? 0 : client.hashCode());
		result = prime * result + ((vectorClock == null) ? 0 : vectorClock.hashCode());
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
		DatabaseVersionHeaderEntity other = (DatabaseVersionHeaderEntity) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
			return false;
		if (vectorClock == null) {
			if (other.vectorClock != null)
				return false;
		} else if (!vectorClock.equals(other.vectorClock))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
				
		sb.append(client);
		sb.append("/");
		sb.append(vectorClock.toString());
		sb.append("/T=");
		sb.append(date.getTime());
		
		return sb.toString();
	}
    
}

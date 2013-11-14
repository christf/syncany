package org.syncany.database.persistence;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.syncany.database.VectorClock;

@Embeddable
public class DatabaseVersionHeaderEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	
    // DB Version and versions of other users (= DB basis)
	@Column(name = "date", nullable = false)
    private Date date;
	
	@Column(name = "vectorclock", nullable = false)
	private VectorClock vectorClock; // vector clock, machine name to database version map
	
	@Column(name = "client", nullable = false)
	private String client;
        
    public DatabaseVersionHeaderEntity() {
    	this.date = new Date();
    	this.vectorClock = new VectorClock();
    	this.client = "UnknownMachine";
    }    

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date timestamp) {
		this.date = timestamp;
	}
	
	public VectorClock getVectorClock() {
		return vectorClock;
	}
	
	public VectorClock getPreviousVectorClock() {
		VectorClock previousVectorClock = vectorClock.clone();

		Long lastPreviousClientLocalClock = previousVectorClock.get(client);
		
		if (lastPreviousClientLocalClock == null) {
			throw new RuntimeException("Previous client '"+client+"' must be present in vector clock of database version header "+this.toString()+".");
		}
		
		if (lastPreviousClientLocalClock == 1) {
			previousVectorClock.remove(client);
			
			if (previousVectorClock.size() == 0) {
				return new VectorClock();
			}
			else {
				return previousVectorClock;
			}
		}
		else {
			previousVectorClock.setClock(client, lastPreviousClientLocalClock-1);
			return previousVectorClock;
		}		
	}
	
	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
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

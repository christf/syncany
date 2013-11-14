package org.syncany.database.persistence;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "DatabaseVersionEntity")
public class DatabaseVersionEntity {
		

    @EmbeddedId
    private DatabaseVersionHeaderEntity header; 
    
	public DatabaseVersionEntity() {
		
	}

	/**
	 * @return the header
	 */
	public DatabaseVersionHeaderEntity getHeader() {
		return header;
	}

	/**
	 * @param header the header to set
	 */
	public void setHeader(DatabaseVersionHeaderEntity header) {
		this.header = header;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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



	
	
}

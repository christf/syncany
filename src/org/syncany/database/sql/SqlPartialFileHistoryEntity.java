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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;

@Entity
@Table(name = "PartialFileHistoryEntity")
public class SqlPartialFileHistoryEntity implements PartialFileHistory{

	@Id
	@Column(name = "fileId")
    private Long fileId;
	
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<Long, SqlFileVersionEntity> versions;
    
    public SqlPartialFileHistoryEntity(long fileId) {
        this.fileId = fileId;
        this.versions = new TreeMap<Long, SqlFileVersionEntity>();    	
    }
    
    public SqlPartialFileHistoryEntity() {
        this.versions = new TreeMap<Long, SqlFileVersionEntity>();    	
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Map<Long, FileVersion> getFileVersions() {
		Map<Long, ? extends FileVersion> versionMap = versions;
        return Collections.unmodifiableMap(versionMap);
    }
    
    public SqlFileVersionEntity getFileVersion(long version) {
    	return versions.get(version);
    }
    
    public SqlFileVersionEntity getLastVersion() {
        if (versions.isEmpty()) {
            return null;
        }
        TreeMap<Long, SqlFileVersionEntity> versionsTree = (TreeMap<Long, SqlFileVersionEntity>) versions;
        return versionsTree.lastEntry().getValue();
    }   

    /**
     * Returns an iterator on the version numbers stored in this partial history, in reverse order. 
     * 
     * @return an iterator on the version numbers in reverse order 
     */
    public Iterator<Long> getDescendingVersionNumber() {
        TreeMap<Long, SqlFileVersionEntity> versionsTree = (TreeMap<Long, SqlFileVersionEntity>) versions;
    	return Collections.unmodifiableSet(versionsTree.descendingKeySet()).iterator();
    }
    
    public void addFileVersion(FileVersion fileVersion) {
		if(fileVersion instanceof SqlFileVersionEntity) {
			versions.put(fileVersion.getVersion(), (SqlFileVersionEntity)fileVersion);     
		} else {
			throw new RuntimeException("Invalid subclass for file version. Implement mapping to Entity.");
		}
    }
    
    @Override
    public SqlPartialFileHistoryEntity clone() {
    	SqlPartialFileHistoryEntity clone = new SqlPartialFileHistoryEntity(fileId);
    	clone.versions.putAll(versions);

    	return clone;
    }

    @Override
    public String toString() {
    	return SqlPartialFileHistoryEntity.class.getSimpleName()+"(fileId="+fileId+", versions="+versions+")";
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
		result = prime * result + ((versions == null) ? 0 : versions.hashCode());
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
		SqlPartialFileHistoryEntity other = (SqlPartialFileHistoryEntity) obj;
		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;
		if (versions == null) {
			if (other.versions != null)
				return false;
		} else if (!versions.equals(other.versions))
			return false;
		return true;
	}
}

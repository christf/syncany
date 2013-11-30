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
package org.syncany.database;

import java.util.Date;

import org.syncany.database.mem.MemFileContent;

/**
 * A file version represents a version of a file at a certain time and captures
 * all of a file's properties. 
 * 
 * <p>A {@link PartialFileHistory} typically consists of multiple <tt>FileVersion</tt>s,  
 * each of which is the incarnation of the same file, but with either changed properties,
 * or changed content.
 * 
 * <p>The <tt>FileVersion</tt>'s checksum attribute implicitly links to a {@link MemFileContent},
 * which represents the content of a file. Multiple file versions can link to the same file content.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface FileVersion extends Cloneable {

    public String getCreatedBy();

    public void setCreatedBy(String createdBy);

    public Long getVersion();

    public void setVersion(Long version);
    
    public FileType getType();

	public void setType(FileType type);

    public Date getLastModified();

    public void setLastModified(Date lastModified);

    public Date getUpdated();

    public void setUpdated(Date updated);

    public FileStatus getStatus();

    public void setStatus(FileStatus status);
    
    public String getPath();

	public String getName();
    
    public void setPath(String path);
    
    public byte[] getChecksum();

	public void setChecksum(byte[] checksum);

	public Long getSize();

	public void setSize(Long size);
	
	public String getLinkTarget();
	
	public void setLinkTarget(String linkTarget);

	public String getPosixPermissions();

	public void setPosixPermissions(String posixPermissions);

	public String getDosAttributes();

	public void setDosAttributes(String dosAttributes);

	public FileVersion clone();
	
	public enum FileStatus {
		NEW ("NEW"), 
		CHANGED ("CHANGED"), 
		RENAMED ("RENAMED"), 
		DELETED ("DELETED");
		
		private String name;       
		
		private FileStatus(String name) {
			this.name = name;
		}
		
		public boolean equalsName(String otherName){
			return (otherName == null) ? false : name.equals(otherName);
		}
		
		public String toString() {
			return name;
		}	
	}
	
	/**
	 * A {@link FileVersion} can be of either one of the types in this enum.
	 * Types are treated differently during the index and synchronization process.
	 */
	public enum FileType {
		FILE ("FILE"), 
		FOLDER ("FOLDER"),
		SYMLINK ("SYMLINK");
		
		private String name;       
		
		private FileType(String name) {
			this.name = name;
		}
		
		public boolean equalsName(String otherName){
			return (otherName == null) ? false : name.equals(otherName);
		}
		
		public String toString() {
			return name;
		}	
	}
}

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

import java.util.Iterator;
import java.util.Map;


public interface IPartialFileHistory {

	public Long getFileId();
	
    public void setFileId(Long fileId);

    public Map<Long, IFileVersion> getFileVersions();
    
    public IFileVersion getFileVersion(long version);
    
    public IFileVersion getLastVersion();
    
    /**
     * Returns an iterator on the version numbers stored in this partial history, in reverse order. 
     * 
     * @return an iterator on the version numbers in reverse order 
     */
    public Iterator<Long> getDescendingVersionNumber();
    
    public void addFileVersion(IFileVersion fileVersion);
    
}

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
package org.syncany.connection.plugins.rest;

import java.util.Map;

import org.jets3t.service.security.ProviderCredentials;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.StorageException;

/**
 *
 * @author oubou68, pheckel
 */
public abstract class RestConnection implements Connection {
    protected String accessKey;
    protected String secretKey; 
    protected String bucket;    
    protected ProviderCredentials credentials;
    
    @Override
	public void init(Map<String, String> map) throws StorageException {
		accessKey = map.get("accessKey");
		secretKey = map.get("secretKey");
		bucket = map.get("bucket");
		
		if (accessKey == null || secretKey == null || bucket == null) {
			throw new StorageException("Config does not contain 'accessKey', 'secretKey' or 'bucket' setting.");
		}
	}   
    
    public String[] getMandatorySettings() {    	
    	return new String[] { "accessKey", "secretKey", "bucket" };
    }
    
    public String[] getOptionalSettings() {    	
    	return new String[] { };
    }
    
    public String getAccessKey() {
        return accessKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public ProviderCredentials getCredentials() {
        if (credentials == null) {
            credentials = createCredentials();
        }
        
        return credentials;
    }        
    
    protected abstract ProviderCredentials createCredentials();        
}

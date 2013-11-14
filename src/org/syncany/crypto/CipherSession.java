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
package org.syncany.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.syncany.util.StringUtil;

public class CipherSession {
    private static final Logger logger = Logger.getLogger(CipherSession.class.getSimpleName());   
	private static final int DEFAULT_SECRET_KEY_READ_CACHE_SIZE = 20;
	private static final int DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT = 100;
	
	private SecretKey masterKey;	
	
	private Map<CipherSpecWithSalt, SecretKeyCacheEntry> secretKeyReadCache;
	private int secretKeyReadCacheSize;
	
	private Map<CipherSpec, SecretKeyCacheEntry> secretKeyWriteCache;
	private int secretKeyWriteReuseCount;
	
	public CipherSession(SaltedSecretKey masterKey) {
		this(masterKey, DEFAULT_SECRET_KEY_READ_CACHE_SIZE, DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT);
	}
	
	public CipherSession(SaltedSecretKey masterKey, int secretKeyReadCacheSize, int secretKeyWriteReuseCount) {
		this.masterKey = masterKey;

		this.secretKeyReadCache = new LinkedHashMap<CipherSpecWithSalt, SecretKeyCacheEntry>();
		this.secretKeyReadCacheSize = secretKeyReadCacheSize;
		
		this.secretKeyWriteCache = new HashMap<CipherSpec, SecretKeyCacheEntry>();
		this.secretKeyWriteReuseCount = secretKeyWriteReuseCount;
	}	

	public SecretKey getMasterKey() {
		return masterKey;
	}

	public void setMasterKey(SecretKey masterKey) {
		this.masterKey = masterKey;
	}

	public SaltedSecretKey getWriteSecretKey(CipherSpec cipherSpec) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		SecretKeyCacheEntry secretKeyCacheEntry = secretKeyWriteCache.get(cipherSpec);
		
		// Remove key if use more than X times 
		if (secretKeyCacheEntry != null && secretKeyCacheEntry.getUseCount() >= secretKeyWriteReuseCount) {
			logger.log(Level.INFO, "Removed WRITE secret key from cache, because it was used "+secretKeyCacheEntry.getUseCount()+" times.");				

			secretKeyWriteCache.remove(cipherSpec);
			secretKeyCacheEntry = null;
		}
				
		// Return cached key, or create a new one
		if (secretKeyCacheEntry != null) {					
			secretKeyCacheEntry.increaseUseCount();
			
			logger.log(Level.INFO, "Using CACHED WRITE secret key "+secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm()+", with salt "+StringUtil.toHex(secretKeyCacheEntry.getSaltedSecretKey().getSalt()));
			return secretKeyCacheEntry.getSaltedSecretKey();
		}
		else {			
			SaltedSecretKey saltedSecretKey = createSaltedSecretKey(cipherSpec);
						
			secretKeyCacheEntry = new SecretKeyCacheEntry(saltedSecretKey);
			secretKeyWriteCache.put(cipherSpec, secretKeyCacheEntry);
			
			logger.log(Level.INFO, "Created NEW WRITE secret key "+secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm()+", and added to cache, with salt "+StringUtil.toHex(saltedSecretKey.getSalt()));		
			return saltedSecretKey;
		}				
	}	
	
	public SaltedSecretKey getReadSecretKey(CipherSpec cipherSpec, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		CipherSpecWithSalt cipherSpecWithSalt = new CipherSpecWithSalt(cipherSpec, salt);
		SecretKeyCacheEntry secretKeyCacheEntry = secretKeyReadCache.get(cipherSpecWithSalt);
		
		if (secretKeyCacheEntry != null) {
			logger.log(Level.INFO, "Using CACHED READ secret key "+secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm()+", with salt "+StringUtil.toHex(salt));
			return secretKeyCacheEntry.getSaltedSecretKey();
		}
		else {
			if (secretKeyReadCache.size() > secretKeyReadCacheSize) {
				CipherSpecWithSalt firstKey = secretKeyReadCache.keySet().iterator().next();
				secretKeyReadCache.remove(firstKey);
				
				logger.log(Level.INFO, "Removed oldest READ secret key from cache.");				
			}
			
			SaltedSecretKey saltedSecretKey = createSaltedSecretKey(cipherSpec, salt);
			secretKeyCacheEntry = new SecretKeyCacheEntry(saltedSecretKey);
			
			secretKeyReadCache.put(cipherSpecWithSalt, secretKeyCacheEntry);
									
			logger.log(Level.INFO, "Created NEW READ secret key "+secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm()+", and added to cache, with salt "+StringUtil.toHex(salt));
			return saltedSecretKey;
		}
	}		
	
	private SaltedSecretKey createSaltedSecretKey(CipherSpec cipherSpec) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		byte[] salt = CipherUtil.createRandomArray(MultiCipherOutputStream.SALT_SIZE); 
		return createSaltedSecretKey(cipherSpec, salt);
	}
	
	private SaltedSecretKey createSaltedSecretKey(CipherSpec cipherSpec, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		return CipherUtil.createDerivedKey(masterKey, salt, cipherSpec);					
	}

	private static class SecretKeyCacheEntry {
		private SaltedSecretKey saltedSecretKey;
		private int useCount;

		public SecretKeyCacheEntry(SaltedSecretKey saltedSecretKey) {
			this.saltedSecretKey = saltedSecretKey;
			this.useCount = 1;
		}

		public SaltedSecretKey getSaltedSecretKey() {
			return saltedSecretKey;
		}

		public int getUseCount() {
			return useCount;
		}
		
		public void increaseUseCount() {
			useCount++;
		}
	}
	
	private static class CipherSpecWithSalt {
		private CipherSpec cipherSpec;
		private byte[] salt;
		
		public CipherSpecWithSalt(CipherSpec cipherSpec, byte[] salt) {
			this.cipherSpec = cipherSpec;
			this.salt = salt;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cipherSpec == null) ? 0 : cipherSpec.hashCode());
			result = prime * result + Arrays.hashCode(salt);
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
			CipherSpecWithSalt other = (CipherSpecWithSalt) obj;
			if (cipherSpec == null) {
				if (other.cipherSpec != null)
					return false;
			} else if (!cipherSpec.equals(other.cipherSpec))
				return false;
			if (!Arrays.equals(salt, other.salt))
				return false;
			return true;
		}			
	}

}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class RestTransferManager extends AbstractTransferManager {
	private static final String APPLICATION_CONTENT_TYPE = "application/x-syncany";
	private static final Logger logger = Logger.getLogger(RestTransferManager.class.getSimpleName());

	private RestStorageService service;
	private StorageBucket bucket;

	private String multichunkPath;
	private String databasePath;

	public RestTransferManager(RestConnection connection) {
		super(connection);

		this.multichunkPath = "multichunks";
		this.databasePath = "databases";
	}

	@Override
	public RestConnection getConnection() {
		return (RestConnection) super.getConnection();
	}

	@Override
	public void connect() throws StorageException {
		try {
			if (service == null) {
				service = createService();
			}

			if (bucket == null) {
				bucket = createBucket();
			}
		}
		catch (ServiceException ex) {
			throw new StorageException("Unable to connect to S3: " + ex.getMessage(), ex);
		}
	}

	protected abstract RestStorageService createService() throws ServiceException;

	protected abstract StorageBucket createBucket();

	@Override
	public void disconnect() throws StorageException {
		// Fressen.
	}

	@Override
	public void init() throws StorageException {
		connect();

		try {
			StorageObject multichunkPathFolder = new StorageObject(multichunkPath + "/"); // Slash ('/') makes it a folder
			service.putObject(bucket.getName(), multichunkPathFolder);

			StorageObject databasePathFolder = new StorageObject(databasePath + "/"); // Slash ('/') makes it a folder
			service.putObject(bucket.getName(), databasePathFolder);
		}
		catch (ServiceException e) {
			throw new StorageException(e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		File tempFile = null;
		String remotePath = getRemoteFile(remoteFile);

		try {
			// Download
			StorageObject fileObj = service.getObject(bucket.getName(), remotePath);
			InputStream fileObjInputStream = fileObj.getDataInputStream();

			logger.log(Level.FINE, "- Downloading from bucket " + bucket.getName() + ": " + fileObj + " ...");
			tempFile = createTempFile(remoteFile.getName());
			FileUtil.writeToFile(fileObjInputStream, tempFile);

			fileObjInputStream.close();

			// Move to final location
			if (localFile.exists()) {
				localFile.delete();
			}

			FileUtils.moveFile(tempFile, localFile);
		}
		catch (Exception ex) {
			if (tempFile != null) {
				tempFile.delete();
			}

			throw new StorageException("Unable to download file '" + remoteFile.getName(), ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);

		try {
			StorageObject fileObject = new StorageObject(remotePath);

			fileObject.setContentLength(localFile.length());
			fileObject.setContentType(APPLICATION_CONTENT_TYPE);
			fileObject.setDataInputStream(new FileInputStream(localFile));

			logger.log(Level.FINE, "- Uploading to bucket " + bucket.getName() + ": " + fileObject + " ...");
			service.putObject(bucket.getName(), fileObject);
		}
		catch (Exception ex) {
			Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);

		try {
			service.deleteObject(bucket.getName(), remotePath);
			return true;
		}
		catch (ServiceException ex) {
			Logger.getLogger(RestTransferManager.class.getName()).log(Level.SEVERE, null, ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		try {
			// List folder
			String remoteFilePath = getRemoteFilePath(remoteFileClass);
			String bucketName = bucket.getName();
			StorageObject[] objects = service.listObjects(bucketName, remoteFilePath, null);

			// Create RemoteFile objects
			Map<String, T> remoteFiles = new HashMap<String, T>();

			for (StorageObject storageObject : objects) {
				String simpleRemoteName = storageObject.getName().substring(storageObject.getName().lastIndexOf("/") + 1);

				if (simpleRemoteName.length() > 0) {
					try {
						T remoteFile = RemoteFile.createRemoteFile(simpleRemoteName, remoteFileClass);
						remoteFiles.put(simpleRemoteName, remoteFile);
					}
					catch (Exception e) {
						logger.log(Level.INFO, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for object " + simpleRemoteName + "; maybe invalid file name pattern. Ignoring file.");
					}
				}
			}

			return remoteFiles;
		}
		catch (ServiceException ex) {
			logger.log(Level.SEVERE, "Unable to list FTP directory.", ex);
			throw new StorageException(ex);
		}
	}

	private String getRemoteFile(RemoteFile remoteFile) {
		String remoteFilePath = getRemoteFilePath(remoteFile.getClass());

		if (remoteFilePath != null) {
			return remoteFilePath + "/" + remoteFile.getName();
		}
		else {
			return remoteFile.getName();
		}
	}

	private String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultiChunkRemoteFile.class)) {
			return multichunkPath;
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return databasePath;
		}
		else {
			return null;
		}
	}
}

package org.syncany.connection.plugins.bt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

// TODO [high] use torrent library that supports DHT
public class BtTransferManager extends AbstractTransferManager {
	private static final Logger logger = Logger.getLogger(BtTransferManager.class.getSimpleName());

	private static final int CONNECT_RETRY_COUNT = 3;
	private static final int TIMEOUT_DEFAULT = 5000;
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_DATA = 5000;

	// private FTPClient ftp;

	private String repoPath;
	private String multichunkPath;
	private String databasePath;

	public BtTransferManager(BtConnection connection) {
		super(connection);

		// this.ftp = new FTPClient();
		this.repoPath = connection.getPath();
		this.multichunkPath = connection.getPath() + "/multichunks";
		this.databasePath = connection.getPath() + "/databases";
	}

	@Override
	public BtConnection getConnection() {
		return (BtConnection) super.getConnection();
	}

	@Override
	public void connect() throws StorageException {
		// webdav connection must be created, the bt connections are created upon download / upload

		/*
		 * for (int i = 0; i < CONNECT_RETRY_COUNT; i++) { try { if (ftp.isConnected()) { return; }
		 * 
		 * if (logger.isLoggable(Level.INFO)) { logger.log(Level.INFO, "FTP client connecting to {0}:{1} ...", new Object[] {
		 * getConnection().getHostname(), getConnection().getPort() }); }
		 * 
		 * ftp.setConnectTimeout(TIMEOUT_CONNECT); ftp.setDataTimeout(TIMEOUT_DATA); ftp.setDefaultTimeout(TIMEOUT_DEFAULT);
		 * 
		 * ftp.connect(getConnection().getHostname(), getConnection().getPort()); ftp.login(getConnection().getUsername(),
		 * getConnection().getPassword()); ftp.enterLocalPassiveMode(); ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!
		 * 
		 * return; } catch (Exception ex) { if (i == CONNECT_RETRY_COUNT - 1) { logger.log(Level.WARNING,
		 * "FTP client connection failed. Retrying failed.", ex); throw new StorageException(ex); } else { logger.log(Level.WARNING,
		 * "FTP client connection failed. Retrying " + (i + 1) + "/" + CONNECT_RETRY_COUNT + " ...", ex); } } }
		 */
	}

	@Override
	public void disconnect() {
		try {
			// ftp.disconnect();
		}
		catch (Exception ex) {
			// Nothing
		}
	}

	@Override
	public void init() throws StorageException {
		connect();
		/*
		 * try { ftp.mkd(multichunkPath); ftp.mkd(databasePath); } catch (IOException e) { forceFtpDisconnect(); throw new
		 * StorageException("Cannot create directory " + multichunkPath + ", or " + databasePath, e); }
		 */
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		// / TTorrent example client code
		// First, instantiate the Client object.
		Client client;
		client = null;
		try {
			client = new Client(
			// This is the interface the client will listen on (you might need something
			// else than localhost here).
					InetAddress.getLocalHost(),

					// Load the torrent from the torrent file and use the given
					// output directory. Partials downloads are automatically recovered.
					SharedTorrent.fromFile(new File("/path/to/your.torrent"), localFile));
		}
		catch (IOException e) {
			// TODO - major - Auto-generated catch block
			e.printStackTrace();
		}

		// You can optionally set download/upload rate limits
		// in kB/second. Setting a limit to 0.0 disables rate
		// limits.
		// client.setMaxDownloadRate(50.0);
		// client.setMaxUploadRate(50.0);

		// At this point, can you either call download() to download the torrent and
		// stop immediately after...
		client.download();

		// Or call client.share(...) with a seed time in seconds:
		// client.share(3600);
		// Which would seed the torrent for an hour after the download is complete.

		// Downloading and seeding is done in background threads.
		// To wait for this process to finish, call:
		client.waitForCompletion();

		// At any time you can call client.stop() to interrupt the download.

		String remotePath = getRemoteFile(remoteFile);

		try {
			// Download file
			File tempFile = createTempFile(localFile.getName());
			OutputStream tempFOS = new FileOutputStream(tempFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Downloading {0} to temp file {1}", new Object[] { remotePath, tempFile });
			}

			// ftp.retrieveFile(remotePath, tempFOS);

			tempFOS.close();

			// Move file
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[] { tempFile, localFile });
			}

			localFile.delete();
			FileUtils.moveFile(tempFile, localFile);
			tempFile.delete();
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Error while downloading file " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);
		String tempRemotePath = getConnection().getPath() + "/temp-" + remoteFile.getName();
		try {
			// Upload to temp file
			InputStream fileFIS = new FileInputStream(localFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Uploading {0} to temp file {1}", new Object[] { localFile, tempRemotePath });
			}

			// ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!

			// if (!ftp.storeFile(tempRemotePath, fileFIS)) {
			// throw new IOException("Error uploading file " + remoteFile.getName());
			// }

			fileFIS.close();

			// Move
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[] { tempRemotePath, remotePath });
			}

			// ftp.rename(tempRemotePath, remotePath);
		}
		catch (IOException ex) {
			// forceFtpDisconnect();
			//
			// logger.log(Level.SEVERE, "Could not upload file " + localFile + " to " + remoteFile.getName(), ex);
			// throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);
		/*
		 * try { return ftp.deleteFile(remotePath); }
		 * 
		 * catch (IOException ex) { forceFtpDisconnect();
		 * 
		 * logger.log(Level.SEVERE, "Could not delete file " + remoteFile.getName(), ex); throw new StorageException(ex); }
		 */
		return (false);
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		// try {
		// List folder
		String remoteFilePath = getRemoteFilePath(remoteFileClass);
		// FTPFile[] ftpFiles = ftp.listFiles(remoteFilePath + "/");

		// Create RemoteFile objects
		Map<String, T> remoteFiles = new HashMap<String, T>();
		/*
		 * for (FTPFile file : ftpFiles) { try { T remoteFile = RemoteFile.createRemoteFile(file.getName(), remoteFileClass);
		 * remoteFiles.put(file.getName(), remoteFile); } catch (Exception e) { logger.log(Level.INFO, "Cannot create instance of " +
		 * remoteFileClass.getSimpleName() + " for file " + file + "; maybe invalid file name pattern. Ignoring file."); } }
		 */
		return remoteFiles;
		// }
		/*
		 * catch (IOException ex) {
		 * 
		 * forceFtpDisconnect();
		 * 
		 * logger.log(Level.SEVERE, "Unable to list FTP directory.", ex); throw new StorageException(ex); }
		 */
	}

	private void forceFtpDisconnect() {
		/*
		 * try { ftp.disconnect(); } catch (IOException e) { // Nothing }
		 */
	}

	private String getRemoteFile(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getClass()) + "/" + remoteFile.getName();
	}

	private String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultiChunkRemoteFile.class)) {
			return multichunkPath;
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return databasePath;
		}
		else {
			return repoPath;
		}
	}
}

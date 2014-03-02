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

package org.syncany.connection.plugins.bt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.syncany.connection.plugins.AbstractTransferManager;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.FileUtil;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineImpl;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.TorrentHandler;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.common.TorrentCreator;

// TODO [medium] use torrent library that supports DHT
// TODO [feature] Allow to configure the interface using the configuration of syncany

public class BtTransferManager extends AbstractTransferManager {
	private static final String APPLICATION_CONTENT_TYPE = "application/x-syncany";
	private static final Logger logger = Logger.getLogger(BtTransferManager.class.getSimpleName());

	private static final String announceUrl = "http://localhost:6969/announce";
	private int port;
	private Sardine sardine;

	private final String btCache = ".syncany/btcache";
	private final String btDataDir = btCache + File.separator + "data";
	private final String btTorrentDir = btCache + File.separator + "torrents";
	private String repoPath;
	private String multichunkPath;
	private String databasePath;
	private InetAddress inetaddress;

	public BtTransferManager(BtConnection connection) {
		super(connection);
		// logger.setLevel(Level.INFO);
		// ConsoleHandler handler = new ConsoleHandler();
		// handler.setFormatter(new SimpleFormatter());
		// logger.addHandler(handler);
		// handler.setLevel(Level.INFO);

		this.repoPath = connection.getUrl().replaceAll("/$", "");
		this.multichunkPath = connection.getUrl() + "/torrents";
		this.databasePath = connection.getUrl() + "/databases";
		this.port = connection.getPort();
	}

	@Override
	public BtConnection getConnection() {
		return (BtConnection) super.getConnection();
	}

	@Override
	public void connect() throws StorageException {
		File mkDir = new File(this.btCache);
		mkDir.mkdir();
		mkDir = new File(this.btDataDir);
		mkDir.mkdir();
		mkDir = new File(this.btTorrentDir);
		mkDir.mkdir();
		mkDir = null;

		NetworkHelper netHelper = new NetworkHelper();

		if (sardine == null) {
			if (getConnection().isSecure()) {
				final SSLSocketFactory sslSocketFactory = getConnection().getSslSocketFactory();

				sardine = new SardineImpl() {
					@Override
					protected SSLSocketFactory createDefaultSecureSocketFactory() {
						return sslSocketFactory;
					}
				};

				sardine.setCredentials(getConnection().getUsername(), getConnection().getPassword());
			}
			else {
				sardine = SardineFactory.begin(getConnection().getUsername(), getConnection().getPassword());
			}
		}

		try {
			this.inetaddress = netHelper.obtainInetAddress();
		}
		catch (SocketException e) {
			throw new StorageException("could not find a suitable IP-Address to bind", e);
		}

		// create remote base path if it does not exist
		try {
			sardine.createDirectory(getConnection().getUrl());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO [high] initialize the seeding queue and start at least some clients.
	}

	@Override
	public void disconnect() {
		sardine = null;
		inetaddress = null;
	}

	// @Override
	// public void init() throws StorageException {

	// }

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		try {
			Client client;

			String remoteURL = getRemoteFileUrl(remoteFile);

			logger.log(Level.INFO, " - Downloading " + remoteURL + " to temp file " + localFile + " ...");
			InputStream webdavFileInputStream = sardine.get(remoteURL);

			FileUtil.writeToFile(webdavFileInputStream, localFile);
			webdavFileInputStream.close();

			if (isMultiChunkRemoteFile(remoteFile.getClass())) {
				// we did not download the data but the torrent file. Getting the data and correcting the paths
				CopyOption[] options = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };
				String localTorrentFile = new String();
				localTorrentFile = btTorrentDir + File.separator + localFile.getName() + ".torrent";
				Files.move(localFile.toPath(), Paths.get(localTorrentFile), options[0]);
				Torrent torrent;
				torrent = new Torrent(new File(localTorrentFile));
				client = new Client(new InetSocketAddress(this.inetaddress, this.port));
				TorrentHandler torrentHandler = new TorrentHandler(client, torrent, localFile.getParentFile());
				client.addTorrent(torrentHandler);
				try {
					client.start();
				}
				catch (Exception e) {
					// TODO is this the correct way to handle this?
					e.printStackTrace();
					throw new StorageException(e);
				}

				CountDownLatch latch = new CountDownLatch(1);
				client.addClientListener(new ClientCompletionListener(latch, TorrentHandler.State.SEEDING));
				await(latch, client);
				Files.copy(localFile.toPath(), Paths.get(btDataDir + File.separator + localFile.getName()), options[0]);
				// no need to share here, this is done by the daemon
			}
			else if (isDataBaseRemoteFile(remoteFile.getClass())) {
				// nothing to do here, the file already got downloaded
			}
		}
		catch (IOException | URISyntaxException ex) {
			logger.log(Level.SEVERE, "Error while downloading file: " + remoteFile, ex);
			throw new StorageException(ex);
		}
		catch (InterruptedException e1) {
			// TODO is this the proper way to handle this?
			e1.printStackTrace();
			throw new StorageException(e1);
		}
	}

	private boolean isMultiChunkFile(File localFile) {
		if (localFile.getName().toString().startsWith("multichunk")) {
			return true;
		}
		return false;
	}

	protected void await(CountDownLatch latch, Client c) throws InterruptedException {
		for (;;) {
			if (latch.await(5, TimeUnit.SECONDS))
				break;
			c.info(true);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();

		System.out.println("local file: " + localFile.getPath() + File.separator + localFile.getName());

		String remoteURL = getRemoteFileUrl(remoteFile);
		File uploadFile = localFile;

		ArrayList<File> files = new ArrayList<File>();

		if (isMultiChunkFile(localFile)) {

			CopyOption[] options = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };
			String torrentFile = new String(btTorrentDir + File.separator + localFile.getName() + ".torrent");
			ArrayList<File> file = new ArrayList<File>();
			file.add(new File(btDataDir + File.separator + localFile.getName()));
			TorrentCreator creator = new TorrentCreator(new File(btDataDir));
			creator.setFiles(file);

			try {
				Files.copy(localFile.toPath(), Paths.get(btDataDir + File.separator + localFile.getName()), options[0]);
				creator.setAnnounce(new URI(announceUrl));
				Torrent torrent = creator.create();

				OutputStream fos = FileUtils.openOutputStream(new File(torrentFile));
				try {
					torrent.save(fos);
				}

				finally {
					IOUtils.closeQuietly(fos);
				}
			}
			catch (IOException | URISyntaxException | InterruptedException e) {
				logger.severe("torrent file could not be written");
				e.printStackTrace();
				throw new StorageException(e);
			}
			uploadFile = new File(torrentFile);
		}
		try {
			logger.log(Level.INFO, " - Uploading local file " + uploadFile.getName() + " to " + remoteURL + " ...");
			InputStream localFileInputStream = new FileInputStream(uploadFile);

			sardine.put(remoteURL, localFileInputStream, APPLICATION_CONTENT_TYPE);
			localFileInputStream.close();
		}
		catch (Exception e) {
			throw new StorageException("could not create torrent for files: " + files + "or could not upload metadata to webdav storage", e);
		}
		// TODO [low] notify the seeding daemon that the directory should be scanned (or that there is a new file for seeding?)
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		try {
			// List folder
			String remoteFileUrl = getRemoteFilePath(remoteFileClass);
			List<DavResource> resources = sardine.list(remoteFileUrl);

			// Create RemoteFile objects
			String rootPath = repoPath.substring(0, repoPath.length() - new URI(repoPath).getRawPath().length());
			Map<String, T> remoteFiles = new HashMap<String, T>();

			for (DavResource res : resources) {
				// WebDAV returns the parent resource itself; ignore it
				String fullResourceUrl = rootPath + res.getPath().replaceAll("/$", "");
				boolean isParentResource = remoteFileUrl.equals(fullResourceUrl.toString());

				if (!isParentResource) {
					try {
						T remoteFile = RemoteFile.createRemoteFile(res.getName(), remoteFileClass);
						remoteFiles.put(res.getName(), remoteFile);

						logger.log(Level.FINE, "- Matching WebDAV resource: " + res);
					}
					catch (Exception e) {
						logger.log(Level.INFO, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for object " + res.getName()
								+ "; maybe invalid file name pattern. Ignoring file.");
					}
				}
			}

			return remoteFiles;
		}
		catch (Exception ex) {
			logger.log(Level.SEVERE, "Unable to list WebDAV directory.", ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();
		String remoteURL = getRemoteFileUrl(remoteFile);

		try {
			sardine.delete(remoteURL);
			return true;
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Error while deleting file from WebDAV: " + remoteURL, ex);
			throw new StorageException(ex);
		}
	}

	private String getRemoteFileUrl(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getClass()) + "/" + remoteFile.getName();
	}

	private boolean isMultiChunkRemoteFile(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultiChunkRemoteFile.class)) {
			return true;
		}
		return false;
	}

	private boolean isDataBaseRemoteFile(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return true;
		}
		return false;
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

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		try {
			if (createIfRequired) {
				sardine.createDirectory(repoPath);
			}
			// use the multichunkPath to store the torrent files for each DB-state
			sardine.createDirectory(multichunkPath);
			sardine.createDirectory(databasePath);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot initialize WebDAV folder.", e);
			throw new StorageException(e);
		}
		File mkDir = new File(this.btCache);
		mkDir.mkdir();
		mkDir = new File(this.btDataDir);
		mkDir.mkdir();
		mkDir = new File(this.btTorrentDir);
		mkDir.mkdir();
		mkDir = null;
	}

	@Override
	public boolean hasWriteAccess() throws StorageException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean repoExists() throws StorageException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean repoIsEmpty() throws StorageException {
		// TODO Auto-generated method stub
		return false;
	}
}

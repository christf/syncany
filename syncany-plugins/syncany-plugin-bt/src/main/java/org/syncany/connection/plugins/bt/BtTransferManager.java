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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.turn.ttorrent.client.SharedTorrent;
// TODO [medium] use torrent library that supports DHT
// FIXME [low] According to the documentation ttorrent only supports downloading single-file-torrents. Hence a multichunk corresponds to one torrent which is not memory-efficient

public class BtTransferManager extends AbstractTransferManager {
	private static final String APPLICATION_CONTENT_TYPE = "application/x-syncany";
	private static final Logger logger = Logger.getLogger(BtTransferManager.class.getSimpleName());

	private static final String announceUrl = "http://kdserv.dyndns.org:6969/announce";

	private Sardine sardine;
	private ArrayList<SeedEntry> seedList;

	private final String btCache = ".syncany/btcache";
	private final String btDataDir = btCache + File.separator + "data";
	private final String btTorrentDir = btCache + File.separator + "torrents";
	private String repoPath;
	private String multichunkPath;
	private String databasePath;
	private InetAddress inetaddress;

	public BtTransferManager(BtConnection connection) {
		super(connection);

		this.repoPath = connection.getUrl().replaceAll("/$", "");
		this.multichunkPath = connection.getUrl() + "/torrents";
		this.databasePath = connection.getUrl() + "/databases";
	}

	@Override
	public BtConnection getConnection() {
		return (BtConnection) super.getConnection();
	}

	// TODO [feature] Allow to configure the interface using the configuration of syncany
	private InetAddress obtainInetAddress() throws SocketException {
		Enumeration<NetworkInterface> interfaces;

		interfaces = NetworkInterface.getNetworkInterfaces();

		for (NetworkInterface interface_ : Collections.list(interfaces)) {
			if (interface_.isLoopback()) {
				continue;
			}
			if (!interface_.isUp()) {
				continue;
			}

			Enumeration<InetAddress> addresses = interface_.getInetAddresses();
			for (InetAddress address : Collections.list(addresses)) {
				// look only for ipv4 addresses
				if (address instanceof Inet6Address) {
					continue;
				}

				try {
					if (!address.isReachable(3000))
						continue;
				}
				catch (IOException e) {
					continue;
				}

				try (SocketChannel socket = SocketChannel.open()) {
					socket.socket().setSoTimeout(3000);

					int startPort = (int) (Math.random() * 64495 + 1024);
					for (int port = startPort; port < startPort + 15; port++) {
						try {
							socket.bind(new InetSocketAddress(address, port));
							break;
						}
						catch (IOException e) {
							continue;
						}
					}
					socket.connect(new InetSocketAddress("google.com", 80));
				}
				catch (IOException | UnresolvedAddressException ex) {
					// even if there is an exception there might be a different interface which works => continue
					continue;
				}

				String logmessage = new String();
				logmessage = String.format("using interface: %s, ia: %s\n", interface_, address);
				logger.info(logmessage);
				return address;
			}
		}
		logger.severe("could not find a suitable network interface to use");
		return null;
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
			this.inetaddress = obtainInetAddress();
		}
		catch (SocketException e) {
			throw new StorageException("could not find a suitable IP-Address to bind", e);
		}

		// TODO [high] initialize the seeding queue and start at least some clients.
	}

	@Override
	public void disconnect() {
		sardine = null;
		inetaddress = null;
	}

	@Override
	public void init() throws StorageException {
		connect();

		try {
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
				// we did not download the data but the torrent file. Correct the paths
				CopyOption[] options = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };
				String localTorrentFile = new String();
				localTorrentFile = btTorrentDir + File.separator + localFile.getName() + ".torrent";
				Files.move(localFile.toPath(), Paths.get(localTorrentFile), options[0]);
				client = new Client(inetaddress, SharedTorrent.fromFile(new File(localTorrentFile), localFile.getParentFile()));
				client.download();
				client.waitForCompletion();
				Files.copy(localFile.toPath(), Paths.get(btDataDir + File.separator + localFile.getName()), options[0]);
				// TODO - share this torrent
			}
			else if (isDataBaseRemoteFile(remoteFile.getClass())) {
				// nothing to do
			}
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Error while downloading file from WebDAV: " + remoteFile, ex);
			throw new StorageException(ex);
		}
	}

	private boolean isMultiChunkFile(File localFile) {
		if (localFile.getName().toString().startsWith("multichunk")) {
			return true;
		}
		return false;
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
			TorrentCreator torrentCreator = new TorrentCreator();

			String torrentFile = new String(btTorrentDir + File.separator + localFile.getName() + ".torrent");
			try {
				Files.copy(localFile.toPath(), Paths.get(btDataDir + File.separator + localFile.getName()), options[0]);
				files.add(new File(btDataDir + File.separator + localFile.getName()));
				String infohash = new String(torrentCreator.create(torrentFile, announceUrl, files));
				logger.info("created torrent " + torrentFile + " having infohash " + infohash);
			}
			catch (Exception e) {
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
		// TODO add this torrent to seeding queue.
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
}

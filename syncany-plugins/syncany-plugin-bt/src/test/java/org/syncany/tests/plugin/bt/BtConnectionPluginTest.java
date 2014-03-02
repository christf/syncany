/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.plugin.bt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.bt.BtConnection;
import org.syncany.connection.plugins.bt.BtPlugin;
import org.syncany.connection.plugins.bt.BtTransferManager;
import org.syncany.tests.util.TestFileUtil;

import com.github.sardine.Sardine;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

/**
 * @author Christof Schulze <christof.schulze@gmx.net>
 *
 */

// TODO
public class BtConnectionPluginTest {
	private Sardine sardine;
	protected Tracker tracker;
	protected Torrent torrent;
	protected TrackedTorrent trackedTorrent;
	protected Client seed;

	protected final List<Client> leechers = new ArrayList<Client>();
	private static final Logger logger = Logger.getLogger(BtConnectionPluginTest.class.getSimpleName());
	private File tempLocalSourceDir;
	private File localRootDir;
	private Map<String, String> btPluginSettings;
	TransferManager transferManager;
	static final int remotedir = Math.abs(new Random().nextInt());

	@Before
	public void setUp() throws Exception {
		tracker = new Tracker(new InetSocketAddress("localhost", 6969));
		tracker.start();

		// File dir = TorrentTestUtils.newTorrentDir(getClass().getSimpleName() + ".seed");

		// TorrentCreator creator = TorrentTestUtils.newTorrentCreator(dir, 126071);
		// creator.setAnnounceList(tracker.getAnnounceUris());
		// creator.setPieceLength(512);
		// torrent = creator.create();

		// trackedTorrent = tracker.announce(torrent);
		// trackedTorrent.setAnnounceInterval(60, TimeUnit.SECONDS);

		// seed = new Client("S-", new InetSocketAddress("localhost", 6885));
		// TorrentHandler sharedTorrent = new TorrentHandler(seed, torrent, dir);
		// sharedTorrent.setBlockLength(64);
		// seed.addTorrent(sharedTorrent);

		localRootDir = TestFileUtil.createTempDirectoryInSystemTemp();

		tempLocalSourceDir = new File(localRootDir + "/local");
		tempLocalSourceDir.mkdir();

		btPluginSettings = new HashMap<String, String>();

		// TODO: use internal webdav server - unfortunately I cannot yank the test from the webdav plugin because there is no test for it yet.

		btPluginSettings.put("url", "http://localhost:80/webdav/" + remotedir);
		btPluginSettings.put("username", "test");
		btPluginSettings.put("password", "test");
		btPluginSettings.put("port", "50153");

		// btPluginSettings.put("path", remoteRepo);
	}

	@After
	public void tearDown() throws IOException {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
		TestFileUtil.deleteDirectory(localRootDir);
		tracker.stop();
	}

	@Test
	public void testLoadPluginAndCreateTransferManager() throws StorageException {
		loadPluginAndCreateTransferManager();
	}

	@Test
	public void testLocalPluginInfo() {
		Plugin pluginInfo = Plugins.get("bt");

		assertNotNull("PluginInfo should not be null.", pluginInfo);
		assertEquals("Plugin ID should be 'bt'.", "bt", pluginInfo.getId());
		assertNotNull("Plugin version should not be null.", pluginInfo.getVersion());
		assertNotNull("Plugin name should not be null.", pluginInfo.getName());
	}

	@Test(expected = StorageException.class)
	public void testConnectWithInvalidSettings() throws StorageException {
		Plugin pluginInfo = Plugins.get("bt");

		Map<String, String> invalidPluginSettings = new HashMap<String, String>();

		Connection connection = pluginInfo.createConnection();
		connection.init(invalidPluginSettings);

		TransferManager transferManager = connection.createTransferManager();

		// This should cause a Storage exception, because the path does not exist
		transferManager.connect();
	}

	@Test
	public void testBtUploadListDownloadAndDelete() throws Exception {
		// TODO [high] remove assert when appropriate
		// assertEquals("test should be properly implemented but it isn't", "failure");

		// Generate test files
		Map<String, File> inputFiles = generateTestInputFile();

		// Create connection, upload, list, download and remove
		TransferManager transferManager = loadPluginAndCreateTransferManager();
		transferManager.connect();

		Map<File, RemoteFile> uploadedFiles = uploadChunkFiles(transferManager, inputFiles.values());
		Map<String, RemoteFile> remoteFiles = transferManager.list(RemoteFile.class);
		Map<RemoteFile, File> downloadedLocalFiles = downloadRemoteFiles(transferManager, remoteFiles.values());

		// Compare
		assertEquals("Number of uploaded files should be the same as the input files.", uploadedFiles.size(), remoteFiles.size());
		assertEquals("Number of remote files should be the same as the downloaded files.", remoteFiles.size(), downloadedLocalFiles.size());

		for (Map.Entry<String, File> inputFileEntry : inputFiles.entrySet()) {
			File inputFile = inputFileEntry.getValue();

			RemoteFile uploadedFile = uploadedFiles.get(inputFile);
			File downloadedLocalFile = downloadedLocalFiles.get(uploadedFile);

			assertNotNull("Cannot be null.", uploadedFile);
			assertNotNull("Cannot be null.", downloadedLocalFile);

			byte[] checksumOriginalFile = TestFileUtil.createChecksum(inputFile);
			byte[] checksumDownloadedFile = TestFileUtil.createChecksum(downloadedLocalFile);

			assertArrayEquals("Uploaded file differs from original file.", checksumOriginalFile, checksumDownloadedFile);
		}

		// Delete
		for (RemoteFile remoteFileToDelete : uploadedFiles.values()) {
			transferManager.delete(remoteFileToDelete);
		}

		Map<String, RemoteFile> remoteFiles2 = transferManager.list(RemoteFile.class);
		Map<RemoteFile, File> downloadedLocalFiles2 = downloadRemoteFiles(transferManager, remoteFiles2.values());

		for (RemoteFile remoteFileToBeDeleted : downloadedLocalFiles2.keySet()) {
			assertFalse("Could not delete remote file.", downloadedLocalFiles2.containsKey(remoteFileToBeDeleted));
		}
	}

	@Test(expected = StorageException.class)
	public void testDeleteNonExistantFile() throws StorageException {
		TransferManager transferManager = loadPluginAndCreateTransferManager();
		transferManager.connect();
		transferManager.delete(new RemoteFile("non-existant-file"));
	}

	private Map<String, File> generateTestInputFile() throws IOException {
		Map<String, File> inputFilesMap = new HashMap<String, File>();
		List<File> inputFiles = TestFileUtil.createRandomFilesInDirectory(tempLocalSourceDir, 50 * 1024, 10);

		for (File file : inputFiles) {
			inputFilesMap.put(file.getName(), file);
		}

		return inputFilesMap;
	}

	private Map<File, RemoteFile> uploadChunkFiles(TransferManager transferManager, Collection<File> inputFiles) throws StorageException {
		Map<File, RemoteFile> inputFileOutputFile = new HashMap<File, RemoteFile>();

		for (File inputFile : inputFiles) {
			RemoteFile remoteOutputFile = new RemoteFile(inputFile.getName());
			transferManager.upload(inputFile, remoteOutputFile);

			inputFileOutputFile.put(inputFile, remoteOutputFile);
		}

		return inputFileOutputFile;
	}

	private Map<RemoteFile, File> downloadRemoteFiles(TransferManager transferManager, Collection<RemoteFile> remoteFiles) throws StorageException {
		Map<RemoteFile, File> downloadedLocalFiles = new HashMap<RemoteFile, File>();

		for (RemoteFile remoteFile : remoteFiles) {
			File downloadedLocalFile = new File(tempLocalSourceDir + "/downloaded-" + remoteFile.getName());
			transferManager.download(remoteFile, downloadedLocalFile);

			downloadedLocalFiles.put(remoteFile, downloadedLocalFile);

			assertTrue("Downloaded file does not exist.", downloadedLocalFile.exists());
		}

		return downloadedLocalFiles;
	}

	private TransferManager loadPluginAndCreateTransferManager() throws StorageException {
		if (this.transferManager == null) {
			Plugin pluginInfo = Plugins.get("bt");

			Connection connection = pluginInfo.createConnection();
			connection.init(btPluginSettings);

			TransferManager transferManager = connection.createTransferManager();

			assertEquals("LocalPluginInfo expected.", BtPlugin.class, pluginInfo.getClass());
			assertEquals("LocalConnection expected.", BtConnection.class, connection.getClass());
			assertEquals("LocalTransferManager expected.", BtTransferManager.class, transferManager.getClass());
			this.transferManager = transferManager;
		}
		return this.transferManager;
		// return transferManager;
	}
}

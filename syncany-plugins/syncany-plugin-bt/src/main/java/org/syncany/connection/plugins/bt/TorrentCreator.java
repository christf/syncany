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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.turn.ttorrent.common.Torrent;

class TorrentCreator {
	private static final Logger logger = Logger.getLogger(TorrentCreator.class.getSimpleName());

	// public String create(String torrentFile, String announceUrl, List<File> files) throws Exception {
	// return create(torrentFile, announceUrl, files, "", "", "torrent");
	// }

	public String create(String torrentFile, String announceURL, File file) throws Exception {
		String infohash;

		OutputStream fos = null;
		fos = new FileOutputStream(torrentFile);

		URI announceURI = new URI(announceURL);
		String creator = String.format("%s (ttorrent)", System.getProperty("user.name"));

		Torrent torrent = null;
		torrent = Torrent.create(file, announceURI, creator);
		infohash = new String(torrent.getHexInfoHash());
		torrent.save(fos);
		IOUtils.closeQuietly(fos);

		logger.log(Level.INFO, "Torrent " + torrentFile + " created successfully.");
		return infohash;
	}

	// private int calcpiecesize(List<File> files) {
	// int pieceSize;
	// long dataSize = 0;
	//
	// for (File file : files) {
	// dataSize += file.length();
	// }
	//
	// // a Torrent should have below 1500 pieces at the same time the piece size should not be larger
	// // than 2MB - adjust the pieceSize accordingly
	// long sizeValue = dataSize / 1500 / 1024;
	//
	// if (sizeValue < 32) {
	// pieceSize = 32;
	// }
	// else if (sizeValue < 64) {
	// pieceSize = 64;
	// }
	// else if (sizeValue < 256) {
	// pieceSize = 256;
	// }
	// else if (sizeValue < 512) {
	// pieceSize = 512;
	// }
	// else if (sizeValue < 1024) {
	// pieceSize = 1024;
	// }
	// else {
	// pieceSize = 2048;
	// }
	//
	// return pieceSize;
	// }

	// public String create(String torrentFile, String announceurl, List<File> files, String author, String comment, String name) throws Exception {
	// return create(torrentFile, announceurl, files, "", "", "torrent", calcpiecesize(files));
	// }

	// public String create(String torrentFile, String announceURL, List<File> files, String author, String comment, String name, int piecesize)
	// throws URISyntaxException, InterruptedException, IOException {
	//
	// String infohash;
	//
	// OutputStream fos = null;
	// fos = new FileOutputStream(torrentFile);
	//
	// URI announceURI = new URI(announceURL);
	// String creator = String.format("%s (ttorrent)", System.getProperty("user.name"));
	//
	// Torrent torrent = null;
	// torrent = Torrent.create(files.get(0).getParentFile(), files, announceURI, creator);
	// // torrent = Torrent.create(source, announceURI, creator);
	// infohash = new String(torrent.getHexInfoHash());
	// torrent.save(fos);
	// IOUtils.closeQuietly(fos);
	//
	// logger.log(Level.INFO, "Torrent " + torrentFile + " created successfully.");
	// return infohash;
	// }
}
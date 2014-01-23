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

import jBittorrentAPI.TorrentProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class TorrentCreator {
	private static final Logger logger = Logger.getLogger(TorrentCreator.class.getSimpleName());

	public String create(String torrentfile, String announceUrl, List<File> files) {
		return create(torrentfile, announceUrl, files, "", "", "torrent");
	}

	private int calcpiecesize(List<File> files) {
		int pieceSize;
		long dataSize = 0;

		for (File file : files) {
			dataSize += file.length();
		}

		// a Torrent should have below 1500 pieces - adjust the pieceSize accordingly
		long sizeValue = dataSize / 1500 / 1024;

		if (sizeValue < 32) {
			pieceSize = 32;
		}
		else if (sizeValue < 64) {
			pieceSize = 64;
		}
		else if (sizeValue < 256) {
			pieceSize = 256;
		}
		else if (sizeValue < 512) {
			pieceSize = 512;
		}
		else {
			pieceSize = 1024;
		}

		return pieceSize;
	}

	public String create(String torrentfile, String announceurl, List<File> files, String author, String comment, String name) {
		return create(torrentfile, announceurl, files, "", "", "torrent", calcpiecesize(files));
	}

	public String create(String torrentfile, String announceurl, List<File> files, String author, String comment, String name, int piecesize) {
		TorrentProcessor torrentProcessor = new TorrentProcessor();
		torrentProcessor.setAnnounceURL(announceurl);

		torrentProcessor.setName(name);
		torrentProcessor.setPieceLength(piecesize);

		try {
			torrentProcessor.addFiles(files);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Problem when adding files to torrent:", files.toString());
			// TODO [major] - throw a real exception here
			System.exit(1);
		}

		torrentProcessor.setCreator(author);
		torrentProcessor.setComment(comment);

		// TorrentMetaInfo = null;
		try {
			logger.log(Level.INFO, "Hashing the files...");
			torrentProcessor.generatePieceHashes();
			logger.log(Level.INFO, "Hash complete. Saving torrent data to file: " + torrentfile);
			FileOutputStream fos = new FileOutputStream(torrentfile);
			fos.write(torrentProcessor.generateTorrent());
			fos.close();

			// TODO [high] - do not write torrent to a file just to read it to obtain infohash
			// = readExampleFile(torrentfile);

			logger.log(Level.INFO, "Torrent " + torrentfile + " created successfully.");
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Error when writing to the torrent file: " + torrentfile);
			// TODO - major - throw a real exception here
			System.exit(1);
		}
		// return (tmi.getInfoHash().toString());
		return "Infohash for " + torrentfile;
	}

}
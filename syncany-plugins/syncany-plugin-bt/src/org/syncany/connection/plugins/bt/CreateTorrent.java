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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class CreateTorrent {
	private static final Logger logger = Logger.getLogger(CreateTorrent.class.getSimpleName());

	public String create(String torrentfile, String announceurl, ArrayList<File> files) {
		return create(torrentfile, announceurl, files, "", "", "torrent");
	}

	private int calcpiecesize(ArrayList<File> files) {
		int piecesize;
		long wsum = 0;

		for (File file : files) {
			wsum += file.length();
		}

		long swert = wsum / 1000 / 1024;

		if (swert < 32)
			piecesize = 32;
		else if (swert < 64)
			piecesize = 64;
		else if (swert < 256)
			piecesize = 256;
		else if (swert < 512)
			piecesize = 512;
		else
			piecesize = 1024;

		return piecesize;

	}

	public String create(String torrentfile, String announceurl, ArrayList<File> files, String author, String comment, String name) {
		return create(torrentfile, announceurl, files, "", "", "torrent", calcpiecesize(files));
	}

	public String create(String torrentfile, String announceurl, ArrayList<File> files, String author, String comment, String name, int piecesize) {
		TorrentProcessor tp = new TorrentProcessor();
		tp.setAnnounceURL(announceurl);

		tp.setName(name);
		tp.setPieceLength(piecesize);

		try {
			tp.addFiles(files);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Problem when adding files to torrent:", files.toString());
			// TODO [major] - throw a real exception here
			System.exit(1);
		}

		tp.setCreator(author);
		tp.setComment(comment);

		// TorrentMetaInfo = null;
		try {
			logger.log(Level.INFO, "Hashing the files...");
			tp.generatePieceHashes();
			logger.log(Level.INFO, "Hash complete. Saving torrent data to file: " + torrentfile);
			FileOutputStream fos = new FileOutputStream(torrentfile);
			fos.write(tp.generateTorrent());
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
		// return (.getInfoHash().toString());
		return "Hi";
	}

}
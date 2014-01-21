/*
 *
 * Java openbtsync is free software and a free user study set-up;
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Java openbtsync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Bittorrent API; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @version 1.0
 * @author Christof Schulze
 * To contact the author:
 * email: christof.schulze@gmx.net
 *
 * This class is based on the Example for creating torrents from the jBittorrent API.
 */

package org.syncany.connection.plugins.bt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.turn.ttorrent.common.Torrent;

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

		Torrent torrent = new Torrent(files, announceurl, 
		tp.setAnnounceURL(announceurl);

		tp.setName(name);
		tp.setPieceLength(piecesize);

		try {
			tp.addFiles(files);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Problem when adding files to torrent:", files.toString());
			// TODO - major - throw a real exception here
			System.exit(1);
		}

		tp.setCreator(author);
		tp.setComment(comment);

		TorrentMetaInfo tmi = null;
		try {
			logger.log(Level.INFO, "Hashing the files...");
			tp.generatePieceHashes();
			logger.log(Level.INFO, "Hash complete. Saving torrent data to file: " + torrentfile);
			FileOutputStream fos = new FileOutputStream(torrentfile);
			fos.write(tp.generateTorrent());
			fos.close();
			// TODO - major - this is too complicated - first save torrent then read it only to obtain infohash - FIX THIS SHIT
			tmi = readExampleFile(torrentfile);
			logger.log(Level.INFO, "Torrent " + torrentfile + " created successfully.");
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "Error when writing to the torrent file: " + torrentfile);
			// TODO - major - throw a real exception here
			System.exit(1);
		}
		return (tmi.getInfoHash().toString());
	}

	public TorrentMetaInfo readExampleFile(String tfile) throws IOException {
		try (BencodeInputStream bis = new BencodeInputStream(new FileInputStream(tfile))) {
			return TorrentMetaInfo.fromValue(bis.readValue());
		}
	}
}

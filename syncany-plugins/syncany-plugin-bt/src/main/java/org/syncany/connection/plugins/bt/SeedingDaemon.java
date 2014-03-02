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
package org.syncany.connection.plugins.bt;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.TorrentHandler;
import com.turn.ttorrent.common.Torrent;

/**
 * @author Christof Schulze <christof.schulze@gmx.net>
 *
 */
public class SeedingDaemon {
	private static final Logger logger = Logger.getLogger(SeedingDaemon.class.getSimpleName());

	// return idle time in millis
	// private static long calcStatusTime(Client client) {
	// long milliClientLastChange = client.getLastChange().getTime();
	// long millicurrentTime = new Date().getTime();
	// return millicurrentTime - milliClientLastChange;
	// }

	public static void main(String[] args) throws Exception {
		int port = 43534; // TODO [med] make the port configurable

		Port seedingPort = new Port();
		try {
			seedingPort.init();
			seedingPort.map(port, Port.Protocol.TCP);
		}
		catch (Exception e) {
			logger.warning("could not map port " + port + " via upnp. Seeding may not work.");
		}
		NetworkHelper netHelper = new NetworkHelper();
		InetAddress address = netHelper.obtainInetAddress();
		logger.info("passing the following address to ttorrent: " + address);

		File torrentdir = new File("/home/pi/torrent/torrents"); // TODO [high] make torrentdir configurable

		ArrayList<String> torrentFiles = new ArrayList<String>();
		File destination = new File("/home/pi/torrent/seeddata");
		destination.mkdirs();

		Client client = new Client(new InetSocketAddress(address, port));
		// TODO [low] get rid of polling the torrents-dir and have "sy down" notify the seeder-process
		try {
			client.start();

			while (true) {
				for (File ftorrent : torrentdir.listFiles()) {
					logger.info("found file while scanning dir " + ftorrent.getName());
					if (!torrentFiles.contains(ftorrent.getName())) {
						// found a new torrent, seed it iff it has finished downloading.
						logger.info("found a new torrent. " + ftorrent.getName());
						String tDataFileName = new String(destination.getAbsolutePath() + File.separatorChar + ftorrent.getName());
						File cdatafile = new File(tDataFileName.substring(0, tDataFileName.length() - 8));
						if (cdatafile.isFile()) {
							torrentFiles.add(ftorrent.getName());
							Torrent torrent = new Torrent(ftorrent);
							TorrentHandler torrentHandler = new TorrentHandler(client, torrent, destination);
							client.addTorrent(torrentHandler);
						}
					}
				}
				// poll for changes on the torrent dir every minute...
				TimeUnit.SECONDS.sleep(60);
			}
		}
		catch (Exception e) {
			logger.severe("Fatal error: {}" + e.getMessage() + e.getStackTrace());
			System.exit(2);
		}
		finally {
			client.stop();
		}

		// int t = 0;
		// boolean considertime = false;
		// while (t < 1000) {
		// for (QueueingClient client : clients) {
		// // System.out.println(client.getTorrent().getName() + " " + client.getState());
		// // client.info();
		// logger.info("Status: " + client.getState().toString() + " last status change: " + client.getLastChange() + " score: "
		// + client.getSeedingscore());
		//
		// if (considertime == false && currentlyseeding == maxseeding) {
		// considertime = true;
		// }
		//
		// switch (client.getState()) {
		// case WAITING:
		// if (currentlyseeding < maxseeding) {
		// if (!considertime || (considertime && calcStatusTime(client) > 20000 * 60)) {
		// client.share();
		// currentlyseeding++;
		// }
		// }
		// break;
		//
		// case DONE:
		// if (currentlyseeding < maxseeding) {
		// logger.info("STARTE CLIENT NEU");
		// client.share();
		// currentlyseeding++;
		// logger.info("STARTE CLIENT NEU");
		// }
		// break;
		// case SEEDING:
		// Set<SharingPeer> sharingPeers = client.getPeers();
		// int numberOfInterested = 0;
		// for (SharingPeer sharingPeer : sharingPeers) {
		// if (sharingPeer.isInterested()) {
		// numberOfInterested++;
		// }
		// }
		// logger.info(client.getTorrent().getName() + " has " + Integer.toString(numberOfInterested) + " interested peers");
		// if (numberOfInterested == 0 && calcStatusTime(client) > 2000) {
		// // QueueingClient tmpClient = new QueueingClient(address, new SharedTorrent(client.getTorrent()), port);
		// // client.stop();
		// // client.waitForCompletion();
		//
		// // client = null;
		// // client = tmpClient;
		// // currentlyseeding--;
		// }
		// break;
		// case ERROR:
		// System.err.println("client error state found");
		// break;
		// case SHARING:
		// System.err.println("BUG DETECTED - client is still downloading - why is that? it should be finished if it is in this queue");
		// break;
		// case VALIDATING:
		// System.err.println("client is validating, leaving it be for a moment.");
		// break;
		//
		// }
		// }
		//
		// TimeUnit.SECONDS.sleep(1);
		// t++;
		// logger.info("===============================================================");
		// }

		// You can optionally set download/upload rate limits
		// in kB/second. Setting a limit to 0.0 disables rate
		// limits.
		// client.setMaxDownloadRate(50.0);
		// client.setMaxUploadRate(50.0);

		// At this point, can you either call download() to download the torrent and
		// stop immediately after...

		// Or call client.share(...) with a seed time in seconds:
		// Which would seed the torrent for an hour after the download is complete.

		// Downloading and seeding is done in background threads.
		// To wait for this process to finish, call:
		// client.waitForCompletion();
	}
}

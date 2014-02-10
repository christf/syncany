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
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.client.peer.SharingPeer;

/**
 * @author christof
 *
 */
public class TestSeeding {
	private static final Logger logger = Logger.getLogger(TestSeeding.class.getSimpleName());

	// TODO [feature] Allow to configure the interface using the configuration of syncany
	private static InetAddress obtainInetAddress() throws SocketException {
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

	// return idle time in millis
	private static long calcStatusTime(QueueingClient client) {
		long milliClientLastChange = client.getLastChange().getTime();
		long millicurrentTime = new Date().getTime();
		return millicurrentTime - milliClientLastChange;
	}

	public static void main(String[] args) throws IOException, InterruptedException, SAXException, ParserConfigurationException {
		final int maxseeding = 10;
		String dir = new String("/home/pi/syncany-testbed/a/");
		int port = 43534;
		Port seedingPort = new Port();
		try {
			seedingPort.init();
			seedingPort.map(port, Port.Protocol.TCP);
		}
		catch (Exception e) {
			logger.warning("could not map port " + port + " via upnp.");
		}
		ArrayList<QueueingClient> clients = new ArrayList<QueueingClient>();
		InetAddress address = obtainInetAddress();

		File torrentdir = new File(dir + ".syncany/btcache/torrents");
		int currentlyseeding = 0;
		ArrayList<String> torrentFiles = new ArrayList<String>();

		for (File torrent : torrentdir.listFiles()) {
			File destination = new File(dir + ".syncany/btcache/data");
			destination.mkdirs();
			System.out.println(port);
			if (!torrentFiles.contains(torrent.getName())) {
				torrentFiles.add(torrent.getName());
			}
			QueueingClient client = new QueueingClient(address, SharedTorrent.fromFile(torrent, destination), port);
			clients.add(client);
		}

		int t = 0;
		boolean considertime = false;
		while (t < 100) {
			for (QueueingClient client : clients) {

				// System.out.println(client.getTorrent().getName() + " " + client.getState());
				// client.info();
				logger.info("Status: " + client.getState().toString() + " last status change: " + client.getLastChange() + " score: "
						+ client.getSeedingscore());

				if (considertime == false && currentlyseeding == maxseeding) {
					considertime = true;
				}

				switch (client.getState()) {
				case WAITING:
					if (currentlyseeding < maxseeding) {
						if (!considertime || (considertime && calcStatusTime(client) > 20000 * 60)) {
							client.share();
							currentlyseeding++;
						}
					}
					break;

				case DONE:
					if (currentlyseeding < maxseeding) {
						logger.info("STARTE CLIENT NEU");
						client.share();
						currentlyseeding++;
						logger.info("STARTE CLIENT NEU");
					}
					break;
				case SEEDING:
					Set<SharingPeer> sharingPeers = client.getPeers();
					int numberOfInterested = 0;
					for (SharingPeer sharingPeer : sharingPeers) {
						if (sharingPeer.isInterested()) {
							numberOfInterested++;
						}
					}
					logger.info(client.getTorrent().getName() + " has " + Integer.toString(numberOfInterested) + " interested peers");
					if (numberOfInterested == 0 && calcStatusTime(client) > 2000) {
						// QueueingClient tmpClient = new QueueingClient(address, new SharedTorrent(client.getTorrent()), port);
						// client.stop();
						// client.waitForCompletion();

						// client = null;
						// client = tmpClient;
						// currentlyseeding--;
					}
					break;
				case ERROR:
					System.err.println("client error state found");
					break;
				case SHARING:
					System.err.println("BUG DETECTED - client is still downloading - why is that? it should be finished if it is in this queue");
					break;
				case VALIDATING:
					System.err.println("client is validating, leaving it be for a moment.");
					break;

				}
			}

			TimeUnit.SECONDS.sleep(1);
			t++;
			logger.info("===============================================================");
		}

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

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
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.turn.ttorrent.client.Client.ClientState;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.client.peer.SharingPeer;

/**
 * @author christof
 *
 */
public class TestSeeding {
	private static final Logger logger = Logger.getLogger(TestSeeding.class.getSimpleName());

	private static InetAddress obtainInetAddress() {
		Enumeration<NetworkInterface> interfaces;

		try {
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
					// TODO [feature] at some point in time also allow IPv6
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

						// TODO [medium] make sure that this random port is not already in use on that interface
						int port = (int) (Math.random() * 64510 + 1024);
						socket.bind(new InetSocketAddress(address, port));
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
		}
		catch (SocketException e1) {
			// no network interfaces found
			e1.printStackTrace();
		}

		logger.severe("could not find a suitable network interface to use");
		// TODO [high] get rid of System.exit
		System.exit(1);
		return null;
	}

	public static void main(String[] args) throws IOException, InterruptedException, SAXException, ParserConfigurationException {
		final int maxseeding = 1;
		int port = 43534;
		Port seedingPort = new Port();
		seedingPort.init();
		seedingPort.map(port, Port.Protocol.TCP);
		ArrayList<QueueingClient> clients = new ArrayList<QueueingClient>();
		InetAddress address = obtainInetAddress();

		File torrentdir = new File("./torrents");
		int currentlyseeding = 0;

		for (File torrent : torrentdir.listFiles()) {
			File destination = new File("./torrentdata/");
			destination.mkdirs();
			QueueingClient client = new QueueingClient(address, SharedTorrent.fromFile(torrent, destination), port);
			client.share();
			currentlyseeding++;
			clients.add(client);
		}

		int t = 0;
		while (t < 100) {
			for (QueueingClient client : clients) {
				// System.out.println(client.getTorrent().getName() + " " + client.getState());
				// client.info();
				logger.info(client.getState().toString());

				if (ClientState.WAITING.equals(client.getState())) {
					if (currentlyseeding < maxseeding) {
						client.share();
						currentlyseeding++;
					}
				}
				else if (ClientState.DONE.equals(client.getState())) {
					if (currentlyseeding < maxseeding) {
						client.share();
						currentlyseeding++;
					}
				}
				else if (ClientState.SEEDING.equals(client.getState())) {
					Set<SharingPeer> sharingPeers = client.getPeers();
					int numberOfInterested = 0;
					for (SharingPeer sharingPeer : sharingPeers) {
						if (sharingPeer.isInterested()) {
							numberOfInterested++;
						}
					}
					logger.info(Integer.toString(numberOfInterested));
					if (numberOfInterested == 0 && currentlyseeding > maxseeding) {
						// client.stop(true);
						// currentlyseeding--;
					}
				}
				else if (ClientState.ERROR.equals(client.getState())) {
					System.err.println("client error state found");
				}
				else if (ClientState.SHARING.equals(client.getState())) {
					System.err.println("client is still downloading - why is that? it should be finished if it is in this queue");
				}
				else if (ClientState.VALIDATING.equals(client.getState())) {
					System.err.println("client is validating");
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

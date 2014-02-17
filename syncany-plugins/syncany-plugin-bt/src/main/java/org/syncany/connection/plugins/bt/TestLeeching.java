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
import java.util.concurrent.TimeUnit;

import com.turn.ttorrent.client.Client.ClientState;
import com.turn.ttorrent.client.SharedTorrent;

/**
 * @author christof
 *
 */
public class TestLeeching {
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
					System.out.println(logmessage);
					return interface_.getInetAddresses().nextElement();
				}
			}
		}
		catch (SocketException e1) {
			// no network interfaces found
			e1.printStackTrace();
		}

		System.err.println("could not find a suitable network interface to use");
		// TODO [high] get rid of System.exit
		System.exit(1);
		return null;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("running");
		File torrentdir = new File("/home/pi/torrent/torrents");
		File destination = new File("/home/pi/torrent/data");
		destination.mkdirs();
		int port = 6881;

		ArrayList<QueueingClient> clientList = new ArrayList<QueueingClient>();
		System.out.println(torrentdir.listFiles());
		for (File torrent : torrentdir.listFiles()) {
			System.out.println(port);
			// if (!torrentFiles.contains(torrent.getName())) {
			// torrentFiles.add(torrent.getName());
			// }
			QueueingClient client = new QueueingClient(obtainInetAddress(), SharedTorrent.fromFile(torrent, destination), port);
			System.out.println("Start to download: " + torrent.getName() + " " + client.getTorrent().getHexInfoHash());
			clientList.add(client);
			client.share();
		}

		while (!clientList.isEmpty()) {
			System.out.println("im loop " + clientList.size());
			for (QueueingClient client : clientList) // use for-each loop
			{
				// Display statistics
				System.out.printf("%f %% - %d bytes downloaded - %d bytes uploaded", client.getTorrent().getCompletion(), client.getTorrent()
						.getDownloaded(), client.getTorrent().getUploaded());
				System.out.println(" " + client.getTorrent().getFilenames() + " " + client.getState());
				// Wait one second

				if (ClientState.SEEDING.equals(client.getState())) {
					clientList.remove(client);
				}
			}
			TimeUnit.SECONDS.sleep(1);
		}
	}
}

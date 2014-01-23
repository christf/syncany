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
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import com.turn.ttorrent.client.Client;
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

		Client client;

		SharedTorrent torrent = SharedTorrent.fromFile(new File("test-http.torrent"), new File("./download/"));

		client = new Client(obtainInetAddress(), torrent);

		try {
			System.out.println("Start to download: " + torrent.getName());
			client.share(); // SEEDING for completion signal
			// client.download() // DONE for completion signal

			while (!ClientState.SEEDING.equals(client.getState())) {
				// Check if there's an error
				if (ClientState.ERROR.equals(client.getState())) {
					throw new Exception("ttorrent client Error State");
				}

				// Display statistics
				System.out.printf("%f %% - %d bytes downloaded - %d bytes uploaded", torrent.getCompletion(), torrent.getDownloaded(),
						torrent.getUploaded());
				System.out.println(" " + torrent.getFilenames());
				// Wait one second
				TimeUnit.SECONDS.sleep(1);
			}

			// client.waitForCompletion();
		}
		catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

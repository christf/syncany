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
import java.util.logging.Logger;

/**
 * @author christof
 *
 */
public class NetworkHelper {
	private static final Logger logger = Logger.getLogger(BtTransferManager.class.getSimpleName());

	public InetAddress obtainInetAddress() throws SocketException {
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
		interfaces = NetworkInterface.getNetworkInterfaces();
		logger.severe("could not find a suitable network interface to use, assuming test and returning local address");
		for (NetworkInterface interface_ : Collections.list(interfaces)) {
			logger.info("probing interface: " + interface_);
			if (interface_.isLoopback()) {
				Enumeration<InetAddress> addresses = interface_.getInetAddresses();
				for (InetAddress address : Collections.list(addresses)) {
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
					return address;
				}
			}
		}
		return null;
	}
}

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
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

/**
 * @author christof
 *
 */

public class Port {
	public enum Protocol {
		TCP, UDP
	};

	private static final Logger logger = Logger.getLogger(Port.class.getSimpleName());

	private GatewayDevice gatewayDevice;
	private InetAddress localAddress;
	private String externalIPAddress;

	public void init() throws IOException, SAXException, ParserConfigurationException {
		GatewayDiscover gatewayDiscover = new GatewayDiscover();
		logger.info("Looking for Gateway Devices");
		gatewayDiscover.discover();
		gatewayDevice = gatewayDiscover.getValidGateway();
		if (null != gatewayDevice) {
			logger.log(Level.INFO, "Gateway device found.\n{0} ({1})",
					new Object[] { gatewayDevice.getModelName(), gatewayDevice.getModelDescription() });
		}
		else {
			logger.info("No valid gateway device found.");
		}
		localAddress = gatewayDevice.getLocalAddress();
		logger.log(Level.INFO, "Using local address: {0}", localAddress);

		try {
			externalIPAddress = gatewayDevice.getExternalIPAddress();
		}
		catch (IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("unable to determine external IP-address of gateway device");
		}
		logger.log(Level.INFO, "External address: {0}", externalIPAddress);
	}

	public void unmap(int port, Protocol protocol) throws IOException, SAXException {
		PortMappingEntry portMapping = new PortMappingEntry();

		logger.log(Level.INFO, "Attempting to map port {0}", port);

		if (gatewayDevice.getSpecificPortMappingEntry(port, protocol.name(), portMapping)) {
			gatewayDevice.deletePortMapping(port, "TCP");
			logger.info("Port mapping removed");
		}
		else {
			logger.info("Port mapping removal failed as port was not mapped in the first place");
		}
	}

	public void map(int port, Protocol protocol) throws IOException, SAXException {
		PortMappingEntry portMapping = new PortMappingEntry();

		logger.log(Level.INFO, "Attempting to map port {0}", port);
		logger.log(Level.INFO, "Querying device to see if mapping for port {0} already exists", port);

		if (!gatewayDevice.getSpecificPortMappingEntry(port, protocol.name(), portMapping)) {
			logger.info("Sending port mapping request");

			if (gatewayDevice.addPortMapping(port, port, localAddress.getHostAddress(), protocol.name(), "syncany-plugins-bt")) {
				logger.log(Level.INFO, "Mapping port " + port + " was succesful");
			}
		}
	}
}
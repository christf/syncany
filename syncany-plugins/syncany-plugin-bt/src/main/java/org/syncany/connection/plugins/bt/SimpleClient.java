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

/**
 * @author christof
 *
 */

import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.Client.ClientState;
import com.turn.ttorrent.client.SharedTorrent;

public class SimpleClient {
	public static final String DEFAULT_TRACKER_URI = "http://localhost:6969/announce";

	/**
	 * Display program usage on the given {@link PrintStream}.
	 *
	 */
	private static void usage(PrintStream s) {
		s.println("usage: SimpleClient [options] torrent");
		s.println("Leech and seed this torrent file.");
		s.println();
		s.println("Available options:");
		s.println(" -h,--help                                 Show this help and exit.");
		s.println(" -o,--output        DIR                        Output directory for file.");
		s.println();
	}

	/**
	 * Main program function.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		/*
		 * // CmdLineParser parser = new CmdLineParser(); // CmdLineParser.Option help = parser.addBooleanOption('h', "help"); //CmdLineParser.Option
		 * outputString = parser.addStringOption('o', "output");
		 * 
		 * // try { // parser.parse(args); // } // catch (CmdLineParser.OptionException oe) { // System.err.println(oe.getMessage()); //
		 * usage(System.err); // System.exit(1); // }
		 * 
		 * // Display help and exit if requested // if (Boolean.TRUE.equals((Boolean) parser.getOptionValue(help))) { usage(System.out);
		 * System.exit(0); }
		 * 
		 * // Get options File output = new File((String) parser.getOptionValue(outputString, "."));
		 * 
		 * // Check that it's the correct usage String[] otherArgs = parser.getRemainingArgs(); if (otherArgs.length != 1) { usage(System.err);
		 * System.exit(1); }
		 */
		// Get the .torrent file path
		File torrentPath = new File(args[0]);

		// Start downloading file
		try {
			SharedTorrent torrent = SharedTorrent.fromFile(torrentPath, new File(args[1]));
			System.out.println("Starting client for torrent: " + torrent.getName());
			Client client = new Client(InetAddress.getLocalHost(), torrent);

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
					System.out.printf("%f %% - %d bytes downloaded - %d bytes uploaded\n", torrent.getCompletion(), torrent.getDownloaded(),
							torrent.getUploaded());

					// Wait one second
					TimeUnit.SECONDS.sleep(1);
				}

				System.out.println("download completed.");
			}
			catch (Exception e) {
				System.err.println("An error occurs...");
				e.printStackTrace(System.err);
			}
			finally {
				System.out.println("stop client.");
				client.stop();
			}
		}
		catch (Exception e) {
			System.err.println("An error occurs...");
			e.printStackTrace(System.err);
		}
	}
}

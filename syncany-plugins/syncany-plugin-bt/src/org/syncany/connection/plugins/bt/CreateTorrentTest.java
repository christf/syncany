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
import java.util.ArrayList;

/**
 * @author christof
 *
 */
public class CreateTorrentTest {
	public static void main(String[] args) {
		CreateTorrent t = new CreateTorrent();
		ArrayList<File> files = new ArrayList<File>();
		files.add(new File("torrentdata/Piece.class"));
		files.add(new File("torrentdata/ttorrent-1.5-SNAPSHOT.jar"));
		System.out.println(t.create("test-http.torrent", "http://kdserv.dyndns.org:6969/announce", files));
	}
}

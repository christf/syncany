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
//@Test
// TODO [medium] Use @Test instead
package org.syncany.tests.plugin.bt;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;

import org.junit.Test;

// TODO [medium] For tests, you can use the TestFileUtil methods to create test files

/**
 * @author christof
 *
 */
public class TorrentCreatorTest {

	@Test
	public void testTorrentCreate() throws Exception {
		// TorrentCreator t = new TorrentCreator();
		ArrayList<File> files = new ArrayList<File>();
		files.add(new File("src/log4j.properties"));
		files.add(new File("src/main/java/org/syncany/connection/plugins/bt/Port.java"));
		String infohash = new String();
		// infohash = new String(t.create("test-http.torrent", "http://kdserv.dyndns.org:6969/announce", files));
		assertEquals("F0B2E69FC4B58007D48A9E573B25C1F4E6C62B7F", infohash);
	}
}

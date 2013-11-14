/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.database.Database;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RenameFileWithDiffModifiedDateScenarioTest {
	@Test
	public void testChangedModifiedDate() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		
		// A, create two files with identical content and change mod. date of one of them
		clientA.createNewFile("A-file1.jpg", 50*1024);
		clientA.copyFile("A-file1.jpg", "A-file1-with-different-modified-date.jpg");		
		clientA.getLocalFile("A-file1.jpg").setLastModified(0);				
		clientA.up();		
		
		// B, down, then move BOTH files
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientB.moveFile("A-file1.jpg", "A-file1-moved.jpg");
		clientB.moveFile("A-file1-with-different-modified-date.jpg", "A-file1-with-different-modified-date-moved.jpg");				
		clientB.up();
		
		Database clientDatabaseB = clientB.loadLocalDatabase();
		
		PartialFileHistory file1Orig = clientDatabaseB.getFileHistory("A-file1-moved.jpg");
		PartialFileHistory file1WithDiffLastModDate = clientDatabaseB.getFileHistory("A-file1-with-different-modified-date-moved.jpg");
		
		assertNotNull(file1Orig);
		assertNotNull(file1WithDiffLastModDate);
		
		FileVersion fileVersion1OrigV1 = file1Orig.getFileVersion(1);
		FileVersion fileVersion1OrigV2 = file1Orig.getFileVersion(2);
		
		FileVersion fileVersion1WithDiffLastModDateV1 = file1WithDiffLastModDate.getFileVersion(1);
		FileVersion fileVersion1WithDiffLastModDateV2 = file1WithDiffLastModDate.getFileVersion(2);
		
		assertNotNull(fileVersion1OrigV1);
		assertNotNull(fileVersion1OrigV2);
		assertNotNull(fileVersion1WithDiffLastModDateV1);
		assertNotNull(fileVersion1WithDiffLastModDateV2);
		
		assertEquals("A-file1.jpg", fileVersion1OrigV1.getName());
		assertEquals("A-file1-moved.jpg", fileVersion1OrigV2.getName());
		assertEquals("A-file1-with-different-modified-date.jpg", fileVersion1WithDiffLastModDateV1.getName());
		assertEquals("A-file1-with-different-modified-date-moved.jpg", fileVersion1WithDiffLastModDateV2.getName());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}

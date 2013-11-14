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
package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.scenarios.AllFilePossibilitiesScenarioTest;
import org.syncany.tests.scenarios.CallUpWhileStillWritingFileScenarioTest;
import org.syncany.tests.scenarios.ChangedAttributesScenarioTest;
import org.syncany.tests.scenarios.ChangedTypeScenarioTest;
import org.syncany.tests.scenarios.CreateSimilarFileParallelScenarioTest;
import org.syncany.tests.scenarios.DirtyDatabaseScenarioTest;
import org.syncany.tests.scenarios.EmptyFileScenarioTest;
import org.syncany.tests.scenarios.EmptyFolderScenarioTest;
import org.syncany.tests.scenarios.EvilCUpWithoutDownScenarioTest;
import org.syncany.tests.scenarios.FileLockedScenarioTest;
import org.syncany.tests.scenarios.FileTreeMoveToSubfolderScenarioTest;
import org.syncany.tests.scenarios.FileVanishedScenarioTest;
import org.syncany.tests.scenarios.FilenameCapitalizationWindowsScenarioTest;
import org.syncany.tests.scenarios.ManyRenamesScenarioTest;
import org.syncany.tests.scenarios.ManySyncUpsAndDatabaseFileCleanupTest;
import org.syncany.tests.scenarios.ManySyncUpsAndOtherClientSyncDownTest;
import org.syncany.tests.scenarios.MixedUpDownScenarioTest;
import org.syncany.tests.scenarios.RenameDeleteScenarioTest;
import org.syncany.tests.scenarios.RenameFileWithDiffModifiedDateScenarioTest;
import org.syncany.tests.scenarios.RenameNoDownloadMultiChunksScenarioTest;
import org.syncany.tests.scenarios.RenameToExistingFileScenarioTest;
import org.syncany.tests.scenarios.SingleFileNoConflictsScenarioTest;
import org.syncany.tests.scenarios.SingleFolderNoConflictsScenarioTest;
import org.syncany.tests.scenarios.SymlinkSyncScenarioTest;
import org.syncany.tests.scenarios.ThreeClientsOneLoserScenarioTest;

@RunWith(Suite.class)
@SuiteClasses({
	AllFilePossibilitiesScenarioTest.class,
	CallUpWhileStillWritingFileScenarioTest.class,
	ChangedAttributesScenarioTest.class,
	RenameFileWithDiffModifiedDateScenarioTest.class,
	ChangedTypeScenarioTest.class,
	CreateSimilarFileParallelScenarioTest.class,
	DirtyDatabaseScenarioTest.class,
	EmptyFileScenarioTest.class,
	EmptyFolderScenarioTest.class,
	EvilCUpWithoutDownScenarioTest.class,
	FileLockedScenarioTest.class,
	FilenameCapitalizationWindowsScenarioTest.class,
	FileTreeMoveToSubfolderScenarioTest.class,
	FileVanishedScenarioTest.class,
	ManyRenamesScenarioTest.class,
	ManySyncUpsAndDatabaseFileCleanupTest.class,
	ManySyncUpsAndOtherClientSyncDownTest.class,
	MixedUpDownScenarioTest.class,
	RenameToExistingFileScenarioTest.class,
	RenameNoDownloadMultiChunksScenarioTest.class,
	RenameDeleteScenarioTest.class,
	SingleFileNoConflictsScenarioTest.class,
	SingleFolderNoConflictsScenarioTest.class,
	SymlinkSyncScenarioTest.class,
	ThreeClientsOneLoserScenarioTest.class
})
public class ScenarioTests {
	// This class executes all tests
}

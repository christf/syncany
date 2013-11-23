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
package org.syncany.operations;

import org.syncany.config.Config;
import org.syncany.database.Database;
import org.syncany.operations.DownOperation.DownOperationOptions;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.operations.UpOperation.UpOperationOptions;
import org.syncany.operations.UpOperation.UpOperationResult;

public class SyncOperation extends Operation {
	private Database loadedDatabase;
	private SyncOperationOptions options;
	
	public SyncOperation(Config config) {
		this(config, null, new SyncOperationOptions());
	}	
	
	public SyncOperation(Config config, Database database, SyncOperationOptions options) {
		super(config);		
		
		this.loadedDatabase = database;
		this.options = options;
	}		
	
	@Override
	public SyncOperationResult execute() throws Exception {
		DownOperation syncDown = new DownOperation(config, loadedDatabase, options.getSyncDownOptions());
		UpOperation syncUp = new UpOperation(config, loadedDatabase, options.getSyncUpOptions());
		
		DownOperationResult syncDownResults = syncDown.execute();
		UpOperationResult syncUpResults = syncUp.execute();
		
		return new SyncOperationResult(syncDownResults, syncUpResults);
	}
	
	public static class SyncOperationOptions implements OperationOptions {
		private UpOperationOptions syncUpOptions = new UpOperationOptions();
		private DownOperationOptions syncDownOptions = new DownOperationOptions();

		public DownOperationOptions getSyncDownOptions() {
			return syncDownOptions;
		}
		
		public void setSyncDownOptions(DownOperationOptions syncDownOptions) {
			this.syncDownOptions = syncDownOptions;
		}				
		
		public UpOperationOptions getSyncUpOptions() {
			return syncUpOptions;
		}
		
		public void setSyncUpOptions(UpOperationOptions syncUpOptions) {
			this.syncUpOptions = syncUpOptions;
		}		
	}
	
	public class SyncOperationResult implements OperationResult {
		private DownOperationResult syncDownResult;
		private UpOperationResult syncUpResult;
		
		public SyncOperationResult(DownOperationResult syncDownResult, UpOperationResult syncUpResult) {
			this.syncDownResult = syncDownResult;
			this.syncUpResult = syncUpResult;
		}

		public DownOperationResult getSyncDownResult() {
			return syncDownResult;
		}		
		
		public UpOperationResult getSyncUpResult() {
			return syncUpResult;
		}				
	}
}

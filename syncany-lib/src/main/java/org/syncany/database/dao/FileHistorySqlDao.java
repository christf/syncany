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
package org.syncany.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock;

/**
 * @author pheckel
 *
 */
public class FileHistorySqlDao extends AbstractSqlDao {
	protected static final Logger logger = Logger.getLogger(FileHistorySqlDao.class.getSimpleName());

	private FileVersionSqlDao fileVersionDao;	
	
	public FileHistorySqlDao(Connection connection, FileVersionSqlDao fileVersionDao) {
		super(connection);		
		this.fileVersionDao = fileVersionDao;		
	}

	public void writeFileHistories(Connection connection, long databaseVersionId, Collection<PartialFileHistory> fileHistories) throws SQLException {
		for (PartialFileHistory fileHistory : fileHistories) {
			PreparedStatement preparedStatement = getStatement(connection, "/sql/filehistory.insert.all.writeFileHistories.sql");

			preparedStatement.setString(1, fileHistory.getFileId().toString());
			preparedStatement.setLong(2, databaseVersionId);

			preparedStatement.executeUpdate();
			preparedStatement.close();

			fileVersionDao.writeFileVersions(connection, fileHistory.getFileId(), databaseVersionId, fileHistory.getFileVersions().values());
		}
	}

	public void removeDirtyFileHistories() throws SQLException {
		PreparedStatement preparedStatement = getStatement("/sql/filehistory.delete.dirty.removeDirtyFileHistories.sql");
		preparedStatement.executeUpdate();
		preparedStatement.close();		
	}

	/**
	 * Note: Also selects versions marked as {@link DatabaseVersionStatus#DIRTY DIRTY}
	 */
	public List<PartialFileHistory> getFileHistoriesWithFileVersions(VectorClock databaseVersionVectorClock) {
		try (PreparedStatement preparedStatement = getStatement("/sql/filehistory.select.all.getFileHistoriesWithFileVersionsByVectorClock.sql")) {
			preparedStatement.setString(1, databaseVersionVectorClock.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createFileHistoriesFromResult(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<PartialFileHistory> getFileHistoriesWithFileVersions() {
		try (PreparedStatement preparedStatement = getStatement("/sql/filehistory.select.master.getFileHistoriesWithFileVersions.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return createFileHistoriesFromResult(resultSet);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected List<PartialFileHistory> createFileHistoriesFromResult(ResultSet resultSet) throws SQLException {
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();;
		PartialFileHistory fileHistory = null;

		while (resultSet.next()) {
			FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
			FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));

			if (fileHistory != null && fileHistory.getFileId().equals(fileHistoryId)) { // Same history!
				fileHistory.addFileVersion(lastFileVersion);
			}
			else { // New history!
				fileHistory = new PartialFileHistory(fileHistoryId);
				fileHistory.addFileVersion(lastFileVersion);
			}
			
			fileHistories.add(fileHistory);
		}

		return fileHistories;
	}

	public PartialFileHistory getFileHistoryWithLastVersion(FileHistoryId fileHistoryId) {
		FileVersion lastFileVersion = fileVersionDao.getFileVersionByFileHistoryId(fileHistoryId);

		if (lastFileVersion != null) {
			PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
			fileHistory.addFileVersion(lastFileVersion);

			return fileHistory;
		}

		return null;
	}

	public PartialFileHistory getFileHistoryWithLastVersion(String relativePath) {
		try (PreparedStatement preparedStatement = getStatement("/sql/filehistory.select.master.getFileHistoryWithLastVersion.sql")) {
			preparedStatement.setString(1, relativePath);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
	
					PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
					fileHistory.addFileVersion(lastFileVersion);
	
					return fileHistory;
				}
			}

			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersion() {
		List<PartialFileHistory> fileHistories = new ArrayList<PartialFileHistory>();

		try (PreparedStatement preparedStatement = getStatement("/sql/filehistory.select.master.getFileHistoriesWithLastVersion.sql")) {
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
	
					PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
					fileHistory.addFileVersion(lastFileVersion);
	
					fileHistories.add(fileHistory);
				}
			}

			return fileHistories;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<PartialFileHistory> getFileHistoriesWithLastVersionByChecksum(FileChecksum fileContentChecksum) {
		List<PartialFileHistory> currentFileTree = new ArrayList<PartialFileHistory>();

		try (PreparedStatement preparedStatement = getStatement("/sql/filehistory.select.master.getFileHistoriesWithLastVersionByChecksum.sql")) {
			preparedStatement.setString(1, fileContentChecksum.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					FileHistoryId fileHistoryId = FileHistoryId.parseFileId(resultSet.getString("filehistory_id"));
					FileVersion lastFileVersion = fileVersionDao.createFileVersionFromRow(resultSet);
	
					PartialFileHistory fileHistory = new PartialFileHistory(fileHistoryId);
					fileHistory.addFileVersion(lastFileVersion);
	
					currentFileTree.add(fileHistory);
				}	
			}

			return currentFileTree;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}

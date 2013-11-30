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
package org.syncany.database.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.syncany.chunk.Transformer;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.ChunkEntry;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.database.mem.MemChunkEntry;
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.database.mem.MemDatabaseVersion;
import org.syncany.database.mem.MemFileContent;
import org.syncany.database.mem.MemFileVersion;
import org.syncany.database.mem.MemPartialFileHistory;
import org.syncany.database.mem.MemMultiChunkEntry;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SqlDatabaseDAO implements DatabaseDAO {
	private static final Logger logger = Logger.getLogger(SqlDatabaseDAO.class.getSimpleName());

	public SqlDatabaseDAO() {

	}
	
	@Override
	public void save(Database db, File destinationFile) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void save(Database db, DatabaseVersion versionFrom, DatabaseVersion versionTo, File destinationFile) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load(Database db, File databaseFile) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load(Database db, File databaseFile, boolean headersOnly) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion, boolean headersOnly) throws IOException {
		// TODO Auto-generated method stub
		
	}

}

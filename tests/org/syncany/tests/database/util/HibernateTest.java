package org.syncany.tests.database.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.syncany.database.ChunkEntry;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.mem.MemChunkEntry.ChunkEntryId;
import org.syncany.database.sql.SqlChunkEntry;
import org.syncany.database.sql.ChunkIdEntity;
import org.syncany.database.sql.DAO;
import org.syncany.database.sql.SqlDatabaseVersionEntity;
import org.syncany.database.sql.SqlDatabaseVersionHeaderEntity;
import org.syncany.database.sql.SqlFileContentEntity;
import org.syncany.database.sql.SqlFileVersionEntity;
import org.syncany.database.sql.SqlMultiChunkEntry;
import org.syncany.database.sql.SqlPartialFileHistoryEntity;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

public class HibernateTest {

	@Test
	public void testWriteReadDatabaseVersionHibernate() throws InterruptedException {
		DAO<SqlDatabaseVersionEntity> dao = new DAO<SqlDatabaseVersionEntity>(SqlDatabaseVersionEntity.class);
		Random rand = new Random();

		SqlDatabaseVersionEntity databaseVersion = new SqlDatabaseVersionEntity();

		//Create DBV 
		
		SqlDatabaseVersionHeaderEntity header = new SqlDatabaseVersionHeaderEntity();
		header.setClient("8");
		Date setDate = new Date();
		header.setDate(setDate);
		VectorClock vectorClock = new VectorClock();
		vectorClock.setClock("A", 1);
		header.setVectorClock(vectorClock);
		databaseVersion.setHeader(header);

		List<SqlChunkEntry> chunks = new ArrayList<SqlChunkEntry>();
		SqlMultiChunkEntry multiChunk = new SqlMultiChunkEntry(TestFileUtil.createRandomArray(10));
		SqlFileContentEntity fileContent = new SqlFileContentEntity(TestFileUtil.createRandomArray(rand.nextInt(100)), 512);
		
		//Generate Chunks
		for(int i = 0; i < 20; i++) {
			SqlChunkEntry chunk = new SqlChunkEntry(TestFileUtil.createRandomArray(rand.nextInt(100) ), 512);
			chunks.add(chunk);
			
			fileContent.addChunk(new ChunkEntryId(chunk.getChecksum()));

			databaseVersion.addChunk(chunk);
			multiChunk.addChunk(new ChunkEntryId(chunk.getChecksum()));
			if(i % 9 == 0) {
				databaseVersion.addMultiChunk(multiChunk);
				multiChunk = new SqlMultiChunkEntry(TestFileUtil.createRandomArray(10)); 
			}
		}
		databaseVersion.addMultiChunk(multiChunk);
		databaseVersion.addFileContent(fileContent);
		
		//Generate FileVersion + FileHistory
		
		SqlPartialFileHistoryEntity fileHistory = new SqlPartialFileHistoryEntity(rand.nextLong());
		
		for(long i = 1; i < 10; i++) {
			SqlFileVersionEntity fileVersion = createFileVersionEntity();
			fileVersion.setVersion(Long.valueOf(i));
			fileVersion.setPath("Path Version - " + i);
			fileHistory.addFileVersion(fileVersion);	
		}
		
		databaseVersion.addFileHistory(fileHistory);
		
		databaseVersion = dao.save(databaseVersion);
		Thread.sleep(5000);
		SqlDatabaseVersionEntity persisted = dao.get((SqlDatabaseVersionHeaderEntity)databaseVersion.getHeader());
	
		persisted = dao.save(persisted);
		
		assertEquals(databaseVersion, persisted);
		
		assertEquals(databaseVersion.getChunks().size(), persisted.getChunks().size());
		
		assertEquals(databaseVersion.getMultiChunks().size(), persisted.getMultiChunks().size());
		
		assertEquals(databaseVersion.getFileContents().size(), persisted.getFileContents().size());
		
		checkChunkOrderInFileContent(chunks, persisted);
			
		List<SqlDatabaseVersionEntity> entities = dao.getAll();
		
		for (SqlDatabaseVersionEntity databaseVersionEntity : entities) {
			System.out.println("Header : " + databaseVersionEntity.getHeader());
			for(ChunkEntry chunk : databaseVersionEntity.getChunks()) {
				System.out.println("	Chunk : " + StringUtil.toHex(chunk.getChecksum()));
			}
			
			System.out.println("MultiChunks");
			for (MultiChunkEntry multiChunkEntry : databaseVersionEntity.getMultiChunks()) {
				System.out.println("MultiChunk: " + StringUtil.toHex(multiChunkEntry.getId()));
				for (ChunkEntryId chunkId : multiChunkEntry.getChunks()) {
					System.out.println("	ChunkId: " +  StringUtil.toHex(chunkId.getArray()));
				}
			}
			
			System.out.println("FileContents");
			for (FileContent filecontent : databaseVersionEntity.getFileContents()) {
				System.out.println("FileContent: " + StringUtil.toHex(filecontent.getChecksum()));
				for (ChunkEntryId chunkId : filecontent.getChunks()) {
					System.out.println("	ChunkId: " +  StringUtil.toHex(chunkId.getArray()));
				}
			}
			
			System.out.println("FileHistories");
			for (PartialFileHistory entity : databaseVersionEntity.getFileHistories()) {
				System.out.println("File ID: " + entity.getFileId());
				System.out.println("FileVersions: ");
				for (FileVersion version : entity.getFileVersions().values()) {
					System.out.println(version.getPath() + "; checksum: " + StringUtil.toHex(version.getChecksum()));
				}
			}
		}
	}

	private void checkChunkOrderInFileContent(List<SqlChunkEntry> chunks, SqlDatabaseVersionEntity persisted) {
		Collection<FileContent> persistedFileContents = persisted.getFileContents();
		Collection<ChunkEntryId> persistedContentChunks = persistedFileContents.iterator().next().getChunks();
		for (int i = 0; i < persistedFileContents.size(); i++) {
			ChunkEntryId persistedChunk = persistedContentChunks.iterator().next();
			assertEquals(StringUtil.toHex(chunks.get(i).getChecksum()), StringUtil.toHex(persistedChunk.getArray()));
		}
	}

	@Test
	public void testWriteReadChunkEntity() throws InterruptedException {
		DAO<SqlChunkEntry> dao = new DAO<SqlChunkEntry>(SqlChunkEntry.class);
		
		Random rand = new Random();

		SqlChunkEntry chunk = new SqlChunkEntry(TestFileUtil.createRandomArray(rand.nextInt(100) ), 512);
		chunk = dao.save(chunk);
		Thread.sleep(5000);
		SqlChunkEntry persisted = dao.getById(StringUtil.toHex(chunk.getChecksum()));
		
		assertEquals(chunk,persisted);
		
		List<SqlChunkEntry> entities = dao.getAll();
		
		for (SqlChunkEntry entity : entities) {
			System.out.println(entity.getChecksum());
		}
	}
	
	@Test
	public void testWriteReadMultiChunkEntity() throws InterruptedException {
		DAO<SqlMultiChunkEntry> dao = new DAO<SqlMultiChunkEntry>(SqlMultiChunkEntry.class);
		
		Random rand = new Random();

		SqlMultiChunkEntry multiChunk = new SqlMultiChunkEntry(TestFileUtil.createRandomArray(rand.nextInt(100)));
		
		for(int i = 0; i < 1; i++) {
			multiChunk.addChunk(new ChunkIdEntity(TestFileUtil.createRandomArray(rand.nextInt(100))));
		}
		
		multiChunk = dao.save(multiChunk);
		Thread.sleep(5000);
		SqlMultiChunkEntry persisted = dao.getById(multiChunk.getIdEncoded());
		
		assertEquals(multiChunk.getIdEncoded(),persisted.getIdEncoded());
		assertEquals(multiChunk.getChunks().size(),persisted.getChunks().size());
		
		List<SqlMultiChunkEntry> entities = dao.getAll();
		
		for (SqlMultiChunkEntry entity : entities) {
			System.out.println(entity.getIdEncoded());
		}
	}
	
	@Test
	public void testWriteReadFileVersionEntity() throws InterruptedException {
		DAO<SqlFileVersionEntity> dao = new DAO<SqlFileVersionEntity>(SqlFileVersionEntity.class);
		

		SqlFileVersionEntity fileVersion = createFileVersionEntity();
		
		fileVersion = dao.save(fileVersion);
		Thread.sleep(5000);
		SqlFileVersionEntity persisted = dao.getById(fileVersion.getChecksumEncoded());
		
		assertEquals(persisted,fileVersion);

		List<SqlFileVersionEntity> entities = dao.getAll();
		
		for (SqlFileVersionEntity entity : entities) {
			System.out.println(entity.getChecksumEncoded());
		}
	}

	private SqlFileVersionEntity createFileVersionEntity() {
		SqlFileVersionEntity fileVersion = new SqlFileVersionEntity();
		
		Random rand = new Random();

		fileVersion.setChecksum(TestFileUtil.createRandomArray(rand.nextInt(100)));
		fileVersion.setCreatedBy("EvilC");
		fileVersion.setStatus(FileStatus.DELETED);
		fileVersion.setType(FileType.FOLDER);
		fileVersion.setPath("Path");
		fileVersion.setVersion(1L);
		return fileVersion;
	}
	
	@Test
	public void testWriteReadPartialFileHistoryEntity() throws InterruptedException {
		DAO<SqlPartialFileHistoryEntity> dao = new DAO<SqlPartialFileHistoryEntity>(SqlPartialFileHistoryEntity.class);
		
		Random rand = new Random();

		SqlPartialFileHistoryEntity fileHistory = new SqlPartialFileHistoryEntity(rand.nextLong());
		
		for(long i = 1; i < 10; i++) {
			SqlFileVersionEntity fileVersion = createFileVersionEntity();
			fileVersion.setVersion(Long.valueOf(i));
			fileVersion.setPath("Path Version - " + i);
			fileHistory.addFileVersion(fileVersion);	
		}
		
		fileHistory = dao.save(fileHistory);
		Thread.sleep(5000);
		SqlPartialFileHistoryEntity persisted = dao.getById(fileHistory.getFileId());
		
		assertEquals(persisted, fileHistory);

		Collection<FileVersion> persistedFileVersions = persisted.getFileVersions().values();
		Collection<FileVersion> originalFileVersions = fileHistory.getFileVersions().values();
		for (int i = 0; i < persistedFileVersions.size(); i++) {
			assertEquals(persistedFileVersions.iterator().next().getVersion(), originalFileVersions.iterator().next().getVersion());
		}
		
		List<SqlPartialFileHistoryEntity> entities = dao.getAll();
		
		for (SqlPartialFileHistoryEntity entity : entities) {
			System.out.println("File ID: " + entity.getFileId());
			System.out.println("FileVersions: ");
			for (FileVersion version : entity.getFileVersions().values()) {
				System.out.println(version.getPath() + "; checksum: " + StringUtil.toHex(version.getChecksum()));
			}
		}
	}
	
	
}

package org.syncany.tests.database.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DAO;
import org.syncany.database.persistence.ChunkEntity;
import org.syncany.database.persistence.DatabaseVersionEntity;
import org.syncany.database.persistence.DatabaseVersionHeaderEntity;
import org.syncany.database.persistence.FileContentEntity;
import org.syncany.database.persistence.FileVersionEntity;
import org.syncany.database.persistence.IChunkEntry;
import org.syncany.database.persistence.IFileContent;
import org.syncany.database.persistence.IFileVersion;
import org.syncany.database.persistence.IFileVersion.FileStatus;
import org.syncany.database.persistence.IFileVersion.FileType;
import org.syncany.database.persistence.IMultiChunkEntry;
import org.syncany.database.persistence.IPartialFileHistory;
import org.syncany.database.persistence.MultiChunkEntity;
import org.syncany.database.persistence.PartialFileHistoryEntity;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

public class HibernateTest {

	@Test
	public void testWriteReadDatabaseVersionHibernate() throws InterruptedException {
		DAO<DatabaseVersionEntity> dao = new DAO<DatabaseVersionEntity>(DatabaseVersionEntity.class);
		Random rand = new Random();

		DatabaseVersionEntity databaseVersion = new DatabaseVersionEntity();

		//Create DBV 
		
		DatabaseVersionHeaderEntity header = new DatabaseVersionHeaderEntity();
		header.setClient("8");
		Date setDate = new Date();
		header.setDate(setDate);
		VectorClock vectorClock = new VectorClock();
		vectorClock.setClock("A", 1);
		header.setVectorClock(vectorClock);
		databaseVersion.setHeader(header);

		List<ChunkEntity> chunks = new ArrayList<ChunkEntity>();
		MultiChunkEntity multiChunk = new MultiChunkEntity(TestFileUtil.createRandomArray(10));
		FileContentEntity fileContent = new FileContentEntity(TestFileUtil.createRandomArray(rand.nextInt(100)), 512);
		
		//Generate Chunks
		for(int i = 0; i < 20; i++) {
			ChunkEntity chunk = new ChunkEntity(TestFileUtil.createRandomArray(rand.nextInt(100) ), 512);
			chunks.add(chunk);
			
			fileContent.addChunk(chunk);
			databaseVersion.addChunk(chunk);
			multiChunk.addChunk(chunk);
			if(i % 9 == 0) {
				databaseVersion.addMultiChunk(multiChunk);
				multiChunk = new MultiChunkEntity(TestFileUtil.createRandomArray(10)); 
			}
		}
		databaseVersion.addMultiChunk(multiChunk);
		databaseVersion.addFileContent(fileContent);
		
		//Generate FileVersion + FileHistory
		
		PartialFileHistoryEntity fileHistory = new PartialFileHistoryEntity(rand.nextLong());
		
		for(long i = 1; i < 10; i++) {
			FileVersionEntity fileVersion = createFileVersionEntity();
			fileVersion.setVersion(Long.valueOf(i));
			fileVersion.setPath("Path Version - " + i);
			fileHistory.addFileVersion(fileVersion);	
		}
		
		databaseVersion.addFileHistory(fileHistory);
		
		databaseVersion = dao.save(databaseVersion);
		Thread.sleep(5000);
		DatabaseVersionEntity persisted = dao.get((DatabaseVersionHeaderEntity)databaseVersion.getHeader());
	
		assertEquals(databaseVersion, persisted);
		
		assertEquals(databaseVersion.getChunks().size(), persisted.getChunks().size());
		
		assertEquals(databaseVersion.getMultiChunks().size(), persisted.getMultiChunks().size());
		
		assertEquals(databaseVersion.getFileContents().size(), persisted.getFileContents().size());
		
		checkChunkOrderInFileContent(chunks, persisted);
			
		List<DatabaseVersionEntity> entities = dao.getAll();
		
		for (DatabaseVersionEntity databaseVersionEntity : entities) {
			System.out.println("Header : " + databaseVersionEntity.getHeader());
			for(IChunkEntry chunk : databaseVersionEntity.getChunks()) {
				System.out.println("	Chunk : " + StringUtil.toHex(chunk.getChecksum()));
			}
			
			System.out.println("MultiChunks");
			for (IMultiChunkEntry multiChunkEntry : databaseVersionEntity.getMultiChunks()) {
				System.out.println("MultiChunk: " + StringUtil.toHex(multiChunkEntry.getId()));
				for (ChunkEntryId chunkId : multiChunkEntry.getChunks()) {
					System.out.println("	ChunkId: " +  StringUtil.toHex(chunkId.getArray()));
				}
			}
			
			System.out.println("FileContents");
			for (IFileContent filecontent : databaseVersionEntity.getFileContents()) {
				System.out.println("FileContent: " + StringUtil.toHex(filecontent.getChecksum()));
				for (ChunkEntryId chunkId : filecontent.getChunks()) {
					System.out.println("	ChunkId: " +  StringUtil.toHex(chunkId.getArray()));
				}
			}
			
			System.out.println("FileHistories");
			for (IPartialFileHistory entity : databaseVersionEntity.getFileHistories()) {
				System.out.println("File ID: " + entity.getFileId());
				System.out.println("FileVersions: ");
				for (IFileVersion version : entity.getFileVersions().values()) {
					System.out.println(version.getPath() + "; checksum: " + StringUtil.toHex(version.getChecksum()));
				}
			}
		}
	}

	private void checkChunkOrderInFileContent(List<ChunkEntity> chunks, DatabaseVersionEntity persisted) {
		Collection<IFileContent> persistedFileContents = persisted.getFileContents();
		Collection<ChunkEntryId> persistedContentChunks = persistedFileContents.iterator().next().getChunks();
		for (int i = 0; i < persistedFileContents.size(); i++) {
			ChunkEntryId persistedChunk = persistedContentChunks.iterator().next();
			assertEquals(StringUtil.toHex(chunks.get(i).getChecksum()), StringUtil.toHex(persistedChunk.getArray()));
		}
	}

	@Test
	public void testWriteReadChunkEntity() throws InterruptedException {
		DAO<ChunkEntity> dao = new DAO<ChunkEntity>(ChunkEntity.class);
		
		Random rand = new Random();

		ChunkEntity chunk = new ChunkEntity(TestFileUtil.createRandomArray(rand.nextInt(100) ), 512);
		chunk = dao.save(chunk);
		Thread.sleep(5000);
		ChunkEntity persisted = dao.getById(StringUtil.toHex(chunk.getChecksum()));
		
		assertEquals(chunk,persisted);
		
		List<ChunkEntity> entities = dao.getAll();
		
		for (ChunkEntity entity : entities) {
			System.out.println(entity.getChecksum());
		}
	}
	
	@Test
	public void testWriteReadMultiChunkEntity() throws InterruptedException {
		DAO<MultiChunkEntity> dao = new DAO<MultiChunkEntity>(MultiChunkEntity.class);
		
		Random rand = new Random();

		MultiChunkEntity multiChunk = new MultiChunkEntity(TestFileUtil.createRandomArray(rand.nextInt(100)));
		
		for(int i = 0; i < 1; i++) {
			multiChunk.addChunk(new ChunkEntity(TestFileUtil.createRandomArray(rand.nextInt(100)), 512));
		}
		
		multiChunk = dao.save(multiChunk);
		Thread.sleep(5000);
		MultiChunkEntity persisted = dao.getById(multiChunk.getIdEncoded());
		
		assertEquals(multiChunk.getIdEncoded(),persisted.getIdEncoded());
		assertEquals(multiChunk.getChunks().size(),persisted.getChunks().size());
		
		List<MultiChunkEntity> entities = dao.getAll();
		
		for (MultiChunkEntity entity : entities) {
			System.out.println(entity.getIdEncoded());
		}
	}
	
	@Test
	public void testWriteReadFileVersionEntity() throws InterruptedException {
		DAO<FileVersionEntity> dao = new DAO<FileVersionEntity>(FileVersionEntity.class);
		

		FileVersionEntity fileVersion = createFileVersionEntity();
		
		fileVersion = dao.save(fileVersion);
		Thread.sleep(5000);
		FileVersionEntity persisted = dao.getById(fileVersion.getChecksumEncoded());
		
		assertEquals(persisted,fileVersion);

		List<FileVersionEntity> entities = dao.getAll();
		
		for (FileVersionEntity entity : entities) {
			System.out.println(entity.getChecksumEncoded());
		}
	}

	private FileVersionEntity createFileVersionEntity() {
		FileVersionEntity fileVersion = new FileVersionEntity();
		
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
		DAO<PartialFileHistoryEntity> dao = new DAO<PartialFileHistoryEntity>(PartialFileHistoryEntity.class);
		
		Random rand = new Random();

		PartialFileHistoryEntity fileHistory = new PartialFileHistoryEntity(rand.nextLong());
		
		for(long i = 1; i < 10; i++) {
			FileVersionEntity fileVersion = createFileVersionEntity();
			fileVersion.setVersion(Long.valueOf(i));
			fileVersion.setPath("Path Version - " + i);
			fileHistory.addFileVersion(fileVersion);	
		}
		
		fileHistory = dao.save(fileHistory);
		Thread.sleep(5000);
		PartialFileHistoryEntity persisted = dao.getById(fileHistory.getFileId());
		
		assertEquals(persisted, fileHistory);

		Collection<IFileVersion> persistedFileVersions = persisted.getFileVersions().values();
		Collection<IFileVersion> originalFileVersions = fileHistory.getFileVersions().values();
		for (int i = 0; i < persistedFileVersions.size(); i++) {
			assertEquals(persistedFileVersions.iterator().next().getVersion(), originalFileVersions.iterator().next().getVersion());
		}
		
		List<PartialFileHistoryEntity> entities = dao.getAll();
		
		for (PartialFileHistoryEntity entity : entities) {
			System.out.println("File ID: " + entity.getFileId());
			System.out.println("FileVersions: ");
			for (IFileVersion version : entity.getFileVersions().values()) {
				System.out.println(version.getPath() + "; checksum: " + StringUtil.toHex(version.getChecksum()));
			}
		}
	}
	
	
}

package org.syncany.tests.database.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DAO;
import org.syncany.database.persistence.ChunkEntity;
import org.syncany.database.persistence.DatabaseVersionEntity;
import org.syncany.database.persistence.DatabaseVersionHeaderEntity;
import org.syncany.tests.util.TestFileUtil;

public class HibernateTest {

	@Test
	public void testWriteReadDatabaseVersionHibernate() {
		DAO<DatabaseVersionEntity> dao = new DAO<DatabaseVersionEntity>(DatabaseVersionEntity.class);

		DatabaseVersionEntity entity = new DatabaseVersionEntity();

		DatabaseVersionHeaderEntity header = new DatabaseVersionHeaderEntity();
		header.setClient("8");
		Date setDate = new Date();
		header.setDate(setDate);
		VectorClock vectorClock = new VectorClock();
		vectorClock.setClock("A", 1);
		header.setVectorClock(vectorClock);
		entity.setHeader(header);

		entity = dao.save(entity);
		DatabaseVersionEntity persisted = dao.get(entity.getHeader());
	
		assertEquals(entity, persisted);
		
		List<DatabaseVersionEntity> entities = dao.getAll();
		
		for (DatabaseVersionEntity databaseVersionEntity : entities) {
			System.out.println(databaseVersionEntity.getHeader());
		}
	}

	@Test
	public void testWriteReadChunkEntity() {
		DAO<ChunkEntity> dao = new DAO<ChunkEntity>(ChunkEntity.class);
		
		ChunkEntity chunk = new ChunkEntity(TestFileUtil.createRandomArray(512), 512);
		chunk = dao.save(chunk);
		
	}

	
}

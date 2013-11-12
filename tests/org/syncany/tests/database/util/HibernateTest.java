package org.syncany.tests.database.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.syncany.database.DatabaseVersionEntity;
import org.syncany.database.DatabaseVersionHeaderEntity;
import org.syncany.database.SimpleEntity;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseVersionDAO;
import org.syncany.database.dao.SimpleEntityDAO;

public class HibernateTest {

	@Test
	public void testWriteHibernate() {
		SimpleEntityDAO dao = new SimpleEntityDAO();

		SimpleEntity entity = new SimpleEntity();
		entity.setName("BLAA");
		SimpleEntity persisted = dao.save(entity);

		assertTrue(persisted.getId() != 0);
	}

	@Test
	public void testWriteReadHibernate() {
		SimpleEntityDAO dao = new SimpleEntityDAO();

		SimpleEntity entity = new SimpleEntity();
		entity.setName("BLAA2222");

		entity = dao.save(entity);
		SimpleEntity persisted = dao.get(entity.getId());

		assertEquals(entity, persisted);
	}

	@Test
	public void testReadHibernate() {
		SimpleEntityDAO dao = new SimpleEntityDAO();

		List<SimpleEntity> result = dao.getAll();

		for (SimpleEntity simpleEntity : result) {
			System.out.println("ID: " + simpleEntity.getId() + " Name: " + simpleEntity.getName());
		}
	}

	@Test
	public void testWriteReadDatabaseVersionHibernate() {
		DatabaseVersionDAO dao = new DatabaseVersionDAO();

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

}

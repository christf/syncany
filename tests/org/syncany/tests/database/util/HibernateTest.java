package org.syncany.tests.database.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;
import org.syncany.database.SimpleEntity;
import org.syncany.database.dao.SimpleEntityDAO;
import org.syncany.database.util.HibernateUtil;

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

		assertEquals(entity,persisted);
	}
	
	@Test
	public void testReadHibernate() {
		SimpleEntityDAO dao = new SimpleEntityDAO();

		Session session = HibernateUtil.getSessionFactory().openSession();

		List<SimpleEntity> result = dao.getAll();
		
		for (SimpleEntity simpleEntity : result) {
			System.out.println("ID: " + simpleEntity.getId() + " Name: " + simpleEntity.getName());
		}
		session.close();
	}
		
}

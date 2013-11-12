package org.syncany.tests.database.util;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;
import org.syncany.database.SimpleEntity;
import org.syncany.database.util.HibernateUtil;

public class HibernateTest {

	@Test
	public void testWriteHibernate() {
		Session session = HibernateUtil.getSessionFactory().openSession();

		Transaction transaction = session.beginTransaction();
		SimpleEntity simpleEntity = new SimpleEntity();
		simpleEntity.setName("BLAA");
		session.save(simpleEntity);
		transaction.commit();
		session.close();

	}
	
	@Test
	public void testWriteReadHibernate() {
		Session session = HibernateUtil.getSessionFactory().openSession();

		Transaction transaction = session.beginTransaction();
		SimpleEntity entity = new SimpleEntity();
		entity.setName("BLAA");
		session.save(entity);
		transaction.commit();

		transaction = session.beginTransaction();
		SimpleEntity readEntity = (SimpleEntity) session.load(SimpleEntity.class, entity.getId());
		
		assertEquals(entity,readEntity);
		session.close();
	}
}

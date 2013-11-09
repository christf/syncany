package org.syncany.tests.database.util;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;
import org.syncany.database.ChunkEntity;
import org.syncany.database.util.HibernateUtil;

public class HibernateTest {

	@Test
	public void testHibernate() {
		Session session = HibernateUtil.getSessionFactory().openSession();

		Transaction transaction = session.beginTransaction();
		ChunkEntity chunk = new ChunkEntity("CONTENT".getBytes(),10);
		session.save(chunk);
		transaction.commit();
		session.close();

	}
}

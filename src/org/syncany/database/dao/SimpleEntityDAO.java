package org.syncany.database.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.syncany.database.SimpleEntity;
import org.syncany.database.util.HibernateUtil;

public class SimpleEntityDAO extends DAO{

	public SimpleEntity save(SimpleEntity entity) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaction = session.beginTransaction();
		
		session.save(entity);
		transaction.commit();
		
		session.flush();
		session.close();
		
		return entity;
	}
	
	public SimpleEntity get(SimpleEntity entity) {
		return get(entity.getId());
	}
	
	public SimpleEntity get(long id) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		SimpleEntity readEntity = (SimpleEntity) session.get(SimpleEntity.class, id);
		session.close();
		return readEntity;
	}
	
	@SuppressWarnings("unchecked")
	public List<SimpleEntity> getAll() {
		return (List<SimpleEntity>)(List<?>) super.getAll(SimpleEntity.class);
	}
}

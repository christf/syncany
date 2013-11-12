package org.syncany.database.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.syncany.database.DatabaseVersionEntity;
import org.syncany.database.DatabaseVersionHeaderEntity;
import org.syncany.database.SimpleEntity;
import org.syncany.database.util.HibernateUtil;

public class DatabaseVersionDAO extends DAO{

	public DatabaseVersionEntity save(DatabaseVersionEntity entity) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaction = session.beginTransaction();
		
		session.save(entity);
		transaction.commit();
		
		session.flush();
		session.close();
		
		return entity;
	}
	
	public DatabaseVersionEntity get(DatabaseVersionHeaderEntity entity) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		DatabaseVersionEntity readEntity = (DatabaseVersionEntity) session.get(DatabaseVersionEntity.class, entity);
		session.close();
		return readEntity;
	}
	
	@SuppressWarnings("unchecked")
	public List<DatabaseVersionEntity> getAll() {
		return (List<DatabaseVersionEntity>)(List<?>) super.getAll(DatabaseVersionEntity.class);
	}
}

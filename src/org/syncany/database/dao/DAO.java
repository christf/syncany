package org.syncany.database.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.syncany.database.persistence.DatabaseVersionEntity;
import org.syncany.database.util.PersistenceUtil;

public class DAO<T>{
	
    private Class<T> type;
    
    public DAO(Class<T> daoClass) {
    	this.type = daoClass;
    }
	
	public T save(T entity) {
		Session session = PersistenceUtil.getSessionFactory().openSession();
		Transaction transaction = session.beginTransaction();
		
		session.saveOrUpdate(entity);
		transaction.commit();
		
		session.flush();
		session.close();
		
		return entity;
	}
	
	@SuppressWarnings("unchecked")
	public T get(Serializable entity) {
		Session session = PersistenceUtil.getSessionFactory().openSession();
	
		T readEntity = (T) session.get(DatabaseVersionEntity.class, entity);
		session.close();
		return readEntity;
	}
	
	@SuppressWarnings("unchecked")
	public T getById(Serializable id) {
		Session session = PersistenceUtil.getSessionFactory().openSession();
		
		T readEntity = (T) session.get(this.type, id);
		
		session.close();
		return readEntity;
	}
	
	@SuppressWarnings("unchecked")
	public List<T> getAll() {
		List<T> objects = null;
		Session session = PersistenceUtil.getSessionFactory().openSession();

		session.beginTransaction();
		
		Query query = session.createQuery("from " + type.getName());
		objects = query.list();

		session.close();
		return objects;
	}
	
}

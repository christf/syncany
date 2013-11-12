package org.syncany.database.dao;

import java.util.List;

import org.hibernate.Session;
import org.syncany.database.util.HibernateUtil;

public abstract class DAO {
	
	protected List<Object> getAll(Class<?> clazz) {
		List<Object> objects = null;
		Session session = HibernateUtil.getSessionFactory().openSession();

		session.beginTransaction();
		objects = session.createQuery("from " + clazz.getName()).list();

		session.close();
		return objects;
	}
	
}

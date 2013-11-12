package org.syncany.database.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	private static SessionFactory sessionFactory;
	private static Configuration config;
	
	static {
		try {
			config = new Configuration();
		} catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		if(sessionFactory == null || sessionFactory.isClosed()) {
			sessionFactory = config.configure().buildSessionFactory();
		}
		return sessionFactory;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		sessionFactory.close();
	}
}
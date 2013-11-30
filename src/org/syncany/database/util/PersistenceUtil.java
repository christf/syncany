package org.syncany.database.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

public class PersistenceUtil {
	private static ServiceRegistry serviceRegistry;
	private static SessionFactory sessionFactory;
	
	static {
		try {
			Configuration configuration = new Configuration().configure();

            serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		}
		catch (Exception ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {		
		return sessionFactory;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		sessionFactory.close();
	}
}

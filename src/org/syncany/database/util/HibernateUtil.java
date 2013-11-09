package org.syncany.database.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
	   private static final SessionFactory sessionFactory;
	   static {
	      try {
	    	  Configuration config = new Configuration();
	    	  sessionFactory = config.configure().buildSessionFactory();

		   
		   } catch (Throwable ex) {
		   System.err.println("Initial SessionFactory creation failed." + ex);
		   throw new ExceptionInInitializerError(ex);
	      }
	   }

	   public static SessionFactory getSessionFactory() {
	      return sessionFactory;
	   }
	}
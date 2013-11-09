package org.syncany.database.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hsqldb.jdbcDriver;

public class DatabaseConnectionFactory {
	private static Connection c;
	
	static {
	    try {
	        DriverManager.registerDriver(new jdbcDriver());
	    } catch (Exception e) {}
	}
	
	public static Connection initDatabaseConnection() throws SQLException {
		c = DriverManager.getConnection("jdbc:hsqldb:file:testdb;hsqldb.write_delay=false;shutdown=true", "SA", "");	
		return c;
	}
	
	public static Connection getDatabaseConnection() throws SQLException {
		if(c == null || c.isClosed()) {
			return initDatabaseConnection();
		}
		return c;
	}
	
	public static ResultSet executeStatement(String query) throws SQLException {
		if(c == null) {
			return null;
		}
		Statement s = c.createStatement();
		ResultSet response = s.executeQuery(query);
		s.close();
		return response;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		c.close();
	}
}

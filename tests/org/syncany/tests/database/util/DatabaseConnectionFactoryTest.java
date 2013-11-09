package org.syncany.tests.database.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;
import org.syncany.database.util.DatabaseConnectionFactory;

public class DatabaseConnectionFactoryTest {
	@Test
	public void testGetDatabaseConnection() throws IOException, SQLException {		
		Connection c = DatabaseConnectionFactory.getDatabaseConnection();
		String createTableSQL = "CREATE TABLE CHUNK("
				+ "USER_ID VARCHAR(5) NOT NULL, "
				+ "USERNAME VARCHAR(20) NOT NULL"
				+ ")";
		
		DatabaseConnectionFactory.executeStatement(createTableSQL);
		
		String insertQuery = "INSERT INTO CHUNK VALUES ('test','testtttt')";

		DatabaseConnectionFactory.executeStatement(insertQuery);
		
		
		String selectFrom = "SELECT USER_ID, USERNAME FROM CHUNK";
		
		ResultSet response = DatabaseConnectionFactory.executeStatement(selectFrom);
		while(response.next()) {
			System.out.println(response.getString("USER_ID") + ";" + response.getString("USERNAME"));	
		}
		c.commit();
		response.close();
		c.close();
	}
	
	@Test
	public void testReadTestDatabase() throws IOException, SQLException {		
		Connection c = DatabaseConnectionFactory.getDatabaseConnection();
		
		String createTableSQL = "SELECT USER_ID, USERNAME FROM CHUNK";
		
		ResultSet response = DatabaseConnectionFactory.executeStatement(createTableSQL);
		while(response.next()) {
			System.out.println(response.getString("USER_ID") + ";" + response.getString("USERNAME"));	
		}
		response.close();
		c.close();
	}
	
}

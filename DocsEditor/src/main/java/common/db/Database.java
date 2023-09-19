package common.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Database {
	
	public Connection getConnection() {
		Connection connection = null;
		try {
			InitialContext initContext = new InitialContext();
			DataSource ds = (DataSource) initContext.lookup("java:/comp/env/jdbc/postgres");
			connection = ds.getConnection();
		} catch (NamingException e) {
			System.out.println("Catched Naming Exception " + e.getMessage());
		} catch (SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		}
		return connection;
	}
}

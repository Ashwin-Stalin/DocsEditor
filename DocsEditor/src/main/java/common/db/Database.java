package common.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Database {
	private static Database shared = null;
	private final static Object lock = new Object();
	private Connection connection = null;
	private Database() {
		try {
			InitialContext initContext = new InitialContext();
			DataSource ds = (DataSource) initContext.lookup("java:/comp/env/jdbc/postgres");
			connection = ds.getConnection();
		}catch(NamingException e) {
			System.out.println("Catched Naming Exception " + e.getMessage());
		}
		catch(SQLException e) {
			System.out.println("Catched SQL Exception " + e.getMessage());
		}
	}
	
	public static Database getInstance() {
		if(Database.shared == null) {
			synchronized(lock) {
				if(Database.shared == null)
					Database.shared = new Database();
			}
		}
		return Database.shared;
	}
	public Connection getConnection() {
		return this.connection;
	}
}

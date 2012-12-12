package pl.shockah.shocky.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pl.shockah.shocky.Data;

public class SQL {
	private static Connection conn = null;
	public static Map<String,PreparedStatement> statements = new HashMap<String,PreparedStatement>();

	public static void raw(String query) {execute(query);}
	public static ResultSet select(QuerySelect query) {return executeQuery(query.getSQLQuery());}
	public static void delete(QueryDelete query) {execute(query.getSQLQuery());}
	public static int update(QueryUpdate query) {return updateResultSet(query.getSQLQuery());}
	
	public static void init() {
		try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Class.forName("pl.shockah.shocky.sql.Criterion");
            Class.forName("pl.shockah.shocky.sql.CriterionNumber");
            Class.forName("pl.shockah.shocky.sql.CriterionString");
            Class.forName("pl.shockah.shocky.sql.Wildcard");
            Class.forName("pl.shockah.shocky.sql.Factoid");
            Class.forName("pl.shockah.shocky.sql.Query");
            Class.forName("pl.shockah.shocky.sql.QueryDelete");
            Class.forName("pl.shockah.shocky.sql.QueryInsert");
            Class.forName("pl.shockah.shocky.sql.QueryUpdate");
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
	}
	
	public synchronized static Connection getSQLConnection() {
		try {
			if (conn == null || !conn.isValid(1)) {
				if (conn != null && !statements.isEmpty()) {
					Iterator<PreparedStatement> iter = statements.values().iterator();
					while(iter.hasNext()) {
						PreparedStatement p = iter.next();
						p.close();
						iter.remove();
					}
				}
				conn = DriverManager.getConnection(String.format("jdbc:mysql://%s/%s?user=%s&password=%s&useUnicode=true&characterEncoding=utf-8",
						Data.config.getString("main-sqlhost"),
						Data.config.getString("main-sqldb"),
						Data.config.getString("main-sqluser"),
						Data.config.getString("main-sqlpass")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			conn = null;
		}
		return conn;
	}
	
	public static ResultSet executeQuery(String query) {
		Statement s = null;
		try {
			s = getSQLConnection().createStatement();
			return s.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void execute(String query) {
		Statement s = null;
		try {
			s = getSQLConnection().createStatement();
			s.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static int updateResultSet(String query) {
		Statement s = null;
		try {
			s = getSQLConnection().createStatement();
			return s.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (s != null)
					s.close();
			} catch (SQLException e) {
			}
		}
		return 0;
	}
	
	public static int insert(QueryInsert query) {
		PreparedStatement s = null;
		try {
			s = query.getSQLQuery(getSQLConnection());
			return s.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (s != null)
					s.close();
			} catch (SQLException e) {
			}
		}
		return 0;
	}
	
	public static String getTable(String name) {
		return Data.config.getString("main-sqlprefix")+name;
	}
}
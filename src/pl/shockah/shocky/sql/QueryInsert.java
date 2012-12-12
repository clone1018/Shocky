package pl.shockah.shocky.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class QueryInsert extends Query {
	private final String table;
	private final String[] keys = new String[8];
	private final Object[] values = new Object[8];
	private int inspos = 0;
	
	public QueryInsert(String table) {
		this.table = table;
	}
	
	public void add(String column, Object value) {
		int pos = inspos;
		boolean inc = true;
		for (int i = 0; i <keys.length; i++) {
			if (keys[i] != null && keys[i].equals(column)) {
				pos = i;
				inc = false;
				break;
			}
		}
		keys[pos] = column;
		values[pos] = value;
		if (inc)
			inspos++;
	}
	
	public String getSQLQuery() {
		String clauseColumns = getColumnsClause(Arrays.asList(keys));
		String clauseValues = getValuesObjectClause(Arrays.asList(values));
		return "INSERT INTO "+table+" ("+clauseColumns+") VALUES("+clauseValues+")";
	}
	
	public PreparedStatement getSQLQuery(Connection con) {
		StringBuilder sb = new StringBuilder("INSERT INTO ");
		sb.append(table);
		sb.append(" (");
		
		int i = 0;
		for (; i < keys.length; i++) {
			if (keys[i] == null) break;
			if (i > 0) sb.append(',');
			sb.append(keys[i]);
		}
		sb.append(" ) VALUES(");
		for (; i>=1;i--) {
			sb.append('?');
			if (i > 1) sb.append(',');
		}
		sb.append(')');
		
		PreparedStatement p = null;
		i=0;
		try {
			p = con.prepareStatement(sb.toString());
			for (; i < values.length; i++) {
				if (keys[i] == null) break;
				p.setObject(i+1, values[i]);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return p;
	}
}
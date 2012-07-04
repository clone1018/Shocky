package pl.shockah.shocky.sql;

import java.util.*;

public class QueryInsert extends Query {
	private final String table;
	private final Map<String,Object> values = new HashMap<String,Object>();
	
	public QueryInsert(String table) {
		this.table = table;
	}
	
	public void add(String column, Object value) {
		values.put(column, value);
	}
	
	public String getSQLQuery() {
		String clauseColumns = getColumnsClause(values.keySet());
		String clauseValues = getValuesObjectClause(values.values());
		return "INSERT INTO "+table+" ("+clauseColumns+") VALUES("+clauseValues+")";
	}
}
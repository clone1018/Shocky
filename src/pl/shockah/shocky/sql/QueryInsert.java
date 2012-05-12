package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class QueryInsert extends Query {
	private String table;
	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<Object> values = new ArrayList<Object>();
	
	public QueryInsert(String table) {
		this.table = table;
	}
	
	public void addColumns(String... columns) {
		this.columns.addAll(Arrays.asList(columns));
	}
	public void addValues(Object... values) {
		this.values.addAll(Arrays.asList(values));
	}
	public void add(ImmutablePair<String,Object>... pairs) {
		for (ImmutablePair<String,Object> pair : pairs) add(pair.getLeft(),pair.getRight());
	}
	public void add(String column, Object value) {
		columns.add(column);
		values.add(value);
	}
	
	public String getSQLQuery() {
		String clauseColumns = getColumnsClause(columns);
		String clauseValues = getValuesObjectClause(values);
		return "INSERT INTO "+table+" ("+clauseColumns+") VALUES("+clauseValues+")";
	}
}
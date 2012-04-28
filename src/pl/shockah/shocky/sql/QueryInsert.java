package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import pl.shockah.Pair;

public class QueryInsert extends Query {
	private static String getColumnsClause(ArrayList<String> list) {
		if (list == null || list.isEmpty()) return "*";
		StringBuilder sb = new StringBuilder();
		
		for (String s : list) {
			if (sb.length() != 0) sb.append(",");
			sb.append(s);
		}
		
		return sb.toString();
	}
	private static String getValuesClause(ArrayList<Object> list) {
		StringBuilder sb = new StringBuilder();
		
		for (Object o : list) {
			if (sb.length() != 0) sb.append(",");
			if (o instanceof String) sb.append("'"+((String)o).replaceAll(Pattern.quote("\"'\n\\"),"\\$0")+"'");
			else sb.append(o);
		}
		
		return sb.toString();
	}
	
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
	public void add(Pair<String,Object>... pairs) {
		for (Pair<String,Object> pair : pairs) add(pair.get1(),pair.get2());
	}
	public void add(String column, Object value) {
		columns.add(column);
		values.add(value);
	}
	
	public String getSQLQuery() {
		String clauseColumns = getColumnsClause(columns);
		String clauseValues = getValuesClause(values);
		return "INSERT INTO "+table+" ("+clauseColumns+") VALUES("+clauseValues+")";
	}
}
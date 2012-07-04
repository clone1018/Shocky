package pl.shockah.shocky.sql;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public abstract class Query {
	protected static String getColumnsClause(Collection<String> list) {
		if (list == null || list.isEmpty()) return "*";
		StringBuilder sb = new StringBuilder();
		
		for (String s : list) {
			if (sb.length() != 0) sb.append(',');
			sb.append(s);
		}
		
		return sb.toString();
	}
	protected static String getValuesPairClause(Map<String,Object> list) {
		StringBuilder sb = new StringBuilder();
		
		for (Entry<String, Object> pair : list.entrySet()) {
			if (sb.length() != 0) sb.append(',');
			sb.append(pair.getKey());
			sb.append('=');
			Object value = pair.getValue();
			if (value instanceof String)
				sb.append('\'').append(value.toString().replace("\\","\\\\").replace("'","\\'")).append('\'');
			else
				sb.append(value);
		}
		
		return sb.toString();
	}
	protected static String getValuesObjectClause(Collection<Object> list) {
		StringBuilder sb = new StringBuilder();
		
		for (Object o : list) {
			if (sb.length() != 0) sb.append(',');
			if (o instanceof String)
				sb.append('\'').append(o.toString().replace("\\","\\\\").replace("'","\\'")).append('\'');
			else
				sb.append(o);
		}
		
		return sb.toString();
	}
	protected static String getOrderByClause(Map<String,Boolean> list) {
		if (list == null || list.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder("ORDER BY ");
		int i = 0;
		for (Entry<String, Boolean> pair : list.entrySet()) {
			if (i > 0) sb.append(',');
			sb.append(pair.getKey());
			sb.append(' ');
			sb.append(pair.getValue() ? "ASC" : "DESC");
			i++;
		}
		
		return sb.toString();
	}
	
	public static String getWhereClause(Collection<Criterion> list) {
		if (list == null || list.isEmpty()) return "";
		StringBuilder sb = new StringBuilder("WHERE ");
		
		int i = 0;
		for (Criterion c : list) {
			if (i > 0) sb.append(" AND ");
			sb.append(c);
			i++;
		}
		
		return sb.toString();
	}
	
	public abstract String getSQLQuery();
}
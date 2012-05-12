package pl.shockah.shocky.sql;

import java.util.ArrayList;
import org.apache.commons.lang3.tuple.ImmutablePair;

public abstract class Query {
	protected static String getColumnsClause(ArrayList<String> list) {
		if (list == null || list.isEmpty()) return "*";
		StringBuilder sb = new StringBuilder();
		
		for (String s : list) {
			if (sb.length() != 0) sb.append(",");
			sb.append(s);
		}
		
		return sb.toString();
	}
	protected static String getValuesPairClause(ArrayList<ImmutablePair<String,Object>> list) {
		StringBuilder sb = new StringBuilder();
		
		for (ImmutablePair<String,Object> pair : list) {
			if (sb.length() != 0) sb.append(",");
			sb.append(pair.left+"=");
			if (pair.right instanceof String) sb.append("'"+((String)pair.right).replace("\\","\\\\").replace("'","\\'")+"'");
			else sb.append(pair.right);
		}
		
		return sb.toString();
	}
	protected static String getValuesObjectClause(ArrayList<Object> list) {
		StringBuilder sb = new StringBuilder();
		
		for (Object o : list) {
			if (sb.length() != 0) sb.append(",");
			if (o instanceof String) sb.append("'"+((String)o).replace("\\","\\\\").replace("'","\\'")+"'");
			else sb.append(o);
		}
		
		return sb.toString();
	}
	protected static String getOrderByClause(ArrayList<ImmutablePair<String,Boolean>> list) {
		if (list == null || list.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		
		for (ImmutablePair<String,Boolean> pair : list) {
			if (sb.length() != 0) sb.append(",");
			sb.append(pair.left+(pair.right ? "" : " DESC"));
		}
		
		sb.insert(0,"ORDER BY ");
		return sb.toString();
	}
	
	public abstract String getSQLQuery();
}
package pl.shockah.shocky.sql;

import java.util.ArrayList;

public class Criterion {
	public static String getWhereClause(ArrayList<Criterion> list) {
		if (list == null || list.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		
		for (Criterion c : list) {
			if (sb.length() != 0) sb.append(" AND ");
			sb.append(c);
		}
		
		sb.insert(0,"WHERE ");
		return sb.toString();
	}
	
	private String raw;
	
	public Criterion(String raw) {
		this.raw = raw;
	}
	
	public String toString() {
		return raw;
	}
}
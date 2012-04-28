package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;

public class QuerySelect extends Query {
	private static String getColumnsClause(ArrayList<String> list) {
		if (list == null || list.isEmpty()) return "*";
		StringBuilder sb = new StringBuilder();
		
		for (String s : list) {
			if (sb.length() != 0) sb.append(",");
			sb.append(s);
		}
		
		return sb.toString();
	}
	
	private String table;
	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<Criterion> criterions = new ArrayList<Criterion>();
	private int limitOffset = 0, limitCount = 1;
	
	public QuerySelect(String table) {
		this.table = table;
	}
	
	public void addColumns(String... columns) {
		this.columns.addAll(Arrays.asList(columns));
	}
	public void addCriterions(Criterion... criterions) {
		this.criterions.addAll(Arrays.asList(criterions));
	}
	
	public void setLimitOffset(int offset) {limitOffset = offset;}
	public void setLimitCount(int count) {limitCount = count;}
	public void setLimit(int offset, int count) {
		setLimitOffset(offset);
		setLimitCount(count);
	}
	
	public String getSQLQuery() {
		String clauseColumns = getColumnsClause(columns);
		String clauseWhere = Criterion.getWhereClause(criterions);
		String clauseLimit = limitOffset == 0 && limitCount == 1 ? "" : "LIMIT "+(limitOffset != 0 ? ""+limitOffset+"," : "")+limitCount;
		return "SELECT "+clauseColumns+" FROM "+table+(clauseWhere.isEmpty() ? "" : " "+clauseWhere)+(clauseLimit.isEmpty() ? "" : " "+clauseLimit);
	}
}
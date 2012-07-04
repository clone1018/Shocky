package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class QuerySelect extends Query {
	private final String table;
	private final ArrayList<String> columns = new ArrayList<String>();
	private final ArrayList<Criterion> criterions = new ArrayList<Criterion>();
	private final Map<String,Boolean> orderby = new HashMap<String,Boolean>();
	private int limitOffset = 0, limitCount = -1;
	
	public QuerySelect(String table) {
		this.table = table;
	}
	
	public void addColumns(String... columns) {
		this.columns.addAll(Arrays.asList(columns));
	}
	public void addCriterions(Criterion... criterions) {
		this.criterions.addAll(Arrays.asList(criterions));
	}
	public void addOrder(String column) {addOrder(column,true);}
	public void addOrder(String column, boolean ascending) {
		orderby.put(column,ascending);
	}
	
	public void setLimitOffset(int offset) {limitOffset = offset;}
	public void setLimitCount(int count) {limitCount = count;}
	public void setLimit(int offset, int count) {
		setLimitOffset(offset);
		setLimitCount(count);
	}
	
	public String getSQLQuery() {
		String clauseColumns = getColumnsClause(columns);
		String clauseWhere = getWhereClause(criterions);
		String clauseOrderBy = getOrderByClause(orderby);
		String clauseLimit = limitOffset == 0 && limitCount == -1 ? "" : "LIMIT "+(limitOffset != 0 ? ""+limitOffset+"," : "")+limitCount;
		return "SELECT "+clauseColumns+" FROM "+table+(clauseWhere.isEmpty() ? "" : " "+clauseWhere)+(clauseOrderBy.isEmpty() ? "" : " "+clauseOrderBy)+(clauseLimit.isEmpty() ? "" : " "+clauseLimit);
	}
}
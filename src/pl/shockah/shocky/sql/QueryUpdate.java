package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class QueryUpdate extends Query {
	private final String table;
	private final Map<String,Object> values = new HashMap<String,Object>();
	private final ArrayList<Criterion> criterions = new ArrayList<Criterion>();
	private final Map<String,Boolean> orderby = new HashMap<String,Boolean>();
	private int limitOffset = 0, limitCount = 1;
	
	public QueryUpdate(String table) {
		this.table = table;
	}
	
	public void set(String column, Object value) {
		values.put(column,value);
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
		String clauseValues = getValuesPairClause(values);
		String clauseWhere = getWhereClause(criterions);
		String clauseOrderBy = getOrderByClause(orderby);
		String clauseLimit = limitOffset == 0 && limitCount == 1 ? "" : "LIMIT "+(limitOffset != 0 ? ""+limitOffset+"," : "")+limitCount;
		return "UPDATE "+table+" SET "+clauseValues+(clauseWhere.isEmpty() ? "" : " "+clauseWhere)+(clauseOrderBy.isEmpty() ? "" : " "+clauseOrderBy)+(clauseLimit.isEmpty() ? "" : " "+clauseLimit);
	}
}
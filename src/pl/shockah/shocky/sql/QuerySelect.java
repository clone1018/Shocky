package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class QuerySelect extends Query {
	private String table;
	private ArrayList<String> columns = new ArrayList<String>();
	private ArrayList<Criterion> criterions = new ArrayList<Criterion>();
	private ArrayList<ImmutablePair<String,Boolean>> orderby = new ArrayList<ImmutablePair<String,Boolean>>();
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
		orderby.add(new ImmutablePair<String,Boolean>(column,ascending));
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
		String clauseOrderBy = getOrderByClause(orderby);
		String clauseLimit = limitOffset == 0 && limitCount == -1 ? "" : "LIMIT "+(limitOffset != 0 ? ""+limitOffset+"," : "")+limitCount;
		return "SELECT "+clauseColumns+" FROM "+table+(clauseWhere.isEmpty() ? "" : " "+clauseWhere)+(clauseOrderBy.isEmpty() ? "" : " "+clauseOrderBy)+(clauseLimit.isEmpty() ? "" : " "+clauseLimit);
	}
}
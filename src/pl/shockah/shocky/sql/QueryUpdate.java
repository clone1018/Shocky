package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class QueryUpdate extends Query {
	private String table;
	private ArrayList<ImmutablePair<String,Object>> values = new ArrayList<ImmutablePair<String,Object>>();
	private ArrayList<Criterion> criterions = new ArrayList<Criterion>();
	private ArrayList<ImmutablePair<String,Boolean>> orderby = new ArrayList<ImmutablePair<String,Boolean>>();
	private int limitOffset = 0, limitCount = 1;
	
	public QueryUpdate(String table) {
		this.table = table;
	}
	
	public void set(ImmutablePair<String,Object>... pairs) {
		values.addAll(Arrays.asList(pairs));
	}
	public void set(String column, Object value) {
		values.add(new ImmutablePair<String,Object>(column,value));
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
		String clauseValues = getValuesPairClause(values);
		String clauseWhere = Criterion.getWhereClause(criterions);
		String clauseOrderBy = getOrderByClause(orderby);
		String clauseLimit = limitOffset == 0 && limitCount == 1 ? "" : "LIMIT "+(limitOffset != 0 ? ""+limitOffset+"," : "")+limitCount;
		return "UPDATE "+table+" SET "+clauseValues+(clauseWhere.isEmpty() ? "" : " "+clauseWhere)+(clauseOrderBy.isEmpty() ? "" : " "+clauseOrderBy)+(clauseLimit.isEmpty() ? "" : " "+clauseLimit);
	}
}
package pl.shockah.shocky.sql;

import java.util.ArrayList;
import java.util.Arrays;

public class QueryDelete extends Query {
	private String table;
	private ArrayList<Criterion> criterions = new ArrayList<Criterion>();
	private int limitOffset = 0, limitCount = 1;
	
	public QueryDelete(String table) {
		this.table = table;
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
		String clauseWhere = Criterion.getWhereClause(criterions);
		String clauseLimit = limitOffset == 0 && limitCount == 1 ? "" : "LIMIT "+(limitOffset != 0 ? ""+limitOffset+"," : "")+limitCount;
		return "DELETE FROM "+table+(clauseWhere.isEmpty() ? "" : " "+clauseWhere)+(clauseLimit.isEmpty() ? "" : " "+clauseLimit);
	}
}
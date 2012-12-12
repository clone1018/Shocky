package pl.shockah.shocky.sql;

public class CriterionString extends Criterion {
	public CriterionString(String column, String value) {
		this(column,value,true);
	}
	public CriterionString(String column, String value, boolean equals) {
		this(column,(equals ? Operation.Equals : Operation.NotEquals),value);
	}
	
	public CriterionString(String column, Operation o, String value) {
		super((value == null && o==Operation.Equals ? column+" IS NULL" : value == null && o==Operation.NotEquals ? column+" IS NOT NULL" : column+o+"'"+value.replace("\\","\\\\").replace("'","\\'")+"'"));
	}
}
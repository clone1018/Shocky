package pl.shockah.shocky.sql;

public class CriterionStringEquals extends Criterion {
	public CriterionStringEquals(String column, String value) {
		this(column,value,true);
	}
	public CriterionStringEquals(String column, String value, boolean equals) {
		super(column+(equals ? Operation.Equals : Operation.NotEquals)+"'"+value.replace("\\","\\\\").replace("'","\\'")+"'");
	}
}
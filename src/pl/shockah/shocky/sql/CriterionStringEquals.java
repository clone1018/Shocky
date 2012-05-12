package pl.shockah.shocky.sql;

public class CriterionStringEquals extends Criterion {
	public CriterionStringEquals(String column, String value) {
		super(column+"='"+value.replace("\\","\\\\").replace("'","\\'")+"'");
	}
}
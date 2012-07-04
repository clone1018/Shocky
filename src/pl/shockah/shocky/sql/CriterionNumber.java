package pl.shockah.shocky.sql;

public class CriterionNumber extends Criterion {
	public CriterionNumber(String column, Operation o, long value) {
		this(column,o.toString(),value);
	}
	public CriterionNumber(String column, String o, long value) {
		super(column+o+value);
	}
}
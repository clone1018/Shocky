package pl.shockah.shocky.sql;

public class CriterionNumber extends Criterion {
	public CriterionNumber(String column, Operation o, long value) {
		super(column+o+value);
	}
	
	public static enum Operation {
		Equals("="), NotEquals("<>"), Lesser("<"), Greater(">"), LesserOrEqual("<="), GreaterOrEqual(">=");
		
		private String operation;
		
		Operation(String o) {
			operation = o;
		}
		
		public String toString() {
			return operation;
		}
	}
}
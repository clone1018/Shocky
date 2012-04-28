package pl.shockah.shocky.sql;

public class CriterionNumber extends Criterion {
	public CriterionNumber(String column, Operation o, int value) {
		super(column+o+value);
	}
	
	public static enum Operation {
		Equals("="), Lesser("<"), Greater(">"), LesserOrEqual("<="), GreaterOrEqual(">=");
		
		private String operation;
		
		Operation(String o) {
			operation = o;
		}
		
		public String toString() {
			return operation;
		}
	}
}
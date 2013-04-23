package pl.shockah.shocky.sql;

public class Criterion {
	
	private final String raw;
	public boolean useOR = false;
	
	public Criterion(String raw) {
		this.raw = raw;
	}
	
	public Criterion setOR() {
		useOR = true;
		return this;
	}
	
	public String toString() {
		return '('+raw+')';
	}
	
	public static enum Operation {
		Equals("="), NotEquals("<>"), Lesser("<"), Greater(">"), LesserOrEqual("<="), GreaterOrEqual(">="), LIKE(" LIKE "), REGEXP(" REGEXP ");
		
		private final String operation;
		
		Operation(String o) {
			operation = o;
		}
		
		public String toString() {
			return operation;
		}
	}
}
package pl.shockah.shocky.sql;

public class Criterion {
	
	private final String raw;
	
	public Criterion(String raw) {
		this.raw = raw;
	}
	
	public String toString() {
		return raw;
	}
	
	public static enum Operation {
		Equals("="), NotEquals("<>"), Lesser("<"), Greater(">"), LesserOrEqual("<="), GreaterOrEqual(">=");
		
		private final String operation;
		
		Operation(String o) {
			operation = o;
		}
		
		public String toString() {
			return operation;
		}
	}
}
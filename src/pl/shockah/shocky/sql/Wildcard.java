package pl.shockah.shocky.sql;

public class Wildcard extends Criterion {
	public static Wildcard blank = new Wildcard();
	
	public Wildcard(String column, Operation o) {
		this(column,o.toString());
	}
	public Wildcard(String column, String o) {
		super(column+o+'?');
	}
	private Wildcard() {
		super("?");
	}
}

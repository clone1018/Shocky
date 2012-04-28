package pl.shockah.shocky.sql;

import java.util.regex.Pattern;

public class CriterionStringEquals extends Criterion {
	public CriterionStringEquals(String column, String value) {
		super(column+"='"+value.replaceAll(Pattern.quote("\"'\n\\"),"\\$0")+"'");
	}
}
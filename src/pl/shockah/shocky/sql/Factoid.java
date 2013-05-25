package pl.shockah.shocky.sql;
import java.sql.ResultSet;

import org.json.JSONObject;

public final class Factoid {
	public static Factoid fromJSONObject(JSONObject j) {
		try {
			return new Factoid(j.getString("factoid"), j.getString("channel"), j.getString("author"), j.getString("rawtext"), Long.parseLong(j.getString("stamp")) * 1000, j.getString( "locked").equals("1"), j.getString("forgotten").equals("1"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Factoid fromResultSet(ResultSet j) {
		try {
			j.next();
			int row = j.getRow();
			if (row == 0)
				return null;
			return new Factoid(j.getString("factoid"), j.getString("channel"), j.getString("author"), j.getString("rawtext"), Long.parseLong(j.getString("stamp")) * 1000, j.getString( "locked").equals("1"), j.getString("forgotten").equals("1"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public final String name, channel, author, rawtext;
	public final long stamp;
	public final boolean locked, forgotten;

	private Factoid(String name, String channel, String author, String rawtext, long stamp) {
		this(name, channel, author, rawtext, stamp, false, false);
	}

	private Factoid(String name, String channel, String author, String rawtext, long stamp, boolean locked, boolean forgotten) {
		this.name = name;
		this.channel = channel;
		this.author = author;
		this.rawtext = rawtext;
		this.stamp = stamp;
		this.locked = locked;
		this.forgotten = forgotten;
	}

	@Override
	public String toString() {
		return rawtext;
	}
}
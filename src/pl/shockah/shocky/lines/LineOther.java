package pl.shockah.shocky.lines;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import pl.shockah.BinBuffer;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.Wildcard;

public class LineOther extends Line {
	public final String text;
	
	public LineOther(ResultSet result) throws SQLException {
		super(result);
		this.text = result.getString("text");
	}
	
	public LineOther(String channel, String text) {this(new Date(),channel,text);}
	public LineOther(long ms, String channel, String text) {this(new Date(ms),channel,text);}
	public LineOther(Date time, String channel, String text) {
		super(time,channel);
		this.text = text;
	}
	
	public LineOther(BinBuffer buffer) {
		super(buffer);
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return text;
	}

	public boolean containsUser(String user) {
		return false;
	}
	
	@Override
	public void fillQuery(QueryInsert q, boolean prepare) {
		super.fillQuery(q, prepare);
		q.add("text",prepare?Wildcard.blank:text);
	}
	
	public int fillQuery(PreparedStatement p, int arg) throws SQLException {
		arg = super.fillQuery(p,arg);
		p.setString(arg++, text);
		return arg;
	}
}
package pl.shockah.shocky.lines;

import java.util.Date;

import pl.shockah.BinBuffer;
import pl.shockah.shocky.sql.QueryInsert;

public class LineOther extends Line {
	public final String text;
	
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
	public void fillQuery(QueryInsert q) {
		super.fillQuery(q);
		q.add("user","");
		q.add("user2","");
		q.add("txt",text);
	}
}
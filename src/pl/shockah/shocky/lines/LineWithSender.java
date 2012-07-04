package pl.shockah.shocky.lines;

import java.util.Date;

import pl.shockah.BinBuffer;
import pl.shockah.shocky.sql.QueryInsert;

public abstract class LineWithSender extends Line {
	public final String sender;
	
	public LineWithSender(String channel, String sender) {this(new Date(),channel,sender);}
	public LineWithSender(long ms, String channel, String sender) {this(new Date(ms),channel,sender);}
	public LineWithSender(Date time, String channel, String sender) {
		super(time,channel);
		this.sender = sender;
	}
	
	public LineWithSender(BinBuffer buffer) {
		super(buffer);
		this.sender = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(sender);
	}
	
	public boolean containsUser(String user) {
		if (user == null) return true;
		return sender.equalsIgnoreCase(user);
	}
	@Override
	public void fillQuery(QueryInsert q) {
		super.fillQuery(q);
		q.add("user",sender);
		q.add("user2","");
	}
}
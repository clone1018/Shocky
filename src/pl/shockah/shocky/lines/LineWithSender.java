package pl.shockah.shocky.lines;

import java.util.Date;

import pl.shockah.BinBuffer;

public abstract class LineWithSender extends Line {
	public final String sender;
	
	public LineWithSender(String sender) {this(new Date(),sender);}
	public LineWithSender(long ms, String sender) {this(new Date(ms),sender);}
	public LineWithSender(Date time, String sender) {
		super(time);
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
}
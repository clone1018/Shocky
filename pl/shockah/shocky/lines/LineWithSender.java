package pl.shockah.shocky.lines;

import java.util.Date;

public abstract class LineWithSender extends Line {
	public final String sender;
	
	public LineWithSender(String sender) {this(new Date(),sender);}
	public LineWithSender(long ms, String sender) {this(new Date(ms),sender);}
	public LineWithSender(Date time, String sender) {
		super(time);
		this.sender = sender;
	}
}
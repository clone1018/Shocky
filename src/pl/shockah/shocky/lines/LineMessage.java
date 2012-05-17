package pl.shockah.shocky.lines;

import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.BinBuffer;

public class LineMessage extends LineWithSender {
	public final String text;
	
	public LineMessage(String sender, String text) {this(new Date(),sender,text);}
	public LineMessage(long ms, String sender, String text) {this(new Date(ms),sender,text);}
	public LineMessage(MessageEvent<PircBotX> event) {this(new Date(),event.getUser().getNick(),event.getMessage());}
	public LineMessage(Date time, String sender, String text) {
		super(time,sender);
		this.text = text;
	}
	
	public LineMessage(BinBuffer buffer) {
		super(buffer);
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return "<"+sender+"> "+text;
	}
}
package pl.shockah.shocky.lines;

import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.ActionEvent;

import pl.shockah.BinBuffer;

public class LineAction extends LineWithSender {
	public final String text;
	
	public LineAction(String sender, String text) {this(new Date(),sender,text);}
	public LineAction(long ms, String sender, String text) {this(new Date(ms),sender,text);}
	public LineAction(ActionEvent<PircBotX> event) {this(new Date(),event.getUser().getNick(),event.getAction());}
	public LineAction(Date time, String sender, String text) {
		super(time,sender);
		this.text = text;
	}
	
	public LineAction(BinBuffer buffer) {
		super(buffer);
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return "* "+sender+" "+text;
	}
}
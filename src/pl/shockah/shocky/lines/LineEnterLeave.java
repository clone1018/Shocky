package pl.shockah.shocky.lines;

import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.ActionEvent;

import pl.shockah.BinBuffer;

public class LineEnterLeave extends LineWithSender {
	public final String text;
	
	public LineEnterLeave(String sender, String text) {this(new Date(),sender,text);}
	public LineEnterLeave(long ms, String sender, String text) {this(new Date(ms),sender,text);}
	public LineEnterLeave(ActionEvent<PircBotX> event) {this(new Date(),event.getUser().getNick(),event.getAction());}
	public LineEnterLeave(Date time, String sender, String text) {
		super(time,sender);
		this.text = text;
	}
	
	public LineEnterLeave(BinBuffer buffer) {
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
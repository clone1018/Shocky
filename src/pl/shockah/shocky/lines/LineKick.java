package pl.shockah.shocky.lines;

import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.KickEvent;

import pl.shockah.BinBuffer;

public class LineKick extends LineWithSender {
	public final String target;
	public final String text;
	
	public LineKick(String channel, String sender, String target, String text) {this(new Date(),channel,sender,target,text);}
	public LineKick(long ms, String channel, String sender, String target, String text) {this(new Date(ms),channel,sender,target,text);}
	public LineKick(KickEvent<PircBotX> event) {this(new Date(),event.getChannel().getName(),event.getSource().getNick(),event.getRecipient().getNick(),event.getReason());}
	public LineKick(Date time, String channel, String sender, String target, String text) {
		super(time,channel,sender);
		this.target = target;
		this.text = text;
	}
	
	public LineKick(BinBuffer buffer) {
		super(buffer);
		this.target = buffer.readUString();
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(target);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return "* "+sender+" has kicked "+target+" ("+text+")";
	}

	public boolean containsUser(String user) {
		if (super.containsUser(user))
			return true;
		return target.equalsIgnoreCase(user);
	}
}
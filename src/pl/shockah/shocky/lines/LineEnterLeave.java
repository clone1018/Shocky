package pl.shockah.shocky.lines;

import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.ActionEvent;

import pl.shockah.BinBuffer;
import pl.shockah.shocky.sql.QueryInsert;

public class LineEnterLeave extends LineWithSender {
	public final String text;
	
	public LineEnterLeave(String channel, String sender, String text) {this(new Date(),channel,sender,text);}
	public LineEnterLeave(long ms, String channel, String sender, String text) {this(new Date(ms),channel,sender,text);}
	public LineEnterLeave(ActionEvent<PircBotX> event) {this(new Date(),event.getChannel().getName(),event.getUser().getNick(),event.getAction());}
	public LineEnterLeave(Date time, String channel, String sender, String text) {
		super(time,channel,sender);
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
	@Override
	public void fillQuery(QueryInsert q) {
		super.fillQuery(q);
		q.add("txt",text);
	}
}
package pl.shockah.shocky.lines;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.KickEvent;

import pl.shockah.BinBuffer;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.Wildcard;

public class LineKick extends LineWithUsers {
	public final String text;
	
	public LineKick(ResultSet result) throws SQLException {
		super(result,result.getString("users").split(";"));
		this.text = result.getString("text");
	}
	
	public LineKick(String channel, String sender, String target, String text) {this(new Date(),channel,sender,target,text);}
	public LineKick(long ms, String channel, String sender, String target, String text) {this(new Date(ms),channel,sender,target,text);}
	public LineKick(KickEvent<ShockyBot> event) {this(new Date(),event.getChannel().getName(),event.getSource().getNick(),event.getRecipient().getNick(),event.getReason());}
	public LineKick(Date time, String channel, String sender, String target, String text) {
		super(time,channel,new String[] {sender,target});
		this.text = text;
	}
	public LineKick(long ms, String channel, String users, String text) {this(new Date(ms),channel,users,text);}
	public LineKick(Date time, String channel, String users, String text) {
		super(time,channel,users.split(";"));
		this.text = text;
	}
	
	public LineKick(BinBuffer buffer) {
		super(buffer);
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return "* "+users[0]+" has kicked "+users[1]+" ("+text+")";
	}

	@Override
	public void fillQuery(QueryInsert q, boolean prepare) {
		super.fillQuery(q,prepare);
		q.add("text",prepare?Wildcard.blank:text);
	}
	
	public int fillQuery(PreparedStatement p, int arg) throws SQLException {
		arg = super.fillQuery(p,arg);
		p.setString(arg++, text);
		return arg;
	}
}
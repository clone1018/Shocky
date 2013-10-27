package pl.shockah.shocky.lines;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.ModeEvent;

import pl.shockah.BinBuffer;
import pl.shockah.StringTools;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.Wildcard;

public class LineMode extends LineWithUsers {
	public final String text;
	
	public LineMode(ResultSet result) throws SQLException {
		super(result,result.getString("users").split(";"));
		this.text = result.getString("text");
	}
	
	public LineMode(String channel, String sender, String text) {this(new Date(),channel,sender,text);}
	public LineMode(long ms, String channel, String sender, String text) {this(new Date(ms),channel,sender,text);}
	public LineMode(Date time, String channel, String users, String text) {
		super(time,channel,users.split(";"));
		this.text = text;
		
	}
	
	public LineMode(ModeEvent<ShockyBot> event) {
		this(new Date(),event.getChannel().getName(),getUsers(event),event.getMode());
	}
	
	private static String getUsers(ModeEvent<ShockyBot> event) {
		String[] tokens = event.getMode().split("\\s");
		String[] users = new String[tokens.length];
		int n = 0;
		users[n++] = event.getUser().getNick();
		char[] mode = tokens[0].toCharArray();
		int token = 1;
		for (int i = 0; i < mode.length; i++) {
			char chr = mode[i];
			switch (chr) {
			case '-':
			case '+':
			break;
			case 'o':
			case 'h':
			case 'v':
				users[n++] = tokens[token++];
				break;
			default: token++; break;
			}
		}
		return StringTools.implode(users,";");
	}
	
	public LineMode(BinBuffer buffer) {
		super(buffer);
		this.text = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeUString(text);
	}

	public String getMessage() {
		return "* "+users[0]+" sets mode: "+text;
	}
	@Override
	public void fillQuery(QueryInsert q, boolean prepare) {
		super.fillQuery(q, prepare);
		q.add("text",prepare?Wildcard.blank:text);
	}
	
	public int fillQuery(PreparedStatement p, int arg) throws SQLException {
		arg = super.fillQuery(p,arg);
		p.setString(arg++, text);
		return arg;
	}
}
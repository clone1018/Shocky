package pl.shockah.shocky.lines;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.ActionEvent;

import pl.shockah.BinBuffer;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.Wildcard;

public class LineEnterLeave extends LineWithUsers {
	public final String text;
	
	public LineEnterLeave(ResultSet result) throws SQLException {
		super(result,new String[]{result.getString("users")});
		this.text = result.getString("text");
	}
	
	public LineEnterLeave(String channel, String sender, String text) {this(new Date(),channel,sender,text);}
	public LineEnterLeave(long ms, String channel, String sender, String text) {this(new Date(ms),channel,sender,text);}
	public LineEnterLeave(ActionEvent<ShockyBot> event) {this(new Date(),event.getChannel().getName(),event.getUser().getNick(),event.getAction());}
	public LineEnterLeave(Date time, String channel, String sender, String text) {
		super(time,channel,new String[]{sender});
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
		return "* "+users[0]+" "+text;
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
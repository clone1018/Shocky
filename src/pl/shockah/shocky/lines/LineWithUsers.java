package pl.shockah.shocky.lines;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import pl.shockah.BinBuffer;
import pl.shockah.StringTools;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.Wildcard;

public abstract class LineWithUsers extends Line {
	public final String[] users;
	
	public LineWithUsers(String channel, String user) {this(new Date(),channel,new String[] {user});}
	public LineWithUsers(long ms, String channel, String user) {this(new Date(ms),channel,new String[] {user});}
	public LineWithUsers(Date time, String channel, String[] users) {
		super(time,channel);
		this.users = users;
	}
	
	public LineWithUsers(BinBuffer buffer) {
		super(buffer);
		int count = buffer.readByte();
		this.users = new String[count];
		for (int i = 0; i < count; i++)
			this.users[i] = buffer.readUString();
	}
	
	public void save(BinBuffer buffer) {
		super.save(buffer);
		buffer.writeByte(users.length);
		for (int i = 0; i < users.length; i++)
			buffer.writeUString(users[i]);
	}
	
	public boolean containsUser(String user) {
		if (user == null) return true;
		boolean contains = false;
		for (int i = 0; !contains && i < users.length; i++)
			contains = users[i].equalsIgnoreCase(user);
		return contains;
	}
	@Override
	public void fillQuery(QueryInsert q, boolean prepare) {
		super.fillQuery(q,prepare);
		q.add("users",prepare?Wildcard.blank:StringTools.implode(users, ";"));
	}
	
	public int fillQuery(PreparedStatement p, int arg) throws SQLException {
		arg = super.fillQuery(p,arg);
		p.setString(arg++, StringTools.implode(users, ";"));
		return arg;
	}
}
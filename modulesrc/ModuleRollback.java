import java.io.File;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

import org.pircbotx.Channel;
import org.pircbotx.ShockyBot;
import org.pircbotx.hooks.events.*;

import pl.shockah.*;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.WebServer;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.events.*;
import pl.shockah.shocky.interfaces.IRollback;
import pl.shockah.shocky.interfaces.ILinePredicate;
import pl.shockah.shocky.lines.*;
import pl.shockah.shocky.sql.*;
import pl.shockah.shocky.sql.Criterion.Operation;

public class ModuleRollback extends Module implements IRollback {
	private static final Pattern durationPattern = Pattern.compile("([0-9]+)([smhd])", Pattern.CASE_INSENSITIVE);
	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	
	protected Command cmd;
	
	public static final int
		TYPE_MESSAGE = 1,
		TYPE_ACTION = 2,
		TYPE_ENTERLEAVE = 3,
		TYPE_KICK = 4,
		TYPE_MODE = 5,
		TYPE_MESSAGEACTION = 6,
		TYPE_OTHER = 0;
	
	private static final Criterion msgAndActCriterion  = new Criterion(
			new CriterionNumber("type",Operation.Equals,TYPE_MESSAGE)
			+ " OR " +
			new CriterionNumber("type",Operation.Equals,TYPE_ACTION));
	
	public static void appendLines(StringBuilder sb, ArrayList<Line> lines, boolean encode) {
		try {
			for (int i = 0; i < lines.size(); i++) {
				if (i != 0) sb.append('\n');
				String line = toString(lines.get(i));
				if (encode)
					line = URLEncoder.encode(line,"UTF8");
				sb.append(line);
			}
		} catch (Exception e) {e.printStackTrace();}
	}
	public static String toString(Line line) {
		return "["+sdf.format(line.time)+"] "+(Line.getWithChannels() ? "["+line.channel+"] " : " ")+line.getMessage();
	}
	
	public String name() {return "rollback";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
		Data.config.setNotExists("rollback-dateformat","dd.MM.yyyy HH:mm:ss");
		sdf.applyPattern(Data.config.getString("rollback-dateformat"));
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Command.addCommands(this, cmd = new CmdPastebin());
		Command.addCommand(this, "pb", cmd);
		
		SQL.raw("CREATE TABLE IF NOT EXISTS rollback (channel varchar(50) NOT NULL,users text,type int(1) unsigned NOT NULL,stamp bigint(20) unsigned NOT NULL,text text NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public void onMessage(MessageEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineMessage(event.getChannel().getName(),event.getUser().getNick(),event.getMessage()));
	}
	public void onMessageOut(MessageOutEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineMessage(event.getChannel().getName(),event.getBot().getNick(),event.getMessage()));
	}
	public void onAction(ActionEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineAction(event.getChannel().getName(),event.getUser().getNick(),event.getMessage()));
	}
	public void onActionOut(ActionOutEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineAction(event.getChannel().getName(),event.getBot().getNick(),event.getMessage()));
	}
	public void onTopic(TopicEvent<ShockyBot> event) {
		if (!event.isChanged()) return;
		addRollbackLine(event.getChannel().getName(),new LineOther(event.getChannel().getName(),"* "+event.getUser().getNick()+" has changed the topic to: "+event.getTopic()));
	}
	public void onJoin(JoinEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineEnterLeave(event.getChannel().getName(),event.getUser().getNick(),"("+event.getUser().getHostmask()+") has joined"));
	}
	public void onPart(PartEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineEnterLeave(event.getChannel().getName(),event.getUser().getNick(),"("+event.getUser().getHostmask()+") has left"));
	}
	public void onQuit(QuitEvent<ShockyBot> event) {
		for (Channel channel : event.getUser().getChannels()) addRollbackLine(channel.getName(),new LineEnterLeave(channel.getName(),event.getUser().getNick(),"has quit ("+event.getReason()+")"));
	}
	public void onKick(KickEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineKick(event));
	}
	public void onNickChange(NickChangeEvent<ShockyBot> event) {
		for (Channel channel : event.getBot().getChannels(event.getUser())) addRollbackLine(channel.getName(),new LineOther(channel.getName(),"* "+event.getOldNick()+" is now known as "+event.getNewNick()));
	}
	public void onMode(ModeEvent<ShockyBot> event) {
		addRollbackLine(event.getChannel().getName(),new LineMode(event));
	}
	public void onUserMode(UserModeEvent<ShockyBot> event) {
		String mode = event.getMode();
		if (mode.charAt(0) == ' ') mode = "+"+mode.substring(1);
		for (Channel channel : event.getBot().getChannels(event.getTarget())) addRollbackLine(channel.getName(),new LineOther(channel.getName(),"* "+event.getSource().getNick()+" sets mode "+mode+" "+event.getTarget().getNick()));
	}
	
	public synchronized void addRollbackLine(String channel, Line line) {
		if (channel == null || !channel.startsWith("#")) return;
		channel = channel.toLowerCase();
		
		PreparedStatement p = null;
		String key = line.getClass().getName();
		try {
			if (SQL.statements.containsKey(key) && !SQL.statements.get(key).isClosed()) {
				p = SQL.statements.get(key);
			} else {
				QueryInsert q = new QueryInsert(SQL.getTable("rollback"));
				q.add("channel",Wildcard.blank);
				q.add("stamp",Wildcard.blank);
				line.fillQuery(q, true);
				p = SQL.getSQLConnection().prepareStatement(q.getSQLQuery());
				SQL.statements.put(key,p);
			}
			synchronized (p) {
				p.setString(1, channel);
				p.setLong(2, System.currentTimeMillis());
				line.fillQuery(p, 3);
				p.execute();
				p.clearParameters();
			}
		} catch (SQLException e) {
			if (p != null)
				try {p.close();} catch (SQLException e1) {}
			e.printStackTrace();
		}
	}
	
	public ArrayList<Line> getRollbackLines(String channel, String user, String regex, String cull, boolean newest, int lines, int seconds) {
		return getRollbackLines(Line.class, channel, user, regex, cull, newest, lines, seconds);
	}
	
	public Line getLine(ResultSet result) throws SQLException {
		/*switch (result.getInt("type")) {
			case TYPE_MESSAGE: return new LineMessage(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"));
			case TYPE_ACTION: return new LineAction(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"));
			case TYPE_ENTERLEAVE: return new LineEnterLeave(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"));
			case TYPE_KICK: return new LineKick(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"));
			case TYPE_MODE: return new LineMode(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"));
			default: return new LineOther(result.getLong("stamp"),result.getString("channel"),result.getString("text"));
		}*/
		switch (result.getInt("type")) {
			case TYPE_MESSAGE: return new LineMessage(result);
			case TYPE_ACTION: return new LineAction(result);
			case TYPE_ENTERLEAVE: return new LineEnterLeave(result);
			case TYPE_KICK: return new LineKick(result);
			case TYPE_MODE: return new LineMode(result);
			default: return new LineOther(result);
		}
	}
	
	private long getOldestTime(String channel) throws SQLException {
		QuerySelect q = new QuerySelect(SQL.getTable("rollback"));
		if (channel != null)
			q.addCriterions(new CriterionString("channel",channel.toLowerCase()));
		q.setLimitCount(1);
		ResultSet j = SQL.select(q);
		try {
			if (j == null || !j.next())
				throw new SQLException("Cannot find oldest timestamp for "+channel);
			return j.getLong("stamp");
		} finally {
			if (j != null)
				j.close();
		}
	}
	
	private <T extends Line> ResultSet getResults(Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds) throws SQLException {
		int intType = TYPE_OTHER;
		if (type == LineMessage.class) intType = TYPE_MESSAGE;
		else if (type == LineAction.class) intType = TYPE_ACTION;
		else if (type == LineEnterLeave.class) intType = TYPE_ENTERLEAVE;
		else if (type == LineKick.class) intType = TYPE_KICK;
		else if (type == LineMode.class) intType = TYPE_MODE;
		else if (type == LineWithUsers.class) intType = TYPE_MESSAGEACTION;
		
		QuerySelect q = new QuerySelect(SQL.getTable("rollback"));
		if (channel != null) q.addCriterions(new CriterionString("channel",channel.toLowerCase()));
		if (user != null) q.addCriterions(new CriterionString("users",Operation.REGEXP,"(^|;)"+user.toLowerCase()+"($|;)"));
		if (lines != 0)
			q.setLimitCount(Math.min(lines,3000));
		else
			q.setLimitCount(3000);
		if (seconds != 0) {
			CriterionNumber criterion;
			long milliseconds = (seconds*1000);
			if (newest)
				criterion = new CriterionNumber("stamp",Operation.GreaterOrEqual,System.currentTimeMillis()-milliseconds);
			else
				criterion = new CriterionNumber("stamp",Operation.LesserOrEqual,getOldestTime(channel)+milliseconds);
			q.addCriterions(criterion);
		}
		if (regex != null && !regex.isEmpty())
			q.addCriterions(new CriterionString("text",Operation.REGEXP,regex));
		if (cull != null && !cull.isEmpty())
			q.addCriterions(new CriterionString("text",cull,false));
		if (type != Line.class) {
			if (intType != TYPE_MESSAGEACTION)
				q.addCriterions(new CriterionNumber("type",Operation.Equals,intType));
			else
				q.addCriterions(msgAndActCriterion);
		}
		q.addOrder("stamp",!newest);
			
		return SQL.select(q);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends Line> ArrayList<T> getRollbackLines(Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds) {
		ArrayList<T> ret = new ArrayList<T>();
		ResultSet result = null;
		try {
			result = getResults(type, channel, user, regex, cull, newest, lines, seconds);
			if (result != null) {
				while(result.next())
					ret.add((T)getLine(result));
				Collections.reverse(ret);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
				try {
					if (result != null && !result.isClosed())
						result.close();
				} catch (SQLException e) {
				}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T extends Line> T getRollbackLine(ILinePredicate<T> predicate, Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds) {
		ResultSet result = null;
		try {
			result = getResults(type, channel, user, regex, cull, newest, lines, seconds);
			if (result != null) {
				while(result.next()) {
					T line = (T) getLine(result);
					if (predicate.accepts(line))
						return line;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (result != null && !result.isClosed())
					result.close();
			} catch (SQLException e) {
			}
		}
		return null;
	}
	
	public class CmdPastebin extends Command {
		public String command() {return "pastebin";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("pastebin/pb");
			
			if (params.type == EType.Channel) {
				sb.append("\npastebin [channel] [user] {lines} - uploads last lines to paste.kde.org/pastebin.com/pastebin.ca");
				sb.append("\npastebin [channel] [user] -{lines} - uploads first lines to paste.kde.org/pastebin.com/pastebin.ca");
				sb.append("\npastebin [channel] [user] {time}{d/h/m/s} - uploads last lines from set time to paste.kde.org/pastebin.com/pastebin.ca");
			} else {
				sb.append("\npastebin [channel] [user] {lines} - uploads last lines to paste.kde.org/pastebin.com/pastebin.ca");
				sb.append("\npastebin [channel] [user] -{lines} - uploads first lines to paste.kde.org/pastebin.com/pastebin.ca");
				sb.append("\npastebin [channel] [user] {time}{d/h/m/s} - uploads last lines from set time to paste.kde.org/pastebin.com/pastebin.ca");
			}
			
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			String[] args = params.input.split(" ");
			String pbLink = null, regex = null;
			callback.type = EType.Notice;
			
			if (args.length > 0) {
				for (int i = args.length-1; i > 0; i--) {
					if (args[i].equals("|")) {
						regex = StringTools.implode(params.input,i+1," ");
						args = StringTools.implode(args,0,i-1," ").split(" ");
						break;
					}
				}
			}
			
			if (args.length < 1 || args.length > 3) {
				callback.append(help(params));
				return;
			}
			
			if (args.length >= 1) {
				String aChannel = null, aUser = null, aLines;
				
				if (args.length == 1) {
					aLines = args[0];
				} else if (args.length == 2) {
					if (args[0].startsWith("#")) aChannel = args[0]; else aUser = args[0];
					aLines = args[1];
				} else {
					aChannel = args[0];
					aUser = args[1];
					aLines = args[2];
				}
				if (aChannel == null && aUser == null) {
					if (params.type == EType.Channel) aChannel = params.channel.getName(); else {
						callback.append(help(params));
						return;
					}
				}
				if (aChannel != null) aChannel = aChannel.toLowerCase();
				
				ArrayList<Line> list;
				Matcher m = durationPattern.matcher(aLines);
				if (m.find()) {
					boolean additive = aLines.charAt(0) != '-';
					
					int time = 0;
					do {
						int i = Integer.parseInt(m.group(1));
						char c = m.group(2).charAt(0);
						switch (c) {
							case 's': time += i; break;
							case 'm': time += i*60; break;
							case 'h': time += i*3600; break;
							case 'd': time += i*86400; break;
						}
					} while (m.find());
					
					list = getRollbackLines(aChannel,aUser,regex,null,additive,0,time);
				} else {
					try {
						int i = Integer.parseInt(aLines);
						list = getRollbackLines(aChannel,aUser,regex,null,i >= 0,Math.abs(i),0);
					} catch (NumberFormatException e) {
						callback.append(help(params));
						return;
					}
				}
				
				if (list.isEmpty()) {
					callback.append("Nothing to upload");
					return;
				}
				pbLink = getLink(list,aUser != null && aChannel == null);
			} else {
				callback.append(help(params));
				return;
			}
			
			if (pbLink != null) {
				callback.type = EType.Channel;
				callback.append(pbLink);
			}
		}
		
		public String getLink(ArrayList<Line> lines, boolean withChannel) {
			StringBuilder sb = new StringBuilder();
			Line.setWithChannels(withChannel);
			appendLines(sb,lines,!WebServer.exists());
			Line.setWithChannels(false);
			
			String link = Utils.paste(sb);
			if (link != null)
				return link;
			return "Failed with all services";
		}
	}
}
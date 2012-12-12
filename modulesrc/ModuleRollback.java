import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.*;
import pl.shockah.*;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.events.*;
import pl.shockah.shocky.lines.*;
import pl.shockah.shocky.prototypes.IRollback;
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
		TYPE_OTHER = 0;
	
	public static void appendLines(StringBuilder sb, ArrayList<Line> lines) {
		try {
			for (int i = 0; i < lines.size(); i++) {
				if (i != 0) sb.append("\n");
				sb.append(URLEncoder.encode(toString(lines.get(i)),"UTF8"));
			}
		} catch (Exception e) {e.printStackTrace();}
	}
	public static String toString(Line line) {
		return "["+sdf.format(line.time)+"] "+(Line.getWithChannels() ? "["+line.channel+"] " : " ")+line.getMessage();
	}
	
	public String name() {return "rollback";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("rollback-dateformat","dd.MM.yyyy HH:mm:ss");
		sdf.applyPattern(Data.config.getString("rollback-dateformat"));
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Command.addCommands(this, cmd = new CmdPastebin());
		Command.addCommand(this, "pb", cmd);
		
		SQL.raw("CREATE TABLE IF NOT EXISTS rollback (channel varchar(50) NOT NULL,users text,type int(1) unsigned NOT NULL,stamp bigint(20) unsigned NOT NULL,text text NOT NULL) ENGINE=ARCHIVE DEFAULT CHARSET=utf8;");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineMessage(event.getChannel().getName(),event.getUser().getNick(),event.getMessage()));
	}
	public void onMessageOut(MessageOutEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineMessage(event.getChannel().getName(),event.getBot().getNick(),event.getMessage()));
	}
	public void onAction(ActionEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineAction(event.getChannel().getName(),event.getUser().getNick(),event.getMessage()));
	}
	public void onActionOut(ActionOutEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineAction(event.getChannel().getName(),event.getBot().getNick(),event.getMessage()));
	}
	public void onTopic(TopicEvent<PircBotX> event) {
		if (!event.isChanged()) return;
		addRollbackLine(event.getChannel().getName(),new LineOther(event.getChannel().getName(),"* "+event.getUser().getNick()+" has changed the topic to: "+event.getTopic()));
	}
	public void onJoin(JoinEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineEnterLeave(event.getChannel().getName(),event.getUser().getNick(),"("+event.getUser().getHostmask()+") has joined"));
	}
	public void onPart(PartEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineEnterLeave(event.getChannel().getName(),event.getUser().getNick(),"("+event.getUser().getHostmask()+") has left"));
	}
	public void onQuit(QuitEvent<PircBotX> event) {
		for (Channel channel : event.getUser().getChannels()) addRollbackLine(channel.getName(),new LineEnterLeave(channel.getName(),event.getUser().getNick(),"has quit ("+event.getReason()+")"));
	}
	public void onKick(KickEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineKick(event));
	}
	public void onNickChange(NickChangeEvent<PircBotX> event) {
		for (Channel channel : event.getBot().getChannels(event.getUser())) addRollbackLine(channel.getName(),new LineOther(channel.getName(),"* "+event.getOldNick()+" is now known as "+event.getNewNick()));
	}
	public void onMode(ModeEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineMode(event));
	}
	public void onUserMode(UserModeEvent<PircBotX> event) {
		String mode = event.getMode();
		if (mode.charAt(0) == ' ') mode = "+"+mode.substring(1);
		for (Channel channel : event.getBot().getChannels(event.getTarget())) addRollbackLine(channel.getName(),new LineOther(channel.getName(),"* "+event.getSource().getNick()+" sets mode "+mode+" "+event.getTarget().getNick()));
	}
	
	public synchronized void addRollbackLine(String channel, Line line) {
		if (channel == null || !channel.startsWith("#")) return;
		channel = channel.toLowerCase();
		
		PreparedStatement p;
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
			e.printStackTrace();
		}
	}
	
	public ArrayList<Line> getRollbackLines(String channel, String user, String regex, String cull, boolean newest, int lines, int seconds) {
		return getRollbackLines(Line.class, channel, user, regex, cull, newest, lines, seconds);
	}
	
	@SuppressWarnings("unchecked") public synchronized <T extends Line> ArrayList<T> getRollbackLines(Class<T> type, String channel, String user, String regex, String cull, boolean newest, int lines, int seconds) {
		ArrayList<T> ret = new ArrayList<T>();
		int intType = TYPE_OTHER;
		if (type == LineMessage.class) intType = TYPE_MESSAGE;
		if (type == LineAction.class) intType = TYPE_ACTION;
		if (type == LineEnterLeave.class) intType = TYPE_ENTERLEAVE;
		if (type == LineKick.class) intType = TYPE_KICK;
		if (type == LineMode.class) intType = TYPE_MODE;
		
		try {
			QuerySelect q = new QuerySelect(SQL.getTable("rollback"));
			if (channel != null) q.addCriterions(new CriterionString("channel",channel.toLowerCase()));
			if (user != null) q.addCriterions(new CriterionString("users",Operation.REGEXP,"(^|;)"+user.toLowerCase()+"($|;)"));
			if (lines != 0) q.setLimitCount(lines);
			if (seconds != 0) {
				if (!newest) {
					QuerySelect q2 = new QuerySelect(SQL.getTable("rollback"));
					if (channel != null) q2.addCriterions(new CriterionString("channel",channel.toLowerCase()));
					q2.setLimitCount(1);
					ResultSet j = SQL.select(q2);
					if (j == null || j.getFetchSize() == 0) return ret;
					q.addCriterions(new CriterionNumber("stamp",Operation.LesserOrEqual,j.getLong("stamp")+seconds));
				} else q.addCriterions(new CriterionNumber("stamp",Operation.GreaterOrEqual,new Date().getTime()-(seconds*1000)));
			}
			if (regex != null && !regex.isEmpty()) q.addCriterions(new CriterionString("text",Operation.REGEXP,regex));
			if (cull != null && !cull.isEmpty()) q.addCriterions(new CriterionString("text",cull,false));
			if (type != Line.class) q.addCriterions(new CriterionNumber("type",Operation.Equals,intType));
			q.addOrder("stamp",!newest);
			
			ResultSet result = SQL.select(q);
			if (result == null)
				return ret;
			while(result.next()) {
				switch (result.getInt("type")) {
					case TYPE_MESSAGE: ret.add((T)new LineMessage(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"))); break;
					case TYPE_ACTION: ret.add((T)new LineAction(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"))); break;
					case TYPE_ENTERLEAVE: ret.add((T)new LineEnterLeave(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"))); break;
					case TYPE_KICK: ret.add((T)new LineKick(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"))); break;
					case TYPE_MODE: ret.add((T)new LineMode(result.getLong("stamp"),result.getString("channel"),result.getString("users"),result.getString("text"))); break;
					default: ret.add((T)new LineOther(result.getLong("stamp"),result.getString("channel"),result.getString("text"))); break;
				}
			}
			/*if (j == null || j.length() == 0) return ret;
			ArrayList<JSONObject> results = new ArrayList<JSONObject>();
			if (j.getFetchSize() == 1 && j.has("___")) {
				JSONArray ja = j.getJSONArray("___");
				for (int i = 0; i < ja.length(); i++) results.add(ja.getJSONObject(i));
			} else results.add(j);
			
			for (JSONObject result : results) {
				switch (result.getInt("type")) {
					case TYPE_MESSAGE: ret.add((T)new LineMessage(result.getLong("stamp"),result.getString("channel"),result.getString("user"),result.getString("txt"))); break;
					case TYPE_ACTION: ret.add((T)new LineAction(result.getLong("stamp"),result.getString("channel"),result.getString("user"),result.getString("txt"))); break;
					case TYPE_ENTERLEAVE: ret.add((T)new LineEnterLeave(result.getLong("stamp"),result.getString("channel"),result.getString("user"),result.getString("txt"))); break;
					case TYPE_KICK: ret.add((T)new LineKick(result.getLong("stamp"),result.getString("channel"),result.getString("user"),result.getString("user2"),result.getString("txt"))); break;
					default: ret.add((T)new LineOther(result.getLong("stamp"),result.getString("channel"),result.getString("txt"))); break;
				}
			}*/
		} catch (Exception e) {e.printStackTrace();}
		
		Collections.reverse(ret);
		return ret;
	}
	
	public class CmdPastebin extends Command {
		public String command() {return "pastebin";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("pastebin/pb");
			
			if (type == EType.Channel) {
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
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			String pbLink = "", regex = null;
			callback.type = EType.Notice;
			
			for (int i = args.length-1; i > 0; i--) {
				if (args[i].equals("|")) {
					regex = StringTools.implode(args,i+1," ");
					args = StringTools.implode(args,0,i-1," ").split(" ");
					break;
				}
			}
			
			if (args.length < 2 || args.length > 4) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			if (args.length >= 2) {
				String aChannel = null, aUser = null, aLines;
				
				if (args.length == 2) {
					aLines = args[1];
				} else if (args.length == 3) {
					if (args[1].startsWith("#")) aChannel = args[1]; else aUser = args[1];
					aLines = args[2];
				} else {
					aChannel = args[1];
					aUser = args[2];
					aLines = args[3];
				}
				if (aChannel == null && aUser == null) {
					if (type == EType.Channel) aChannel = channel.getName(); else {
						callback.append(help(bot,type,channel,sender));
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
				} else list = getRollbackLines(aChannel,aUser,regex,null,aLines.charAt(0) != '-',Math.abs(Integer.parseInt(aLines)),0);
				
				if (list.isEmpty()) {
					callback.append("Nothing to upload");
					return;
				}
				pbLink = getLink(list,aUser != null && aChannel == null);
			} else {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			if (!pbLink.isEmpty()) {
				callback.type = EType.Channel;
				callback.append(pbLink);
			}
		}
		
		public String getLink(ArrayList<Line> lines, boolean withChannel) {
			StringBuilder sb = new StringBuilder();
			Line.setWithChannels(withChannel);
			appendLines(sb,lines);
			Line.setWithChannels(false);
			
			String link = Utils.paste(sb);
			if (link != null)
				return link;
			return "Failed with all services";
		}
	}
}
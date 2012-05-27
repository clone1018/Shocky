import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserModeEvent;
import pl.shockah.BinBuffer;
import pl.shockah.BinFile;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.events.*;
import pl.shockah.shocky.lines.*;

public class ModuleRollback extends Module {
	public final HashMap<String,ArrayList<Line>> rollback = new HashMap<String,ArrayList<Line>>(), rollbackTmp = new HashMap<String,ArrayList<Line>>();
	public final ArrayList<PasteService> services = new ArrayList<ModuleRollback.PasteService>();
	protected Command cmd;
	
	public static void appendLines(StringBuilder sb, ArrayList<Line> lines) {
		try {
			for (int i = 0; i < lines.size(); i++) {
				if (i != 0) sb.append("\n");
				sb.append(URLEncoder.encode(toString(lines.get(i)),"UTF8"));
			}
		} catch (Exception e) {e.printStackTrace();}
	}
	public static String toString(Line line) {
		SimpleDateFormat sdf = new SimpleDateFormat(Data.config.getString("rollback-dateformat"));
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return "["+sdf.format(line.time)+"] "+line.getMessage();
	}
	
	public String name() {return "rollback";}
	public boolean isListener() {return true;}
	public void onEnable() {
		File dir = new File("data","rollback"); dir.mkdir();
		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) return;
			
			String channel = f.getName();
			BinBuffer binb = new BinFile(f).read(); binb.setPos(0);
			int count = binb.readInt();
			for (int i = 0; i < count; i++) {
				try {
					Line line = Line.readLine(binb);
					if (line != null)
						addRollbackLine(channel,line);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		rollbackTmp.clear();
		
		Data.config.setNotExists("rollback-dateformat","dd.MM.yyyy HH:mm:ss");
		Command.addCommands(cmd = new CmdPastebin());
		
		services.add(new ServicePasteKdeOrg());
		services.add(new ServicePastebinCom());
		services.add(new ServicePastebinCa());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
		services.clear();
	}
	public void onDataSave() {
		File dir = new File("data","rollback"); dir.mkdir();
		BinBuffer binb = new BinBuffer();
		
		Iterator<Entry<String, ArrayList<Line>>> it = rollbackTmp.entrySet().iterator();
		while (it.hasNext()) {
			binb.clear();
			Map.Entry<String, ArrayList<Line>> pair = it.next();
			
			ArrayList<Line> lines = pair.getValue();
			binb.writeInt(lines.size());
			for (Line line : lines) {
				byte type = Line.getLineID(line);
				if (type != -1) {
					binb.writeByte(type);
					line.save(binb);
				}
			}
			
			binb.setPos(0);
			new BinFile(new File(dir,pair.getKey())).append(binb);
		}
		
		rollbackTmp.clear();
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineMessage(event.getUser().getNick(),event.getMessage()));
	}
	public void onMessageOut(MessageOutEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineMessage(event.getBot().getNick(),event.getMessage()));
	}
	public void onAction(ActionEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineAction(event.getUser().getNick(),event.getMessage()));
	}
	public void onActionOut(ActionOutEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineAction(event.getBot().getNick(),event.getMessage()));
	}
	public void onTopic(TopicEvent<PircBotX> event) {
		if (!event.isChanged()) return;
		addRollbackLine(event.getChannel().getName(),new LineOther("* "+event.getUser().getNick()+" has changed the topic to: "+event.getTopic()));
	}
	public void onJoin(JoinEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineEnterLeave(event.getUser().getNick(),"("+event.getUser().getHostmask()+") has joined"));
	}
	public void onPart(PartEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineEnterLeave(event.getUser().getNick(),"("+event.getUser().getHostmask()+") has left"));
	}
	public void onQuit(QuitEvent<PircBotX> event) {
		for (Channel channel : event.getUser().getChannels()) addRollbackLine(channel.getName(),new LineEnterLeave(event.getUser().getNick(),"has quit ("+event.getReason()+")"));
	}
	public void onKick(KickEvent<PircBotX> event) {
		addRollbackLine(event.getChannel().getName(),new LineKick(event));
	}
	public void onNickChange(NickChangeEvent<PircBotX> event) {
		for (Channel channel : event.getBot().getChannels(event.getUser())) addRollbackLine(channel.getName(),new LineOther("* "+event.getOldNick()+" is now known as "+event.getNewNick()));
	}
	public void onMode(ModeEvent<PircBotX> event) {
		String mode = event.getMode();
		if (mode.charAt(0) == ' ') mode = "+"+mode.substring(1);
		addRollbackLine(event.getChannel().getName(),new LineOther("* "+event.getUser().getNick()+" sets mode "+mode));
	}
	public void onUserMode(UserModeEvent<PircBotX> event) {
		String mode = event.getMode();
		if (mode.charAt(0) == ' ') mode = "+"+mode.substring(1);
		for (Channel channel : event.getBot().getChannels(event.getTarget())) addRollbackLine(channel.getName(),new LineOther("* "+event.getSource().getNick()+" sets mode "+mode+" "+event.getTarget().getNick()));
	}
	
	public synchronized void addRollbackLine(String channel, Line line) {
		if (!channel.startsWith("#")) return;
		channel = channel.toLowerCase();
		if (!rollback.containsKey(channel)) rollback.put(channel,new ArrayList<Line>());
		if (!rollbackTmp.containsKey(channel)) rollbackTmp.put(channel,new ArrayList<Line>());
		rollback.get(channel).add(line);
		rollbackTmp.get(channel).add(line);
	}
	
	public ArrayList<Line> getRollbackLines(String channel, String user, String regex, boolean newest, int lines, int seconds) {
		return getRollbackLines(Line.class, channel, user, regex, newest, lines, seconds);
	}
	
	public <T extends Line> ArrayList<T> getRollbackLines(Class<T> type, String channel, String user, String regex, boolean newest, int lines, int seconds) {
		ArrayList<T> ret = new ArrayList<T>();
		ArrayList<Line> linesChannel = rollback.get(channel);
		if (linesChannel == null || linesChannel.isEmpty()) return ret;
		Pattern pat = regex == null ? null : Pattern.compile(regex);
		
		int i = newest ? linesChannel.size() : -1;
		if (lines != 0) {
			while (lines > ret.size()) {
				i += newest ? -1 : 1;
				if (i < 0 || i >= linesChannel.size()) break;
				
				Line line = linesChannel.get(i);
				if (!type.isAssignableFrom(line.getClass()))
					continue;
				@SuppressWarnings("unchecked")
				T generic = (T) linesChannel.get(i);
				if (line.containsUser(user)) {
					if (pat == null) ret.add(generic);
					else {
						String tmp = null;
						if (line instanceof LineMessage) tmp = ((LineMessage)line).text;
						if (line instanceof LineAction) tmp = ((LineAction)line).text;
						if (tmp != null) {
							if (pat.matcher(tmp).find()) ret.add(generic);
							continue;
						}
					}
				}
			}
		} else {
			Date check = newest ? new Date(new Date().getTime()-(seconds*1000l)) : new Date(linesChannel.get(0).time.getTime()+(seconds*1000l));
			
			while (true) {
				i += newest ? -1 : 1;
				if (i < 0 || i >= linesChannel.size()) break;
				
				Line line = linesChannel.get(i);
				if (!type.isAssignableFrom(line.getClass()))
					continue;
				@SuppressWarnings("unchecked")
				T generic = (T) linesChannel.get(i);
				if (newest && line.time.before(check)) break;
				if (!newest && line.time.after(check)) break;
				
				if (line.containsUser(user)) {
					if (pat == null) ret.add(generic);
					else {
						String tmp = null;
						if (line instanceof LineMessage) tmp = ((LineMessage)line).text;
						if (line instanceof LineAction) tmp = ((LineAction)line).text;
						if (tmp != null) {
							if (pat.matcher(tmp).find()) ret.add(generic);
							continue;
						}
					}
				}
			}
		}
		
		if (newest) Collections.reverse(ret);
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
				sb.append("\npastebin [channel] [user] {time}{h/m/s} - uploads last lines from set time to paste.kde.org/pastebin.com/pastebin.ca");
			} else {
				sb.append("\npastebin {channel} [user] {lines} - uploads last lines to paste.kde.org/pastebin.com/pastebin.ca");
				sb.append("\npastebin {channel} [user] -{lines} - uploads first lines to paste.kde.org/pastebin.com/pastebin.ca");
				sb.append("\npastebin {channel} [user] {time}{h/m/s} - uploads last lines from set time to paste.kde.org/pastebin.com/pastebin.ca");
			}
			
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("pb");}
		
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
				String aChannel = type == EType.Channel ? channel.getName() : null, aUser = null, aLines;
				
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
				if (aChannel == null) {
					callback.append(help(bot,type,channel,sender));
					return;
				}
				aChannel = aChannel.toLowerCase();
				
				if (rollback.containsKey(aChannel) && !rollback.get(aChannel).isEmpty()) {
					ArrayList<Line> list;
					if (aLines.toLowerCase().endsWith("s")) list = getRollbackLines(aChannel,aUser,regex,aLines.charAt(0) != '-',0,Math.abs(Integer.parseInt(aLines.substring(0,aLines.length()-1))));
					else if (aLines.toLowerCase().endsWith("m")) list = getRollbackLines(aChannel,aUser,regex,aLines.charAt(0) != '-',0,Math.abs(60*Integer.parseInt(aLines.substring(0,aLines.length()-1))));
					else if (aLines.toLowerCase().endsWith("h")) list = getRollbackLines(aChannel,aUser,regex,aLines.charAt(0) != '-',0,Math.abs(3600*Integer.parseInt(aLines.substring(0,aLines.length()-1))));
					else if (aLines.toLowerCase().endsWith("d")) list = getRollbackLines(aChannel,aUser,regex,aLines.charAt(0) != '-',0,Math.abs(86400*Integer.parseInt(aLines.substring(0,aLines.length()-1))));
					else list = getRollbackLines(aChannel,aUser,regex,aLines.charAt(0) != '-',Math.abs(Integer.parseInt(aLines)),0);
					
					if (list.isEmpty()) {
						callback.append("Nothing to upload");
						return;
					}
					pbLink = getLink(list);
				} else callback.append("No "+aChannel+" archive");
			} else {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			if (!pbLink.isEmpty()) {
				callback.type = EType.Channel;
				callback.append(pbLink);
			}
		}
		
		public String getLink(ArrayList<Line> lines) {
			String link;
			for (PasteService service : services) {
				link = service.paste(lines); if (link.isEmpty() || link.startsWith("http://")) return link;
			}
			return "Failed with all services";
		}
	}

	public abstract class PasteService {
		public abstract String paste(ArrayList<Line> lines);
	}
	public class ServicePasteKdeOrg extends PasteService {
		public String paste(ArrayList<Line> lines) {
			HTTPQuery q = new HTTPQuery("http://paste.kde.org/","POST");
			
			StringBuilder sb = new StringBuilder();
			sb.append("paste_lang=Text");
			sb.append("&api_submit=1");
			sb.append("&mode=xml");
			sb.append("&paste_private=yes");
			sb.append("&paste_data=");
			appendLines(sb,lines);
			
			q.connect(true,true);
			q.write(sb.toString());
			ArrayList<String> list = q.read();
			q.close();
			
			String pasteId = null, pasteHash = null;
			for (String s : list) {
				s = s.trim();
				if (s.startsWith("<id>")) pasteId = s.substring(4,s.length()-5);
				if (s.startsWith("<hash>")) pasteHash = s.substring(6,s.length()-7);
			}
			
			if (pasteId != null) {
				if (pasteHash != null) return "http://paste.kde.org/"+pasteId+"/"+pasteHash+"/";
				return "http://paste.kde.org/"+pasteId+"/";
			}
			return null;
		}
	}
	public class ServicePastebinCom extends PasteService {
		private static final String apiKey = "caa6c4204f869de432d5434776598b1c";
		
		public String paste(ArrayList<Line> lines) {
			HTTPQuery q = new HTTPQuery("http://pastebin.com/api/api_post.php","POST");
			
			StringBuilder sb = new StringBuilder();
			sb.append("api_option=paste");
			sb.append("&api_dev_key="+apiKey);
			sb.append("&api_paste_private=1");
			sb.append("&api_paste_format=text");
			sb.append("&api_paste_code=");
			appendLines(sb,lines);
			
			q.connect(true,true);
			q.write(sb.toString());
			ArrayList<String> list = q.read();
			q.close();
			
			return list.get(0);
		}
	}
	public class ServicePastebinCa extends PasteService {
		private static final String apiKey = "srDSz+PeUmUWZWm5qhHkK0WVlmQe29cx";
		
		public String paste(ArrayList<Line> lines) {
			HTTPQuery q = new HTTPQuery("http://pastebin.ca/quiet-paste.php","POST");
			
			StringBuilder sb = new StringBuilder();
			try {
				sb.append("api="+URLEncoder.encode(apiKey,"UTF8"));
				sb.append("&content=");
				appendLines(sb,lines);
			} catch (Exception e) {e.printStackTrace();}
			
			q.connect(true,true);
			q.write(sb.toString());
			ArrayList<String> list = q.read();
			q.close();
			
			String s = list.get(0);
			if (s.startsWith("SUCCESS")) return "http://pastebin.ca/"+s.substring(7);
			return null;
		}
	}
}
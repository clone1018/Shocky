import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserModeEvent;
import pl.shockah.FileLine;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.lines.LineMessage;

public class ModuleTell extends Module {
	public final HashMap<String,ArrayList<LineMessage>> tells = new HashMap<String,ArrayList<LineMessage>>();
	protected Command cmd;
	
	public String name() {return "tell";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Command.addCommands(cmd = new CmdTell());
		
		ArrayList<String> lines = FileLine.read(new File("data","tell.cfg"));
		for (int i = 0; i < lines.size(); i += 4) addTell(lines.get(i),new LineMessage(Long.parseLong(lines.get(i+2)),lines.get(i+1),lines.get(i+3)));
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	public void onDataSave() {
		ArrayList<String> lines = new ArrayList<String>();
		Iterator<Entry<String,ArrayList<LineMessage>>> it = tells.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String,ArrayList<LineMessage>> pair = it.next();
			for (LineMessage l : pair.getValue()) {
				lines.add(pair.getKey());
				lines.add(l.sender);
				lines.add(""+l.time.getTime());
				lines.add(l.getMessage());
			}
		}
		FileLine.write(new File("data","tell.cfg"),lines);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		String[] args = event.getMessage().split(" ");
		if (args.length>0&&args[0].length()>0&&!Command.matches(cmd,event.getBot(),EType.Channel,args[0]))
			sendTells(event.getBot(),event.getUser());}
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {if (!Command.matches(cmd,event.getBot(),EType.Private,event.getMessage().split(" ")[0])) sendTells(event.getBot(),event.getUser());}
	public void onNotice(NoticeEvent<PircBotX> event) {if (!Command.matches(cmd,event.getBot(),EType.Notice,event.getMessage().split(" ")[0])) sendTells(event.getBot(),event.getUser());}
	public void onAction(ActionEvent<PircBotX> event) {sendTells(event.getBot(),event.getUser());}
	public void onTopic(TopicEvent<PircBotX> event) {if (event.isChanged()) sendTells(event.getBot(),event.getUser());}
	public void onKick(KickEvent<PircBotX> event) {sendTells(event.getBot(),event.getSource());}
	public void onMode(ModeEvent<PircBotX> event) {sendTells(event.getBot(),event.getUser());}
	public void onUserMode(UserModeEvent<PircBotX> event) {sendTells(event.getBot(),event.getSource());}
	
	public void sendTells(PircBotX bot, User user) {
		String unick = user.getNick().toLowerCase();
		if (!tells.containsKey(unick)) return;
		ArrayList<LineMessage> lines = tells.get(unick);
		tells.remove(unick);
		
		SimpleDateFormat sdf = new SimpleDateFormat("d.MM, HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		for (LineMessage line : lines) Shocky.sendNotice(bot,user,line.sender+" said "+Utils.timeAgo(line.time)+": "+line.text);
	}
	
	public synchronized void addTell(String user, LineMessage line) {
		if (user.startsWith("#")) return;
		user = user.toLowerCase();
		if (!tells.containsKey(user)) tells.put(user,new ArrayList<LineMessage>());
		tells.get(user).add(line);
	}
	
	public class CmdTell extends Command {
		public String command() {return "tell";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "tell {user} {message} - relay the message to user";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			callback.type = EType.Notice;
			if (args.length >= 3) {
				addTell(args[1],new LineMessage(sender.getNick(),StringTools.implode(args,2," ")));
				callback.append("I'll pass that along");
				return;
			}
			callback.append(help(bot,type,channel,sender));
		}
	}
}
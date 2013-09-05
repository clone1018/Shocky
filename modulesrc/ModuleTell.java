import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
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
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.interfaces.ISeen;
import pl.shockah.shocky.lines.LineMessage;

public class ModuleTell extends Module {
	public final HashMap<String,ArrayList<LineMessage>> tells = new HashMap<String,ArrayList<LineMessage>>();
	protected Command cmd;
	
	public String name() {return "tell";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdTell());
		
		ArrayList<String> lines = FileLine.read(new File(dir,"tell.cfg"));
		for (int i = 0; i < lines.size(); i += 4) addTell(lines.get(i),new LineMessage(Long.parseLong(lines.get(i+2)),"",lines.get(i+1),lines.get(i+3)));
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	public void onDataSave(File dir) {
		ArrayList<String> lines = new ArrayList<String>();
		Iterator<Entry<String,ArrayList<LineMessage>>> it = tells.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String,ArrayList<LineMessage>> pair = it.next();
			for (LineMessage l : pair.getValue()) {
				lines.add(pair.getKey());
				lines.add(StringTools.implode(l.users, ";"));
				lines.add(Long.toString(l.time.getTime()));
				lines.add(l.text);
			}
		}
		FileLine.write(new File(dir,"tell.cfg"),lines);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		String[] args = event.getMessage().split(" ");
		if (args.length>0&&args[0].length()>0&&!Command.matches(cmd,event.getBot(),EType.Channel,event.getChannel().getName(),args[0]))
			sendTells(event.getBot(),event.getUser());}
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {if (!Command.matches(cmd,event.getBot(),EType.Private,null,event.getMessage().split(" ")[0])) sendTells(event.getBot(),event.getUser());}
	public void onNotice(NoticeEvent<PircBotX> event) {if (!Command.matches(cmd,event.getBot(),EType.Notice,null,event.getMessage().split(" ")[0])) sendTells(event.getBot(),event.getUser());}
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
		
		for (LineMessage line : lines) Shocky.sendNotice(bot,user,line.users[0]+" said "+Utils.timeAgo(line.time)+": "+line.text);
	}
	
	public synchronized void addTell(String user, LineMessage line) {
		if (user.startsWith("#")) return;
		user = user.toLowerCase();
		if (!tells.containsKey(user)) tells.put(user,new ArrayList<LineMessage>());
		tells.get(user).add(line);
	}
	
	public class CmdTell extends Command {
		public String command() {return "tell";}
		public String help(Parameters params) {
			return "tell {user} {message} - relay the message to user";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			if (params.tokenCount < 2) {
				callback.append(help(params));
				return;
			}
			
			String target = params.tokens.nextToken();
			
			Module seenModule = Module.getModule("seen");
			if (seenModule != null && seenModule instanceof ISeen && seenModule.isEnabled(params.channel.getName())) {
				ISeen seen = (ISeen)seenModule;
				if (!seen.hasSeen(target)) {
					callback.append("I do not know ").append(target);
					return;
				}
			}
			
			String tell = params.getParams(0);
			if (tell.length() > Data.config.getInt("main-messagelength"))
			{
				callback.append("I dare not send such a lengthy message");
				return;
			}
			
			addTell(target,new LineMessage("",params.sender.getNick(),tell));
			callback.append("I'll pass that along");
		}
	}

	@Override
	public void onCleanup(PircBotX bot, CommandCallback callback, User sender) {
		Module seenModule = Module.getModule("seen");
		if (!(seenModule != null && seenModule instanceof ISeen)) {
			callback.append("[tell]: Cannot find Seen module");
			return;
		}
		try {
			ISeen seen = (ISeen)seenModule;
			int total = 0;
			Iterator<String> iter = tells.keySet().iterator();
			while (iter.hasNext()) {
				String target = iter.next();
				if (!seen.hasSeen(target)) {
					iter.remove();
					total++;
				}
			}
			callback.append("[tell]: Purged a total of ").append(total).append(" unknown tells.");
		} catch(Throwable t) {
			callback.append("[tell]: ").append(t.getMessage());
		}
	}
}
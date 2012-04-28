package pl.shockah.shocky;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.MultiBotManager;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import pl.shockah.Pair;
import pl.shockah.shocky.cmds.Command;

public class Shocky extends ListenerAdapter {
	public static Map<Thread,Pair<Command.EType,Command.EType>> overrideTarget = Collections.synchronizedMap(new HashMap<Thread,Pair<Command.EType,Command.EType>>());
	private static TimedActions timed;
	private static MultiBotManager multiBot;
	private static boolean isClosing = false;
	
	public static void main(String[] args) {
		Data.load();
		
		Data.config.setNotExists("main-botname","Shocky");
		Data.config.setNotExists("main-server","irc.esper.net");
		Data.config.setNotExists("main-version","Shocky - PircBotX 1.6 - https://github.com/clone1018/Shocky - http://pircbotx.googlecode.com");
		Data.config.setNotExists("main-verbose",false);
		Data.config.setNotExists("main-maxchannels",10);
		Data.config.setNotExists("main-nickservpass","");
		Data.config.setNotExists("main-cmdchar","`~");
		Data.config.setNotExists("main-sqlurl","http://localhost/shocky/sql.php");
		
		multiBot = new MultiBotManager(Data.config.getString("main-botname"));
		try {
			multiBot.setName(Data.config.getString("main-botname"));
			multiBot.setLogin(Data.config.getString("main-botname"));
			multiBot.setAutoNickChange(true);
			multiBot.setMessageDelay(500);
			multiBot.setEncoding("UTF8");
			multiBot.setVerbose(Data.config.getBoolean("main-verbose"));
		} catch (Exception e) {e.printStackTrace();}
		multiBot.getListenerManager().addListener(new Shocky());
		
		timed = new TimedActions();
		
		try {
			for (String channel : Data.getChannels()) MultiChannel.join(channel);
			MultiChannel.join(null);
		} catch (Exception e) {e.printStackTrace();}
		
		Module.loadNewModules();
		new ThreadConsoleInput().start();
	}
	
	public static void dataSave() {
		timed.actionPerformed(null);
	}
	
	public static void die() {die(null);}
	public static void die(String reason) {
		isClosing = true;
		Set<PircBotX> bots = getBots();
		for (Module module : Module.getModules(false)) for (PircBotX bot : bots) module.onDie(bot);
		
		if (reason == null) {
			multiBot.disconnectAll();
			killMe();
			return;
		}
		reason = reason.replace("\n"," | ");
		for (PircBotX bot : getBots()) bot.quitServer(reason);
		killMe();
	}
	private static void killMe() {
		while (true) {
			for (PircBotX bot : multiBot.getBots()) if (bot.isConnected()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {e.printStackTrace();}
				continue;
			}
			break;
		}
		
		dataSave();
		System.exit(0);
	}
	
	public static MultiBotManager getBotManager() {
		return multiBot;
	}
	public static Set<PircBotX> getBots() {
		return multiBot.getBots();
	}
	
	public static void send(PircBotX bot, Command.EType type, Command.EType msgChannel, Command.EType msgPrivate, Command.EType msgNotice, Command.EType msgConsole, Channel channel, User user, String message) {
		Command.EType t = null;
		switch (type) {
			case Channel: t = msgChannel; break;
			case Private: t = msgPrivate; break;
			case Notice: t = msgNotice; break;
			case Console: t = msgConsole; break;
		}
		send(bot,t,channel,user,message);
	}
	public static void send(PircBotX bot, Command.EType type, Channel channel, User user, String message) {
		Thread t = Thread.currentThread();
		if (overrideTarget.containsKey(t)) {
			Pair<Command.EType,Command.EType> pair = overrideTarget.get(t);
			if (type == pair.get1()) type = pair.get2();
		}
		switch (type) {
			case Channel: sendChannel(bot,channel,message); break;
			case Private: sendPrivate(bot,user,message); break;
			case Notice: sendNotice(bot,user,message); break;
			case Console: sendConsole(message); break;
		}
	}
	public static void sendChannel(PircBotX bot, Channel channel, String message) {
		for (String line : message.split("\n")) bot.sendMessage(channel,line);
	}
	public static void sendPrivate(PircBotX bot, User user, String message) {
		for (String line : message.split("\n")) bot.sendMessage(user,line);
	}
	public static void sendNotice(PircBotX bot, User user, String message) {
		for (String line : message.split("\n")) bot.sendNotice(user,line);
	}
	public static void sendConsole(String message) {
		for (String line : message.split("\n")) System.out.println(Colors.removeFormattingAndColors(line));
	}
	
	public static User getUser(String user) {
		if (user == null) return null;
		for (PircBotX bot : getBots()) if (getUser(bot,user) != null) return getUser(bot,user);
		return null;
	}
	public static User getUser(PircBotX bot, String user) {
		if (user == null) return null;
		User u = bot.getUser(user);
		if (u.getHostmask().isEmpty()) return null;
		return u;
	}
	public static String getLogin(User user) {
		if (user == null) return null;
		return Whois.getWhoisLogin(user);
	}
	
	public static PircBotX getBotForChannel(String channel) {
		for (PircBotX bot : getBots()) for (Channel c : bot.getChannels()) if (c.getName().equals(channel)) return bot;
		return null;
	}
	public static Channel getChannel(String channel) {
		for (PircBotX bot : getBots()) for (Channel c : bot.getChannels()) if (c.getName().equals(channel)) return c;
		return null;
	}
	public static boolean isChannel(String channame) {
		for (int i = 0; i < MultiChannel.channelPrefixes.length(); i++) if (channame.charAt(0) == MultiChannel.channelPrefixes.charAt(i)) return true;
		return false;
	}
	
	public static boolean isClosing() {
		return isClosing;
	}
	
	public void onNickChange(NickChangeEvent<PircBotX> event) {
		Whois.renameWhois(event);
	}
	public void onQuit(QuitEvent<PircBotX> event) {
		if (event.getUser().getNick().equals(event.getBot().getNick())) return;
		Whois.clearWhois(event.getUser());
	}
	public void onPart(PartEvent<PircBotX> event) {
		if (event.getUser().getNick().equals(event.getBot().getNick())) return;
		Whois.clearWhois(event.getUser());
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.getBlacklistNicks().contains(event.getUser().getNick().toLowerCase())) return;
		Command cmd = Command.getCommand(event.getBot(),Command.EType.Channel,event.getMessage());
		if (cmd != null) cmd.doCommand(event.getBot(),Command.EType.Channel,event.getChannel(),event.getUser(),event.getMessage());
	}
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
		if (Data.getBlacklistNicks().contains(event.getUser().getNick().toLowerCase())) return;
		Command cmd = Command.getCommand(event.getBot(),Command.EType.Private,event.getMessage());
		if (cmd != null) cmd.doCommand(event.getBot(),Command.EType.Private,null,event.getUser(),event.getMessage());
	}
	public void onNotice(NoticeEvent<PircBotX> event) {
		if (event.getUser().getNick().equals("NickServ")) return;
		if (Data.getBlacklistNicks().contains(event.getUser().getNick().toLowerCase())) return;
		Command cmd = Command.getCommand(event.getBot(),Command.EType.Notice,event.getMessage());
		if (cmd != null) cmd.doCommand(event.getBot(),Command.EType.Notice,null,event.getUser(),event.getMessage());
	}
	public void onKick(KickEvent<PircBotX> event) {
		if (event.getRecipient().getNick().equals(event.getBot().getNick())) try {
			MultiChannel.part(event.getChannel().getName());
		} catch (Exception e) {e.printStackTrace();}
	}
}
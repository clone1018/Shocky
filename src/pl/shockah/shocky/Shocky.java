package pl.shockah.shocky;

import java.util.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pircbotx.*;
import org.pircbotx.hooks.events.*;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.cmds.CommandCallback;

public class Shocky extends ListenerAdapter {
	public static Map<Thread,ImmutablePair<Command.EType,Command.EType>> overrideTarget = Collections.synchronizedMap(new HashMap<Thread,ImmutablePair<Command.EType,Command.EType>>());
	private static TimedActions timed;
	private static MultiBotManager multiBot;
	private static boolean isClosing = false;
	
	public static void main(String[] args) {
		Data.load();
				
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
		
		Module.loadNewModules();
		System.out.println("--- Shocky, the IRC bot, up and running! ---");
		System.out.println("--- type \"help\" to list all available commands ---");
		try {
			MultiChannel.join(Data.channels.toArray(new String[0]));
		} catch (Exception e) {e.printStackTrace();}
		
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
			ImmutablePair<Command.EType,Command.EType> pair = overrideTarget.get(t);
			if (type == pair.getLeft()) type = pair.getRight();
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
		if (Data.isBlacklisted(event.getUser())) return;
		Command cmd = Command.getCommand(event.getBot(),Command.EType.Channel,event.getMessage());
		if (cmd != null) {
			CommandCallback callback = new CommandCallback();
			cmd.doCommand(event.getBot(),Command.EType.Channel,callback,event.getChannel(),event.getUser(),event.getMessage());
			if (callback.length()>0 && callback.type == EType.Channel) {
				callback.insert(0,": ");
				callback.insert(0,event.getUser().getNick());
			}
			if (callback.length()>0)
				send(event.getBot(),callback.type==EType.Notice?EType.Notice:Command.EType.Channel,event.getChannel(),event.getUser(),callback.toString());
		}
	}
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		Command cmd = Command.getCommand(event.getBot(),Command.EType.Private,event.getMessage());
		if (cmd != null) {
			CommandCallback callback = new CommandCallback();
			cmd.doCommand(event.getBot(),Command.EType.Private,callback,null,event.getUser(),event.getMessage());
			if (callback.length()>0)
				send(event.getBot(),Command.EType.Private,null,event.getUser(),callback.toString());
		}
	}
	public void onNotice(NoticeEvent<PircBotX> event) {
		if (event.getUser().getNick().equals("NickServ")) return;
		if (Data.isBlacklisted(event.getUser())) return;
		Command cmd = Command.getCommand(event.getBot(),Command.EType.Notice,event.getMessage());
		if (cmd != null) {
			CommandCallback callback = new CommandCallback();
			cmd.doCommand(event.getBot(),Command.EType.Notice,callback,null,event.getUser(),event.getMessage());
			if (callback.length()>0)
				send(event.getBot(),Command.EType.Notice,event.getChannel(),event.getUser(),callback.toString());
		}
	}
	public void onKick(KickEvent<PircBotX> event) {
		if (event.getRecipient().getNick().equals(event.getBot().getNick())) try {
			MultiChannel.lostChannel(event.getBot(), event.getChannel().getName());
		} catch (Exception e) {e.printStackTrace();}
	}

	@Override
	public void onServerResponse(ServerResponseEvent<PircBotX> event)
			throws Exception {
		switch(event.getCode()) {
		case 471://Cannot join channel (+l)
		case 473://Cannot join channel (+i)
		case 474://Cannot join channel (+b)
		case 475://Cannot join channel (+k)
		case 477://You need a registered nick to join that channel.
		case 485://Cannot join channel (reason)
			MultiChannel.lostChannel(event.getBot(), event.getResponse().split(" ")[1]);
			break;
		}
	}
}
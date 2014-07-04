package pl.shockah.shocky;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import org.pircbotx.*;
import org.pircbotx.hooks.events.*;

import pl.shockah.Reflection;
import pl.shockah.shocky.cmds.AuthorizationException;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.sql.SQL;
import pl.shockah.shocky.threads.SandboxSecurityManager;

public class Shocky extends ListenerAdapter {
	private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
	private static final RunnableSave saver = new RunnableSave();
	private static ScheduledFuture<?> futureSave = null;
	private static MultiBotManager multiBot;
	private static boolean isClosing = false;
	private static SandboxSecurityManager secure;
	
	public static void main(String[] args) {
		System.setProperty("http.keepAlive", "false");
		Data.load();
		SQL.init();
				
		multiBot = new ShockyMultiBotManager(Data.config.getString("main-botname"));
		try {
			multiBot.setName(Data.config.getString("main-botname"));
			multiBot.setLogin(Data.config.getString("main-botname"));
			multiBot.setAutoNickChange(true);
			multiBot.setMessageDelay(Data.config.getInt("main-messagedelay"));
			multiBot.setEncoding("UTF8");
			multiBot.setVerbose(Data.config.getBoolean("main-verbose"));
			multiBot.setListenerManager(new ShockyListenerManager<PircBotX>());
		} catch (Exception e) {e.printStackTrace();}
		multiBot.getListenerManager().addListener(new Shocky());
		
		Module.loadNewModules();
		Utils.initPasteServices();
		
		System.out.println("--- Shocky, the IRC bot, up and running! ---");
		System.out.println("--- type \"help\" to list all available commands ---");
		
		try {
			MultiChannel.join(Data.channels.toArray(new String[0]));
		} catch (Exception e) {e.printStackTrace();}
		
		int delay = Data.config.getInt("main-saveinterval");
		if (delay <= 0)
			delay = 300;
		futureSave = timer.scheduleAtFixedRate(saver, delay, delay, TimeUnit.MINUTES);
		
		new ThreadConsoleInput().start();
		List<File> files = new ArrayList<File>();
		for (Module module : Module.getModules())
		{
			File[] fileArray = module.getReadableFiles();
			for (int i = 0; i < fileArray.length; ++i)
				files.add(fileArray[i]);
		}
		secure = new SandboxSecurityManager(files.toArray(new File[0]));
		System.setSecurityManager(secure);
	}
	
	public static void dataSave() {
		saver.run();
	}
	
	public static long nextSave(TimeUnit unit) {
		if (futureSave == null)
			return -1;
		return futureSave.getDelay(unit);
	}
	
	public static void die() {die(null);}
	public static void die(String reason) {
		timer.shutdown();
		isClosing = true;
		Set<PircBotX> bots = getBots();
		for (Module module : Module.getModules(false)) for (PircBotX bot : bots) module.onDie(bot);
		
		if (reason == null) {
			multiBot.disconnectAll();
			killMe();
			return;
		}
		reason = reason.replace("\n"," | ");
		for (PircBotX bot : bots) {
			bot.quitServer(reason);
		}
		WebServer.stop();
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
			case Action:
			case Channel: t = msgChannel; break;
			case Private: t = msgPrivate; break;
			case Notice: t = msgNotice; break;
			case Console: t = msgConsole; break;
		}
		send(bot,t,channel,user,message);
	}
	public static void send(PircBotX bot, Command.EType type, Channel channel, User user, String message) {
		switch (type) {
			case Action:
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
	public static void sendAction(PircBotX bot, Channel channel, String message) {
		for (String line : message.split("\n")) bot.sendAction(channel,line);
	}
	public static void sendAction(PircBotX bot, User user, String message) {
		for (String line : message.split("\n")) bot.sendAction(user,line);
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
	
	@Override
	public void onConnect(ConnectEvent<ShockyBot> event) throws Exception {
		PircBotX bot = event.getBot();
		bot.sendRawLine("CAP LS");
		bot.sendRawLine("CAP REQ account-notify");
		bot.sendRawLine("CAP END");
	}

	/*public void onNickChange(NickChangeEvent<PircBotX> event) {
		Whois.renameWhois(event);
	}*/
	public void onQuit(QuitEvent<ShockyBot> event) {
		if (event.getUser().getNick().equals(event.getBot().getNick())) return;
		Whois.clearWhois(event.getUser());
	}
	public void onPart(PartEvent<ShockyBot> event) {
		if (event.getUser().getNick().equals(event.getBot().getNick())) return;
		Whois.clearWhois(event.getUser());
	}
	
	public void onMessage(MessageEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		String message = event.getMessage().trim();
		if (message.length()<=1) return;
		if (!Data.forChannel(event.getChannel()).getString("main-cmdchar").contains(message.substring(0, 1))) {
			URLDispatcher.findURLs(event.getBot(), event.getChannel(), event.getUser(), message);
			return;
		}
		CommandCallback callback = new CommandCallback();
		callback.targetUser = event.getUser();
		callback.targetChannel = event.getChannel();
		String[] args = message.split("\\s+", 2);
		Command cmd = Command.getCommand(event.getBot(),event.getUser(),event.getChannel(),Command.EType.Channel,callback,args[0].substring(1));
		if (cmd != null) {
			String s = (args.length == 1) ? "" : args[1];
			Parameters params = new Parameters(event.getBot(),Command.EType.Channel,event.getChannel(),event.getUser(),s);
			try {
				cmd.doCommand(params,callback);
			} catch (AuthorizationException e) {
				sendNotice(event.getBot(),event.getUser(),e.getMessage());
				return;
			}
		}
		if (callback.length()>0) {
			if (callback.type == EType.Channel) {
				callback.insert(0,": ");
				callback.insert(0,event.getUser().getNick());
			}
			send(event.getBot(),callback.type==EType.Notice?EType.Notice:Command.EType.Channel,callback.targetChannel,callback.targetUser,callback.toString());
		}
	}
	public void onPrivateMessage(PrivateMessageEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		CommandCallback callback = new CommandCallback();
		String[] args = event.getMessage().split("\\s+", 2);
		Command cmd = Command.getCommand(event.getBot(),event.getUser(),null,Command.EType.Private,callback,args[0]);
		if (cmd != null) {
			String s = (args.length == 1) ? "" : args[1];
			Parameters params = new Parameters(event.getBot(),Command.EType.Private,null,event.getUser(),s);
			try {
				cmd.doCommand(params,callback);
			} catch (AuthorizationException e) {
				sendPrivate(event.getBot(),event.getUser(),e.getMessage());
				return;
			}
		} else {
			URLDispatcher.findURLs(event.getBot(), null, event.getUser(), event.getMessage());
		}
		if (callback.length()>0)
			send(event.getBot(),Command.EType.Private,null,event.getUser(),callback.toString());
	}
	public void onNotice(NoticeEvent<ShockyBot> event) {
		if (event.getUser().getNick().equals("NickServ")) return;
		if (Data.isBlacklisted(event.getUser())) return;
		CommandCallback callback = new CommandCallback();
		String[] args = event.getMessage().split("\\s+", 2);
		Command cmd = Command.getCommand(event.getBot(),event.getUser(),null,Command.EType.Notice,callback,args[0]);
		if (cmd != null) {
			String s = (args.length == 1) ? "" : args[1];
			Parameters params = new Parameters(event.getBot(),Command.EType.Notice,null,event.getUser(),s);
			try {
				cmd.doCommand(params,callback);
			} catch (AuthorizationException e) {
				sendNotice(event.getBot(),event.getUser(),e.getMessage());
				return;
			}
		}
		if (callback.length()>0)
			send(event.getBot(),Command.EType.Notice,event.getChannel(),event.getUser(),callback.toString());
	}
	
	public void onKick(KickEvent<ShockyBot> event) {
		if (event.getRecipient().equals(event.getBot().getUserBot())) try {
			MultiChannel.lostChannel(event.getChannel().getName());
		} catch (Exception e) {e.printStackTrace();}
	}
	
	@Override
	public void onUserList(UserListEvent<ShockyBot> event) throws Exception {
		PircBotX bot = event.getBot();
		if (event.getChannel()==null || !event.getChannel().getName().startsWith("#")) {
			return;
		}
		Set<User> users = event.getUsers();
		if (users.isEmpty() || (users.size()==1 && users.contains(bot.getUserBot()))) {
			try {
				MultiChannel.lostChannel(event.getChannel().getName());
				bot.partChannel(event.getChannel());
			} catch (Exception e) {e.printStackTrace();}
		}
	}

	@Override
	public void onServerResponse(ServerResponseEvent<ShockyBot> event)
			throws Exception {
		switch(event.getCode()) {
		case 5:
			String[] s = event.getResponse().split(" :", 2);
			serverInfo(event.getBot(),s[0].split(" "));
		case ReplyConstants.ERR_CHANNELISFULL://Cannot join channel (+l)
		case ReplyConstants.ERR_INVITEONLYCHAN://Cannot join channel (+i)
		case ReplyConstants.ERR_BANNEDFROMCHAN://Cannot join channel (+b)
		case ReplyConstants.ERR_BADCHANNELKEY://Cannot join channel (+k)
		case 477://You need a registered nick to join that channel.
		case 485://Cannot join channel (reason)
			MultiChannel.lostChannel(event.getResponse().split(" ")[1]);
			break;
		}
	}
	
	private void serverInfo (PircBotX bot, String[] args) {
		for (int i = 1; i < args.length; ++i) {
			String s = args[i];
			if (s.contentEquals("WHOX")) {
				Method m = Reflection.getPrivateMethod(ServerInfo.class, "setWhoX", boolean.class);
				Reflection.invokeMethod(m, bot.getServerInfo(), true);
			}
		}
	}

	@SuppressWarnings("unused")
	@Override
	public void onUnknown(UnknownEvent<ShockyBot> event) throws Exception {
		ShockyBot bot = event.getBot();
		String line = event.getLine();
		
	    String sourceNick = "";
	    String sourceLogin = "";
	    String sourceHostname = "";

	    StringTokenizer tokenizer = new StringTokenizer(line);
	    String senderInfo = tokenizer.nextToken();
	    String command = tokenizer.nextToken();
	    String target = null;

	    int exclamation = senderInfo.indexOf("!");
	    int at = senderInfo.indexOf("@");
	    if (senderInfo.startsWith(":")) {
	      if ((exclamation > 0) && (at > 0) && (exclamation < at)) {
	        sourceNick = senderInfo.substring(1, exclamation);
	        sourceLogin = senderInfo.substring(exclamation + 1, at);
	        sourceHostname = senderInfo.substring(at + 1);
	      }
	    }
	    command = command.toUpperCase();
	    if (sourceNick.startsWith(":"))
	      sourceNick = sourceNick.substring(1);
	    if (target == null)
	      target = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
	    if (target.startsWith(":")) {
	      target = target.substring(1);
	    }
	    
	    User source = bot.getUser(sourceNick);
	    Channel channel = (target.length() != 0) && (bot.getChannelPrefixes().indexOf(target.charAt(0)) >= 0) ? bot.getChannel(target) : null;
	    String message = line.contains(" :") ? line.substring(line.indexOf(" :") + 2) : "";
	    
	    System.out.append(source != null ? source.toString() : "No Source").append(',').
	    append(channel != null ? channel.toString() : "No Channel").append(',').
	    append(command != null ? command.toString() : "No Command?").append(',').
	    append(target != null ? target.toString() : "No Target?").append(',').
	    append(message).println();
	    
	    if (command.equals("ACCOUNT")) {
	    	if (target.contentEquals("*"))
	    		Whois.clearWhois(source);
	    	else
	    		Whois.setWhois(source, target);
	    }
	}
}
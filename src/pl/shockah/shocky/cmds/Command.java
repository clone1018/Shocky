package pl.shockah.shocky.cmds;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public abstract class Command implements Comparable<Command> {
	private static final SortedMap<String, Command> cmds = Collections.synchronizedSortedMap(new TreeMap<String,Command>());
	
	public static enum EType {
		Channel, Private, Notice, Console;
	}
	
	static {
		addCommands(new CmdController(),new CmdBlacklist(),new CmdRaw(),new CmdDie(),new CmdSave(),new CmdGet(),new CmdSet(),new CmdModule());
		addCommands(new CmdHelp(),new CmdJoin(),new CmdPart());
	}
	
	public static void addCommand(String name, Command commands) {
		cmds.put(name,commands);
	}
	public static void addCommands(Command... commands) {
		for (int i = 0; i < commands.length; i++)
			cmds.put(commands[i].command(),commands[i]);
	}
	public static void addCommands(Map<String,Command> commands) {
		cmds.putAll(commands);
	}
	public static void removeCommands(String... commands) {
		for (int i = 0; i < commands.length; i++)
			cmds.remove(commands[i]);
	}
	public static void removeCommands(Command... commands) {
		for (int i = 0; i < commands.length; i++) {
			boolean more = true;
			while (more)
				more = cmds.values().remove(commands[i]);
		}
	}
	public static Command getCommand(PircBotX bot, User sender, Command.EType type, CommandCallback callback, String message) {
		String[] args = message.split(" ");
		if (args.length==0 || args[0].length()==0)
			return null;
		String cmdName = args[0];
		Map<String,Command> matchMap = getCommands(cmdName);
		if (matchMap.size()==1)
			return matchMap.values().iterator().next();
		else if (matchMap.size()>1)
		{
			String[] keys = matchMap.keySet().toArray(new String[0]);
			String s = String.format("Did you mean: %s or %s",StringTools.implode(keys, 0, keys.length-2, ", "), keys[keys.length-1]);
			callback.type = EType.Notice;
			callback.append(s);
		}
		return null;
	}
	public static Map<String,Command> getCommands() {
		return Collections.unmodifiableMap(cmds);
	}
	public static Map<String,Command> getCommands(String cmdName) {
		TreeMap<String,Command> matchMap = new TreeMap<String,Command>();
		for (Entry<String, Command> cmd : cmds.entrySet()) {
			if (cmd.getKey().equals(cmdName))
				return Collections.singletonMap(cmd.getKey(), cmd.getValue());
			if (cmd.getKey().startsWith(cmdName))
				matchMap.put(cmd.getKey(), cmd.getValue());
		}
		return matchMap;
	}
	public static boolean matches(Command command, PircBotX bot, EType type, String cmd) {
		if (type == EType.Channel) {
			if (cmd.length()<=1)
				return false;
			if (!Data.config.getString("main-cmdchar").contains(cmd.substring(0, 1)))
				return false;
			cmd=cmd.substring(1);
		}
		return getCommands(cmd).keySet().size()==1;
	}
	
	public final int compareTo(Command command) {
		return command().compareTo(command.command());
	}
	
	public abstract String command();
	public abstract String help(PircBotX bot, EType type, Channel channel, User sender);
	public void visible(PircBotX bot, EType type, Channel channel, User sender) {}
	public abstract void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message);
	
	public String toString() {
		return command();
	}
	
	public static boolean isController(PircBotX bot, EType type, User user) {
		if (bot == null) return true;
		if (bot.getInetAddress().isLoopbackAddress()) return true;
		if (type == EType.Console) return true;
		if (Shocky.getLogin(user) == null) return false;
		return Data.controllers.contains(Shocky.getLogin(user));
	}
	public static boolean canUseController(PircBotX bot, EType type, User user) {
		if (isController(bot,type,user)) return true;
		Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,null,user,"Restricted command");
		return false;
	}
	
	public static boolean isOp(PircBotX bot, EType type, Channel channel, User user) {
		if (type == EType.Console) return false;
		if (channel == null) return false;
		return channel.isOp(user);
	}
	public static boolean canUseOp(PircBotX bot, EType type, Channel channel, User user) {
		if (isOp(bot,type,channel,user)) return true;
		Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,user,"Restricted command");
		return false;
	}
	
	public static boolean canUseAny(PircBotX bot, EType type, Channel channel, User user) {
		if (isController(bot,type,user) || isOp(bot,type,channel,user)) return true;
		Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,null,user,"Restricted command");
		return false;
	}
}
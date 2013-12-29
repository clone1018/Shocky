package pl.shockah.shocky.cmds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;

public abstract class Command implements Comparable<Command> {
	private static final Map<Command, Object> cmdSources = Collections.synchronizedMap(new HashMap<Command, Object>());
	private static final Map<String, Command> cmds = Collections.synchronizedSortedMap(new TreeMap<String, Command>());
	private static final List<String> aliases = Collections.synchronizedList(new ArrayList<String>());
	
	public static enum EType {
		Channel, Action, Private, Notice, Console;
	}
	
	static {
		Command cmdCtrl = new CmdController();
		addCommands(null,cmdCtrl,new CmdBlacklist(),new CmdRaw(),new CmdDie(),new CmdSave(),new CmdGet(),new CmdSet(),new CmdModule());
		addCommands(null,new CmdHelp(),new CmdJoin(),new CmdPart(),new CmdClean());
		
		addCommand(null,"ctrl",cmdCtrl);
	}
	
	public static void addCommand(Object source, String name, Command command) {
		synchronized(cmds) {
			cmdSources.put(command, source);
			cmds.put(name,command);
			aliases.add(name);
		}
	}
	public static void addCommands(Object source, Command... commands) {
		synchronized(cmds) {
			for (int i = 0; i < commands.length; i++) {
				cmdSources.put(commands[i], source);
				cmds.put(commands[i].command(),commands[i]);
			}
		}
	}
	public static void addCommands(Object source, Map<String,Command> commands) {
		synchronized(cmds) {
			for (Entry<String, Command> command : commands.entrySet()) {
				cmdSources.put(command.getValue(), source);
				cmds.put(command.getKey(), command.getValue());
			}
		}
	}
	public static void removeCommands(String... commands) {
		synchronized(cmds) {
			for (int i = 0; i < commands.length; i++) {
				Command cmd = cmds.remove(commands[i]);
				if (cmd != null) {
					cmdSources.remove(cmd);
					aliases.remove(commands[i]);
				}
			}
		}
	}
	public static void removeCommands(Command... commands) {
		synchronized(cmds) {
			for (int i = 0; i < commands.length; i++) {
				boolean more = true;
				while (more)
					more = cmds.values().remove(commands[i]);
				cmdSources.remove(commands[i]);
			}
		}
	}
	public static Command getCommand(PircBotX bot, User sender, Channel channel, Command.EType type, CommandCallback callback, String name) {
		if (name == null || name.isEmpty())
			return null;
		Map<String,Command> matchMap = getCommands(name, channel);
		if (matchMap.size()==1)
			return matchMap.values().iterator().next();
		else if (matchMap.size()>1)
		{
			Object[] keys = matchMap.keySet().toArray();
			String s = String.format("Did you mean: %s or %s",StringTools.implode(keys, 0, keys.length-2, ", "), keys[keys.length-1]);
			callback.type = EType.Notice;
			callback.append(s);
		}
		return null;
	}
	public static Map<String,Command> getCommands() {
		TreeMap<String,Command> map = new TreeMap<String,Command>();
		synchronized(cmds) {
			for (String s : cmds.keySet()) {
				if (!aliases.contains(s))
					map.put(s,cmds.get(s));
			}
		}
		return Collections.unmodifiableSortedMap(map);
	}
	public static Map<String,Command> getCommands(String cmdName, Channel channel) {
		TreeMap<String,Command> matchMap = new TreeMap<String,Command>();
		synchronized(cmds) {
			for (Entry<String, Command> cmd : cmds.entrySet()) {
				if (channel != null && !cmd.getValue().isEnabled(channel.getName()))
					continue;
				if (cmd.getKey().equals(cmdName))
					return Collections.singletonMap(cmd.getKey(), cmd.getValue());
				if (cmd.getKey().startsWith(cmdName))
					matchMap.put(cmd.getKey(), cmd.getValue());
			}
		}
		return matchMap;
	}
	public static boolean matches(Command command, PircBotX bot, EType type, Channel channel, String cmd) {
		if (type == EType.Channel) {
			if (cmd.length()<=1)
				return false;
			if (!Data.forChannel(channel).getString("main-cmdchar").contains(cmd.substring(0, 1)))
				return false;
			cmd=cmd.substring(1);
		}
		return getCommands(cmd, channel).keySet().size()==1;
	}
	
	public final int compareTo(Command command) {
		return command().compareTo(command.command());
	}
	
	public abstract String command();
	public abstract String help(Parameters params);
	public void visible(Parameters params) {}
	public abstract void doCommand(Parameters params, CommandCallback callback);
	
	public String toString() {
		return command();
	}
	
	public boolean isEnabled(String channel) {
		Object source = cmdSources.get(this);
		if (source != null && source instanceof Module) {
			Module module = (Module)source;
			return module.isEnabled(channel);
		}
		return true;
	}
}
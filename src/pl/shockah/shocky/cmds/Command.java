package pl.shockah.shocky.cmds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public abstract class Command implements Comparable<Command> {
	private static final List<Command> cmds = Collections.synchronizedList(new ArrayList<Command>());
	
	public static enum EType {
		Channel(), Private(), Notice(), Console();
	}
	
	static {
		addCommands(new CmdController(),new CmdBlacklist(),new CmdRaw(),new CmdDie(),new CmdSave(),new CmdGet(),new CmdSet(),new CmdModule());
		addCommands(new CmdHelp(),new CmdJoin(),new CmdPart());
	}
	
	public static void addCommands(Command... commands) {
		cmds.addAll(Arrays.asList(commands));
	}
	public static void removeCommands(Command... commands) {
		cmds.removeAll(Arrays.asList(commands));
	}
	public static void removeCommands(String... commands) {
		List<String> list = Arrays.asList(commands);
		for (int i = 0; i < cmds.size(); i++) if (list.contains(cmds.get(i).command())) cmds.remove(i--);
	}
	public static Command getCommand(PircBotX bot, Command.EType type, String message) {
		String[] args = message.split(" ");
		for (Command cmd : cmds) if (args.length>0&&args[0].length()>0&&matches(cmd,bot,type,args[0])) return cmd;
		return null;
	}
	public static ArrayList<Command> getCommands() {
		return new ArrayList<Command>(cmds);
	}
	public static boolean matches(Command command, PircBotX bot, EType type, String cmd) {
		if (type == EType.Channel) {
			String c = cmd.substring(1), cmdchar = Data.config.getString("main-cmdchar");
			for (int i = 0; i < cmdchar.length(); i++) if (cmd.charAt(0) == cmdchar.charAt(i) && command.matches(bot,type,c)) return true;
			return false;
		}
		return command.matches(bot,type,cmd);
	}
	
	public final int compareTo(Command command) {
		return command().compareTo(command.command());
	}
	
	public abstract String command();
	public abstract String help(PircBotX bot, EType type, Channel channel, User sender);
	public abstract boolean matches(PircBotX bot, EType type, String cmd);
	public void visible(PircBotX bot, EType type, Channel channel, User sender) {}
	public abstract void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message);
	
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
package pl.shockah.shocky.cmds;

import java.util.ArrayList;
import java.util.Collections;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;

public class CmdHelp extends Command {
	public String command() {return "help";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		StringBuilder sb = new StringBuilder();
		sb.append("help {command} - shows command's help\n");
		
		ArrayList<Command> cmds = Command.getCommands();
		Collections.sort(cmds);
		for (int i = 0; i < cmds.size(); i++) {
			Command cmd = cmds.get(i);
			if (i != 0) sb.append(", ");
			sb.append(cmd.command());
		}
		return sb.toString();
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		if (args.length == 2) {
			Command cmd = Command.getCommand(bot,type,(type == EType.Channel ? Data.config.getString("main-cmdchar").charAt(0) : "")+args[1]);
			callback.append(cmd == null ? "No such command" : cmd.help(bot,type,channel,sender));
			return;
		}
		callback.append(help(bot,type,channel,sender));
	}
}
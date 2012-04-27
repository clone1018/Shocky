package pl.shockah.shocky.cmds;

import java.util.ArrayList;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public class CmdHelp extends Command {
	public String command() {return "help";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		StringBuilder sb = new StringBuilder();
		sb.append("help {command} - shows command's help\n");
		
		ArrayList<Command> cmds = Command.getCommands();
		for (int i = 0; i < cmds.size(); i++) {
			Command cmd = cmds.get(i);
			if (i != 0) sb.append(", ");
			sb.append(cmd.command());
		}
		return sb.toString();
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		if (args.length == 2) {
			Command cmd = Command.getCommand(bot,type,(type == EType.Channel ? Data.config.getString("main-cmdchar").charAt(0) : "")+args[1]);
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,cmd == null ? "No such command" : cmd.help(bot,type,channel,sender));
			return;
		}
		Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
	}
}
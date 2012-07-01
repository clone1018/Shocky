package pl.shockah.shocky.cmds;

import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

public class CmdHelp extends Command {
	public String command() {return "help";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		StringBuilder sb = new StringBuilder();
		sb.append("help {command} - shows command's help\n");
		
		Map<String, Command> cmds = Command.getCommands();
		int i = 0;
		for (Entry<String, Command> cmd : cmds.entrySet()) {
			if (channel != null && !cmd.getValue().isEnabled(channel.getName()))
				continue;
			if (i != 0) sb.append(", ");
			sb.append(cmd.getKey());
			i++;
		}
		return sb.toString();
	}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		if (args.length == 2) {
			Command cmd = Command.getCommand(bot,sender,channel != null?channel.getName():null,type,callback,args[1]);
			if (cmd != null)
				callback.append(cmd.help(bot,type,channel,sender));
			return;
		}
		callback.append(help(bot,type,channel,sender));
	}
}
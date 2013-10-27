package pl.shockah.shocky.cmds;

import java.util.Map;
import java.util.Map.Entry;

public class CmdHelp extends Command {
	public String command() {return "help";}
	public String help(Parameters params) {
		StringBuilder sb = new StringBuilder();
		sb.append("help {command} - shows command's help\n");
		
		Map<String, Command> cmds = Command.getCommands();
		int i = 0;
		for (Entry<String, Command> cmd : cmds.entrySet()) {
			if (params.channel != null && !cmd.getValue().isEnabled(params.channel.getName()))
				continue;
			if (i != 0) sb.append(", ");
			sb.append(cmd.getKey());
			i++;
		}
		return sb.toString();
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		callback.type = EType.Notice;
		if (params.tokenCount == 1) {
			String cmdname = params.nextParam();
			Command cmd = Command.getCommand(params.bot,params.sender,params.channel,params.type,callback,cmdname);
			if (cmd != null)
				callback.append(cmd.help(params));
		} else {
			callback.append(help(params));
		}
	}
}
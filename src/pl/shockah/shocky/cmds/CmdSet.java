package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;

public class CmdSet extends Command {
	public String command() {return "set";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] set {key} {value} - sets a bot option value";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		
		if (args.length == 3) {
			args[1] = args[1].toLowerCase();
			Data.config.set(args[1],args[2]);
			callback.append("Set "+args[1]+" to "+args[2]);
			return;
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}
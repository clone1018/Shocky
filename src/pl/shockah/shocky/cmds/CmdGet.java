package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;

public class CmdGet extends Command {
	public String command() {return "get";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] get {key} - gets a bot option value";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		
		if (args.length == 2) {
			args[1] = args[1].toLowerCase();
			callback.append(args[1]+": "+Data.config.getString(args[1]));
			return;
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}
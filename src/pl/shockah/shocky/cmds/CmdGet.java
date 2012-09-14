package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.Config;
import pl.shockah.shocky.Data;

public class CmdGet extends Command {
	public String command() {return "get";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] get {key} - gets a bot option value";
	}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		
		int i = 1;
		boolean global = (args.length >= i && args[i].equalsIgnoreCase("."));
		if (global) i++;
		String key = (args.length >= i)?args[i++].toLowerCase():null;
		
		if (global && !canUseController(bot,type,sender)) return;
		else if (!canUseAny(bot,type,channel, sender)) return;
		
		Config config;
		if (global)
			config = Data.config;
		else {
			if (key != null && Data.protectedKeys.contains(key)) {
				callback.append("Key "+key+" is protected");
				return;
			}
			config = Data.forChannel(channel);
		}
		
		if (key != null) {
			callback.append(key+": "+config.getString(key));
			return;
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}
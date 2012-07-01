package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.Config;
import pl.shockah.shocky.Data;

public class CmdSet extends Command {
	public String command() {return "set";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] set {key} {value} - sets a bot option value";
	}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		
		int i = 1;
		boolean global = (args.length > i && args[i].equalsIgnoreCase("."));
		if (global) i++;
		String key = (args.length > i)?args[i++].toLowerCase():null;
		String value = (args.length > i)?args[i++]:null;
		
		if (global && !canUseController(bot,type,sender)) return;
		else if (!canUseAny(bot,type,channel, sender)) return;
		
		Config config;
		if (global)
			config = Data.config;
		else
			config = Data.forChannel(channel);
		
		if (key != null && value != null) {
			config.set(key,value);
			callback.append("Set "+key+" to "+value);
			return;
		} else if (!global && key != null) {
			value = config.getString(key);
			config.remove(key);
			callback.append("Unset "+key+". Was "+value);
			return;
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}
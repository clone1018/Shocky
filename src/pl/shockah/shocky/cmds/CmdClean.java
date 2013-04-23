package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.Module;

public class CmdClean extends Command {
	public String command() {return "clean";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] clean - cleans up certain modules";
	}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		callback.type = EType.Notice;
		for (Module module : Module.getModules())
			module.onCleanup(bot, callback, sender);
	}
}
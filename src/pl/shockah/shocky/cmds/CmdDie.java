package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.Shocky;

public class CmdDie extends Command {
	public String command() {return "die";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] die - shutdowns the bot";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		
		String[] args = message.split(" ");
		if (args.length == 1) Shocky.die(); else Shocky.die(StringTools.implode(args,1," "));
	}
}
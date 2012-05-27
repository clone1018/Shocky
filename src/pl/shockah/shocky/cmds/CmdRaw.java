package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.Shocky;

public class CmdRaw extends Command {
	public String command() {return "raw";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] raw {query} - raw IRC query";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		
		message = StringTools.implode(message.split(" "),1," ");
		if (bot == null) {
			if (message.toUpperCase().startsWith("PRIVMSG #")) bot = Shocky.getBotForChannel(message.split(" ")[1]);
			else bot = Shocky.getBots().iterator().next();
		}
		bot.sendRawLine(message);
	}
}
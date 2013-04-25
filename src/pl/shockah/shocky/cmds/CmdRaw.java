package pl.shockah.shocky.cmds;

import org.pircbotx.PircBotX;
import pl.shockah.shocky.Shocky;

public class CmdRaw extends Command {
	public String command() {return "raw";}
	public String help(Parameters params) {
		return "[r:controller] raw {query} - raw IRC query";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		PircBotX bot = params.bot;
		if (bot == null) {
			if (params.input.toUpperCase().startsWith("PRIVMSG #"))
				bot = Shocky.getBotForChannel(params.input.split(" ")[1]);
			else
				bot = Shocky.getBots().iterator().next();
		}
		bot.sendRawLine(params.input);
	}
}
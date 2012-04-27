package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public class CmdGet extends Command {
	public String command() {return "get";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] get {key} - gets a bot option value";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		String[] args = message.split(" ");
		
		if (args.length == 2) {
			args[1] = args[1].toLowerCase();
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,args[1]+": "+Data.config.getString(args[1]));
			return;
		}
		
		Shocky.sendNotice(bot,sender,help(bot,type,channel,sender));
	}
}
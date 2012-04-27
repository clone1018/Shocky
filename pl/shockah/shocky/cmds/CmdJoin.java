package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.MultiChannel;
import pl.shockah.shocky.Shocky;

public class CmdJoin extends Command {
	public String command() {return "join";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "join {channel} - makes the bot join channel";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		if (args.length == 2) {
			try {
				MultiChannel.join(args[1]);
			} catch (Exception e) {Shocky.sendNotice(bot,sender,"Already in channel "+args[1]);}
			return;
		}
		
		Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
	}
}
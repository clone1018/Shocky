package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.MultiChannel;

public class CmdJoin extends Command {
	public String command() {return "join";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "join {channel} - makes the bot join channel";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		if (args.length == 2) {
			try {
				MultiChannel.join(args[1]);
			} catch (Exception e) {callback.append("Already in channel "+args[1]);}
			return;
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}
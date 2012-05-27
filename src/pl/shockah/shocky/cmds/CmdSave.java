package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Shocky;

public class CmdSave extends Command {
	public String command() {return "save";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		return "[r:controller] save - saves the data";
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		callback.type = EType.Notice;
		Shocky.dataSave();
		callback.append("Saved");
	}
}
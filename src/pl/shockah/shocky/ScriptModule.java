package pl.shockah.shocky;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.cmds.Command.EType;

public abstract class ScriptModule extends Module {
	
	public abstract String identifier();
	
	public abstract String parse(PircBotX bot, EType type, Channel channel, User sender, String code, String message);
}

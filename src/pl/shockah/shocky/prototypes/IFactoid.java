package pl.shockah.shocky.prototypes;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

public interface IFactoid {
	String runFactoid(PircBotX bot, Channel channel, User sender, String message);
}

package pl.shockah.shocky.prototypes;

import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

public interface IFactoid {
	String runFactoid(Map<Integer,Object> cache, PircBotX bot, Channel channel, User sender, String message);
}

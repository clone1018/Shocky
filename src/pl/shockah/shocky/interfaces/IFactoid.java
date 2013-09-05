package pl.shockah.shocky.interfaces;

import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.sql.Factoid;

public interface IFactoid {
	
	String runFactoid(Map<Integer,Object> cache, PircBotX bot, Channel channel, User sender, String message);
	
	Factoid getFactoid(Map<Integer,Object> cache, String channel, String factoid);
	
	Factoid[] getFactoids(Map<Integer,Object> cache, int max, String channel, String factoid);
}

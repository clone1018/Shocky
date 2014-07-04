package pl.shockah.shocky.interfaces;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.Cache;
import pl.shockah.shocky.sql.Factoid;

public interface IFactoid extends IModule {
	String runFactoid(Cache cache, PircBotX bot, Channel channel, User sender, String message) throws Exception;
	
	Factoid getFactoid(Cache cache, Channel channel, String factoid);
	
	Factoid getFactoid(Cache cache, Channel channel, String factoid, boolean forgotten);
	
	Factoid[] getFactoids(Cache cache, int max, Channel channel, String factoid);
	
	Factoid[] getFactoids(Cache cache, int max, Channel channel, String factoid, boolean forgotten);
}

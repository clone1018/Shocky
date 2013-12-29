package org.pircbotx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import pl.shockah.shocky.interfaces.ILogger;

public class ShockyBot extends PircBotX {
	
	private static final List<ILogger> loggers = new ArrayList<ILogger>();

	public ShockyBot() {
		super();
	}

	@Override
	public ShockyChannel getChannel(String name) {
		if (name == null)
			throw new NullPointerException("Can't get a null channel");
		for (Channel curChan : this.userChanInfo.getAValues()) {
			if (curChan.getName().equals(name)) {
				return (ShockyChannel) curChan;
			}
		}
		ShockyChannel chan = new ShockyChannel(this, name);
		this.userChanInfo.putB(chan);
		return chan;
	}

	@Override
	public ShockyUser getUser(String nick) {
		if (nick == null)
			throw new NullPointerException("Can't get a null user");
		if (this.userNickMap.containsKey(nick))
			return (ShockyUser) this.userNickMap.get(nick);
		ShockyUser user = new ShockyUser(this, nick);
		this.userChanInfo.putA(user);
		return user;
	}
	
	@Override
	public Set<User> getUsers(Channel chan) {
	    if (chan == null)
	        throw new NullPointerException("Can't get a null channel");
	    if (!chan.getName().startsWith("#"))
	    {
	    	this.userChanInfo.deleteA(chan);
	    	return Collections.emptySet();
	    }
		return super.getUsers(chan);
	}
	
	public String getChannelPrefixes() {
		return channelPrefixes;
	}
	
	public static void addLogger(ILogger logger)
	{
		synchronized(loggers) {
			loggers.add(logger);
		}
	}
	
	public static void removeLogger(ILogger logger)
	{
		synchronized(loggers) {
			loggers.remove(logger);
		}
	}

	@Override
	public void log(String line) {
		super.log(line);
		synchronized(loggers) {
			if (loggers.isEmpty())
				return;
			for (int i = loggers.size() - 1; i >= 0; --i) {
				try {
					loggers.get(i).log(line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

package org.pircbotx;

import java.util.Collections;
import java.util.Set;

public class ShockyBot extends PircBotX {

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
}

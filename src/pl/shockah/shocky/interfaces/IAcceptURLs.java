package pl.shockah.shocky.interfaces;

import java.net.URL;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

public interface IAcceptURLs {
	boolean shouldAcceptURL(URL u);
	void handleURL(PircBotX bot, Channel channel, User sender, URL u);
}

package pl.shockah.shocky;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.interfaces.IAcceptURLs;

public class URLDispatcher {
	private static final Pattern urlPattern = Pattern.compile("(([a-z]+)://)?([a-z0-9]+(\\.[a-z0-9-]+)+(:[0-9]+)?)(/[a-z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)?",Pattern.CASE_INSENSITIVE);
	private static final List<IAcceptURLs> handles = new ArrayList<IAcceptURLs>();
	
	public static void findURLs(PircBotX bot, Channel channel, User sender, String message) {
		StringTokenizer tokens = new StringTokenizer(message);
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			if ((token.charAt(0)=='('&&token.charAt(token.length()-1)==')')||(token.charAt(0)=='['&&token.charAt(token.length()-1)==']'))
				token = token.substring(1, token.length()-1);
			Matcher m = urlPattern.matcher(token);
			if (!m.find())
				continue;
			String found = m.group();
			String protocol = m.group(2);
			if (protocol == null)
				found = "http://"+found;
			URL u;
			try {u = new URL(found);}
			catch (MalformedURLException e) {continue;}
			synchronized (handles) {
				for (int i = 0; i < handles.size(); ++i) {
					IAcceptURLs h = handles.get(i);
					if (h.shouldAcceptURL(u))
						h.handleURL(bot, channel, sender, u);
				}
			}
		}
	}
	
	public static void addHandler(IAcceptURLs i) {
		synchronized (handles) {
			handles.add(i);
		}
	}
	
	public static void removeHandler(IAcceptURLs i) {
		synchronized (handles) {
			handles.remove(i);
		}
	}
}

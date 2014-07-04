package pl.shockah.shocky;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
		Map<IAcceptURLs, List<URL>> map = null;
		int total = 0;
		while (total < 8 && tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			char c1 = token.charAt(0), c2 = token.charAt(token.length()-1);
			if ((c1=='('&&c2==')')||(c1=='['&&c2==']')||(c1=='{'&&c2=='}')||(c1=='<'&&c2=='>'))
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
					if (!h.shouldAcceptURL(u))
						continue;
					if (map == null) {
						if (!tokens.hasMoreTokens()) {
							h.handleURL(bot, channel, sender, Collections.singletonList(u));
							return;
						}
						map = new LinkedHashMap<IAcceptURLs, List<URL>>(2, 1.0f);
					}
					List<URL> list;
					if (map.containsKey(h))
						list = map.get(h);
					else
						map.put(h, list = new LinkedList<URL>());
					if (!list.contains(u)) {
						list.add(u);
						++total;
					}
				}
			}
		}
		if (map != null) {
			for (Map.Entry<IAcceptURLs, List<URL>> entry : map.entrySet()) {
				IAcceptURLs handler = entry.getKey();
				List<URL> list = entry.getValue();
				int a = 0, b = 0, c = list.size();
				if (c <= 3) {
					handler.handleURL(bot, channel, sender, list);
					continue;
				}
				while (a < c) {
					b += 3;
					if (b > c)
						b = c;
					handler.handleURL(bot, channel, sender, list.subList(a, b));
					a = b;
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

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.interfaces.IAcceptURLs;


public class ModuleSpotify extends Module implements IAcceptURLs {
	
	private Pattern target = Pattern.compile("^/track/([a-zA-Z0-9]+)$");

	@Override
	public void onEnable(File dir) {
		Data.config.setNotExists("spotify-otherbot",false);
	}

	@Override
	public boolean shouldAcceptURL(URL u) {
		if (u == null)
			return false;
		if (!u.getProtocol().startsWith("http"))
			return false;
		return u.getHost().endsWith("open.spotify.com");
	}
	
	@Override
	public void handleURL(PircBotX bot, Channel channel, User sender, List<URL> urls) {
		if (bot == null || urls == null || urls.isEmpty() || (channel == null && sender == null))
			return;
		if (channel != null && (!isEnabled(channel.getName()) || Data.forChannel(channel).getBoolean("spotify-otherbot")))
			return;
		
		StringBuilder sb = new StringBuilder();
		Iterator<URL> iter = urls.iterator();
		while (iter.hasNext()) {
			URL u = iter.next();
			Matcher m = this.target.matcher(u.getPath());
			if (!m.find())
				continue;
		
			String id = m.group(1);
			CharSequence s = info(id);
			if (s == null)
				continue;
			if (urls.size() > 1)
				sb.append(id).append(": ");
			sb.append(s);
			if (iter.hasNext())
				sb.append('\n');
		}
		if (sb.length() == 0)
			return;
		String s = StringTools.limitLength(StringTools.formatLines(sb));
		if (channel != null)
			bot.sendMessage(channel, sender.getNick()+": "+s);
		else if (sender != null)
			bot.sendMessage(sender, s);
	}
	
	public CharSequence info(String id) {
		HTTPQuery q = null;
		
		try {
			q = HTTPQuery.create("http://ws.spotify.com/lookup/1/.json?uri=spotify:track:" + URLEncoder.encode(id, "UTF8"));
			q.connect(true, false);
			
			JSONObject jItem = new JSONObject(q.readWhole()).getJSONObject("track");
			q.close();
			
			String track = jItem.getString("name");
			String released = jItem.getJSONObject("album").getString("released");
			JSONArray artistsArray = jItem.getJSONArray("artists");
			int length = artistsArray.length();
			String[] artists = new String[length];
			for(int i = 0; i < length; i++)
				artists[i] = artistsArray.getJSONObject(i).getString("name");
			int duration = (int)jItem.getDouble("length");
			
			StringBuilder sb = new StringBuilder();
			sb.append(Colors.BOLD).append(track).append(Colors.BOLD);
			sb.append(" | length ").append(Colors.BOLD).append(Utils.timeAgo(duration)).append(Colors.BOLD);
			sb.append(" | released ").append(Colors.BOLD).append(released).append(Colors.BOLD);
			sb.append(" | by ").append(Colors.BOLD).append(StringTools.implode(artists, ", ")).append(Colors.BOLD);
			return sb;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	@Override
	public String name() {
		return "spotify";
	}
}

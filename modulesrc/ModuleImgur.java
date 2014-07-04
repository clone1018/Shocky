import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.Config;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.interfaces.IAcceptURLs;


public class ModuleImgur extends Module implements IAcceptURLs {
	public static enum Request {NONE, IMAGE, ALBUM, GALLERY, GALLERYIMAGE, GALLERYALBUM}
	public static final Pattern urlPattern = Pattern.compile("(?:/(a|gallery))?/(.+)");
	public static final Pattern redditLink = Pattern.compile("^/r/[\\w]+/comments/([0-9a-z]+)/");
	@Override
	public String name() {return "imgur";}
	
	@Override
	public void onEnable(File dir) {
		Data.config.setNotExists("imgur-otherbot",false);
		Data.config.setNotExists("imgur-clientid","");
		Data.protectedKeys.add("imgur-clientid");
	}

	public CharSequence getImageInfo(Channel channel, Request r, String id) {
		return getImageInfo(channel, r, id, null);
	}
	public CharSequence getImageInfo(Channel channel, Request r, String id, String sub) {
		Config cfg = (channel != null) ? Data.forChannel(channel) : Data.config;
		String cid = cfg.getString("imgur-clientid");
		if (cid.isEmpty())
			return null;
			
		String url;
		switch (r) {
			case IMAGE:			url = "https://api.imgur.com/3/image/%1$s"; break;
			case ALBUM:			url = "https://api.imgur.com/3/album/%1$s"; break;
			case GALLERY:		url = "https://api.imgur.com/3/gallery/%1$s"; break;
			case GALLERYIMAGE: 	url = (sub != null) ? "https://api.imgur.com/3/gallery/r/%2$s/%1$s" : "https://api.imgur.com/3/gallery/image/%1$s"; break;
			case GALLERYALBUM:	url = "https://api.imgur.com/3/gallery/album/%1$s"; break;
			default: return null;
		}
		try {
			HTTPQuery q = HTTPQuery.create(String.format(url, id, sub));
			q.connect(true, false);
			q.setHeaderProperty("Authorization", "Client-ID "+cid);
			JSONObject j = new JSONObject(q.readWhole());
			/*for (Entry<String, List<String>> e : q.getConnection().getHeaderFields().entrySet()) {
				System.out.println(e.getKey());
				for (String s : e.getValue())
					System.out.append('\t').append(s).append('\n');
			}*/
			if (!j.getBoolean("success")) {
				JSONObject e = j.getJSONObject("error");
				System.out.println(e.getString("message"));
				return null;
			}
			j = j.getJSONObject("data");
			String title = j.optString("title",null);
			if (title == null) {
				if (sub == null && !j.isNull("section"))
					return getImageInfo(channel, Request.GALLERYIMAGE, id, j.getString("section"));
				if (r != Request.GALLERYIMAGE)
					return getImageInfo(channel, Request.GALLERYIMAGE, id);
				return null;
			}
			
			StringBuilder sb = new StringBuilder();
			NumberFormat nf = NumberFormat.getNumberInstance();
			DateFormat df = DateFormat.getDateInstance();
			Date d = new Date(j.getLong("datetime")*1000L);
			
			sb.append(StringTools.ircFormatted(title,false)).append('\n');
			if (j.optBoolean("nsfw",false))
				sb.append("[NSFW]\n");
			int count = j.optInt("images_count",1);
			if (count != 1)
				sb.append(count).append(" images\n");
			sb.append(nf.format(j.getLong("views"))).append(" views\n");
			sb.append(df.format(d)).append('\n');
			if (j.has("ups") && j.has("downs"))
				sb.append(nf.format(j.getLong("ups"))).append("U ").
				append(nf.format(j.getLong("downs"))).append("D\n");
			if (j.has("reddit_comments")) {
				String reddit = j.getString("reddit_comments");
				Matcher m = redditLink.matcher(reddit);
				if (m.find())
					sb.append("http://redd.it/").append(m.group(1));
				else
					sb.append(Utils.shortenUrl("http://www.reddit.com"+reddit));
			}
			
			return sb;
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean shouldAcceptURL(URL u) {
		if (u == null)
			return false;
		if (!u.getProtocol().startsWith("http"))
			return false;
		String host = u.getHost();
		int i = host.lastIndexOf("imgur.com");
		if (i == -1)
			return false;
		if (i == 0)
			return true;
		String subdomain = host.substring(0, i);
		return subdomain.contentEquals("i.");
	}

	@Override
	public void handleURL(PircBotX bot, Channel channel, User sender, List<URL> urls) {
		if (bot == null || urls == null || urls.isEmpty() || (channel == null && sender == null))
			return;
		if (channel != null && (!isEnabled(channel.getName()) || Data.forChannel(channel).getBoolean("imgur-otherbot")))
			return;
		StringBuilder sb = new StringBuilder();
		Iterator<URL> iter = urls.iterator();
		while (iter.hasNext()) {
			URL u = iter.next();
			boolean iServer = u.getHost().contentEquals("i.imgur.com");
			Matcher m = urlPattern.matcher(u.getPath());
			if (!m.find())
				continue;
			String type = m.group(1);
			String id = m.group(2);
			if (id.indexOf(',')>=0)
				continue;
			int i = id.indexOf('.');
			if (i >= 0) {
				if (!iServer)
					continue;
				id = id.substring(0, i);
			}else {
				if (iServer)
					continue;
			}
			Request r = Request.IMAGE;
			if (type != null)
			{
				if (type.equalsIgnoreCase("a"))
					r = Request.ALBUM;
				else if (type.equalsIgnoreCase("gallery"))
					r = Request.GALLERY;
			}
			CharSequence s = getImageInfo(channel,r,id);
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
			Shocky.sendChannel(bot, channel, s);
		else if (sender != null)
			bot.sendMessage(sender, s);
	}

}

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.ShockyBot;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.IAcceptURLs;

public class ModuleYoutube extends Module implements IAcceptURLs {
	protected Command cmd;
	private ArrayList<Pattern> patternsAction = new ArrayList<Pattern>(), patternsMessage = new ArrayList<Pattern>();
	
	public static CharSequence getVideoInfo(User user, String vID) {
		HTTPQuery q = null;
		String key = Data.config.getString("youtube-key");
		if (key.isEmpty())
			return null;
		try {
			StringBuilder sb = new StringBuilder("https://www.googleapis.com/youtube/v3/videos?part=snippet%2Cstatistics%2CcontentDetails&key=");
			sb.append(URLEncoder.encode(key,"UTF8")).append("&id=").append(URLEncoder.encode(vID,"UTF8")).append("&quotaUser=").append(URLEncoder.encode(user.getHostmask(),"UTF8"));
			sb.append("&fields=items(snippet(channelTitle%2Ctitle)%2CcontentDetails%2Fduration%2Cstatistics(likeCount%2CdislikeCount%2CviewCount))");
			q = HTTPQuery.create(sb.toString());
			q.connect(true,false);
			
			JSONObject jItem = new JSONObject(q.readWhole());
			q.close();
			
			if (jItem.has("error"))
				return "Error: "+jItem.getJSONObject("error").getString("message");
			
			JSONArray items = jItem.optJSONArray("items");
			if (items == null || items.length() == 0)
				return null;
			JSONObject item = items.getJSONObject(0);
			JSONObject snippet = item.getJSONObject("snippet");
			JSONObject statistics = item.getJSONObject("statistics");
			JSONObject contentDetails = item.getJSONObject("contentDetails");
			
			String vUploader = StringTools.unicodeParse(snippet.getString("channelTitle"));
			String vTitle = StringTools.unicodeParse(snippet.getString("title"));
			String vDuration = Utils.timeAgo(contentDetails.getString("duration"));
			double likes = statistics.getInt("likeCount");
			double dislikes = statistics.getInt("dislikeCount");
			long views = statistics.getLong("viewCount");
			
			sb = new StringBuilder();
			sb.append(vTitle);
			sb.append(" | length ").append(vDuration);
			if (likes > 0)
				sb.append(" | rated ").append(String.format("%.0f%%", likes * 100D / (likes + dislikes)));
			sb.append(" | ").append(NumberFormat.getNumberInstance().format(views)).append(" view");
			if (views != 1)
				sb.append('s');
			sb.append(" | by ").append(vUploader);
			return sb.toString();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static CharSequence getVideoSearch(User user, String query, boolean data, boolean url) {
		HTTPQuery q = null;
		String key = Data.config.getString("youtube-key");
		if (key.isEmpty())
			return null;
		try {
			StringBuilder sb = new StringBuilder("https://www.googleapis.com/youtube/v3/search?safeSearch=none&part=snippet&type=video&maxResults=1&fields=items%2Fid%2FvideoId&key=");
			sb.append(URLEncoder.encode(key,"UTF8")).append("&q=").append(URLEncoder.encode(query,"UTF8")).append("&quotaUser=").append(URLEncoder.encode(user.getHostmask(),"UTF8"));
			q = HTTPQuery.create(sb.toString());
			q.connect(true,false);
			
			JSONObject jItem = new JSONObject(q.readWhole());
			q.close();
			
			if (jItem.has("error"))
				return "Error: "+jItem.getJSONObject("error").getString("message");
			
			JSONArray items = jItem.optJSONArray("items");
			if (items == null || items.length() == 0)
				return null;
			
			sb = new StringBuilder();
			JSONObject item = items.getJSONObject(0);
			String vID = item.getJSONObject("id").getString("videoId");
			if (data) {
				CharSequence info = getVideoInfo(user, vID);
				if (info == null)
					return null;
				sb.append(info);
				if (url)
					sb.append(" | ");
			}
			if (url)
				sb.append("http://youtu.be/").append(vID);
			
			return sb.toString();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public String name() {return "youtube";}
	public boolean isListener() {return true;}
	public void onEnable(File dir) {
		Data.config.setNotExists("yt-otherbot",false);
		Data.config.setNotExists("youtube-key","");
		Data.protectedKeys.add("youtube-key");
		
		Command.addCommands(this, cmd = new CmdYoutube());
		Command.addCommand(this, "yt", cmd);
		Command.addCommand(this, "y", cmd);
		
		patternsAction.add(Pattern.compile("^.*?(?:(?:playing)|(?:listening (?:to)?)):? (.+)$"));
		patternsMessage.add(Pattern.compile("^np: (.*)$"));
	}
	public void onDisable() {
		patternsAction.clear();
		patternsMessage.clear();
		
		Command.removeCommands(cmd);
	}
	
	@Override
	public boolean shouldAcceptURL(URL u) {
		if (u == null)
			return false;
		if (!u.getProtocol().startsWith("http"))
			return false;
		String host = u.getHost();
		return host.contentEquals("youtu.be") || host.endsWith("youtube.com");
	}
	@Override
	public void handleURL(PircBotX bot, Channel channel, User sender, List<URL> urls) {
		if (bot == null || urls == null || urls.isEmpty() || (channel == null && sender == null))
			return;
		if (channel != null && (!isEnabled(channel.getName()) || Data.forChannel(channel).getBoolean("yt-otherbot")))
			return;
		
		StringBuilder sb = new StringBuilder();
		Iterator<URL> iter = urls.iterator();
		while (iter.hasNext()) {
			URL u = iter.next();
			
			String id = null;
			String host = u.getHost();
			if (host.contentEquals("youtu.be")) {
				if (u.getPath().isEmpty())
					continue;
				id = u.getPath().substring(1);
			}
			else if (host.endsWith("youtube.com")) {
				StringTokenizer tok1 = new StringTokenizer(u.getQuery(),"&");
				while (tok1.hasMoreTokens()) {
					StringTokenizer tok2 = new StringTokenizer(tok1.nextToken(),"=");
					if (tok2.countTokens()!=2)
						continue;
					String key = tok2.nextToken();
					String value = tok2.nextToken();
					if (key.contentEquals("v")) {
						id = value;
						break;
					}
				}
			}
		
			if (id == null)
				continue;
			CharSequence result = getVideoInfo(sender, id);
			if (result == null)
				continue;
			if (urls.size() > 1)
				sb.append(id).append(": ");
			sb.append(result);
			if (iter.hasNext())
				sb.append('\n');
		}
		if (sb.length() == 0)
			return;
		String result = StringTools.limitLength(StringTools.formatLines(sb));
		if (channel != null)
			bot.sendMessage(channel, result);
		else if (sender != null)
			bot.sendMessage(sender, result);
	}
	
	public void onMessage(MessageEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		handleEvent(event.getBot(),event.getChannel(),event.getUser(),event.getMessage(),patternsMessage);
		
		/*if (!Data.forChannel(event.getChannel()).getBoolean("yt-otherbot")) {
			Matcher m = patternURL.matcher(event.getMessage());
			while (m.find()) {
				String vID = m.group(1);
				if (vID == null) vID = m.group(2);
				String result = getVideoInfo(vID);
				if (result == null) return;
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+result);
			}
		}*/
	}
	public void onAction(ActionEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		handleEvent(event.getBot(),event.getChannel(),event.getUser(),event.getAction(),patternsAction);
	}
	
	private static void handleEvent(PircBotX bot, Channel channel, User user, CharSequence cs, Iterable<Pattern> patterns) {
		for (Pattern p : patterns) {
			Matcher m = p.matcher(cs);
			if (m.find()) {
				String s = m.group(1);
				if (s.startsWith("http://") || s.startsWith("www.//") || s.startsWith("youtu.be/") || s.startsWith("youtube/")) return;
				CharSequence result = getVideoSearch(user,s,!Data.forChannel(channel).getBoolean("yt-otherbot"),true);
				if (result == null) return;
				s = Utils.mungeAllNicks(channel,0,result);
				Shocky.sendChannel(bot,channel,user.getNick()+": "+s);
				break;
			}
		}
	}
	
	public class CmdYoutube extends Command {
		public String command() {return "youtube";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("youtube/yt/y");
			sb.append("\nyoutube {query} - returns the first YouTube search result");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			CharSequence search = getVideoSearch(params.sender,params.input,!Data.forChannel(params.channel).getBoolean("yt-otherbot"),true);
			if (search != null && search.length() > 0) {
				search = Utils.mungeAllNicks(params.channel,0,search,params.sender);
				callback.append(search);
			} else {
				callback.type = EType.Notice;
				callback.append("No results were found.");
			}
		}
	}
}
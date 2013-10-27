import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
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
import pl.shockah.shocky.URLDispatcher;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.IAcceptURLs;

public class ModuleYoutube extends Module implements IAcceptURLs {
	protected Command cmd;
	private ArrayList<Pattern> patternsAction = new ArrayList<Pattern>(), patternsMessage = new ArrayList<Pattern>();
	//private Pattern patternURL = Pattern.compile("https?://(?:(?:(?:www\\.)?youtube\\.com/watch\\?.*?v=([a-zA-Z0-9_\\-]+))|(?:(?:www\\.)?youtu\\.be/([a-zA-Z0-9_\\-]+)))");
	
	public static String getVideoInfo(String vID) {
		HTTPQuery q = null;
		
		try {
			q = HTTPQuery.create("http://gdata.youtube.com/feeds/api/videos/"+URLEncoder.encode(vID,"UTF8")+"?v=2&alt=jsonc");
			q.connect(true,false);
			
			JSONObject jItem = new JSONObject(q.readWhole()).getJSONObject("data");
			q.close();
			
			String vUploader = jItem.getString("uploader");
			String vTitle = StringTools.unicodeParse(jItem.getString("title"));
			int vDuration = jItem.getInt("duration");
			double vRating = jItem.has("rating") ? jItem.getDouble("rating") : -1;
			int vViewCount = jItem.getInt("viewCount");
			
			StringBuilder sb = new StringBuilder();
			sb.append(vTitle);
			sb.append(" | length ").append(Utils.timeAgo(vDuration));
			if (vRating != -1)
				sb.append(" | rated ").append(String.format("%.2f",vRating).replace(',','.')).append("/5.00");
			sb.append(" | ").append(vViewCount).append(" view");
			if (vViewCount != 1)
				sb.append('s');
			sb.append(" | by ").append(vUploader);
			return sb.toString();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static String getVideoSearch(String query, boolean data, boolean url) {
		HTTPQuery q = null;
		
		try {
			q = HTTPQuery.create("http://gdata.youtube.com/feeds/api/videos?max-results=1&v=2&alt=jsonc&q="+URLEncoder.encode(query,"UTF8"));
			q.connect(true,false);
			
			JSONObject jItem = new JSONObject(q.readWhole()).getJSONObject("data");
			q.close();
			
			if (jItem.getInt("totalItems")==0)
				return null;
			jItem = jItem.getJSONArray("items").getJSONObject(0);
			
			String vID = jItem.getString("id");
			String vUploader = jItem.getString("uploader");
			String vTitle = jItem.getString("title");
			int vDuration = jItem.getInt("duration");
			double vRating = jItem.has("rating") ? jItem.getDouble("rating") : -1;
			int vViewCount = jItem.getInt("viewCount");
			
			StringBuilder sb = new StringBuilder();
			if (data) {
				sb.append(Colors.BOLD).append(vTitle).append(Colors.NORMAL);
				sb.append(" | length ").append(Colors.BOLD).append(Utils.timeAgo(vDuration)).append(Colors.NORMAL);
				if (vRating != -1)
					sb.append(" | rated ").append(Colors.BOLD).append(String.format("%.2f",vRating).replace(',','.')).append("/5.00").append(Colors.NORMAL);
				sb.append(" | ").append(Colors.BOLD).append(vViewCount).append(Colors.NORMAL).append(" view");
				if (vViewCount != 1)
					sb.append('s');
				sb.append(" | by ").append(Colors.BOLD).append(vUploader).append(Colors.NORMAL);
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
		
		Command.addCommands(this, cmd = new CmdYoutube());
		Command.addCommand(this, "yt", cmd);
		Command.addCommand(this, "y", cmd);
		URLDispatcher.addHandler(this);
		
		patternsAction.add(Pattern.compile("^.*?(?:(?:playing)|(?:listening (?:to)?)):? (.+)$"));
		patternsMessage.add(Pattern.compile("^np: (.*)$"));
	}
	public void onDisable() {
		patternsAction.clear();
		patternsMessage.clear();
		
		Command.removeCommands(cmd);
		URLDispatcher.removeHandler(this);
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
	public void handleURL(PircBotX bot, Channel channel, User sender, URL u) {
		if (bot == null || u == null || (channel == null && sender == null))
			return;
		if (channel != null && (!isEnabled(channel.getName()) || Data.forChannel(channel).getBoolean("yt-otherbot")))
			return;
		
		String id = null;
		String host = u.getHost();
		
		if (host.contentEquals("youtu.be")) {
			if (u.getPath().isEmpty())
				return;
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
			return;
		String result = getVideoInfo(id);
		if (result == null)
			return;
		
		if (channel != null)
			bot.sendMessage(channel, sender.getNick()+": "+result);
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
				String result = getVideoSearch(s,!Data.forChannel(channel).getBoolean("yt-otherbot"),true);
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
			
			String search = getVideoSearch(params.input,!Data.forChannel(params.channel).getBoolean("yt-otherbot"),true);
			if (search != null && !search.isEmpty()) {
				search = Utils.mungeAllNicks(params.channel,0,search,params.sender);
				callback.append(search);
			} else {
				callback.type = EType.Notice;
				callback.append("No results were found.");
			}
		}
	}
}
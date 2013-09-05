import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
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

public class ModuleYoutube extends Module {
	protected Command cmd;
	private ArrayList<Pattern> patternsAction = new ArrayList<Pattern>(), patternsMessage = new ArrayList<Pattern>();
	private Pattern patternURL = Pattern.compile("https?://(?:(?:(?:www\\.)?youtube\\.com/watch\\?.*?v=([a-zA-Z0-9_\\-]+))|(?:(?:www\\.)?youtu\\.be/([a-zA-Z0-9_\\-]+)))");
	
	public String getVideoInfo(String vID) {
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
	public String getVideoSearch(String query, boolean data, boolean url) {
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
		
		patternsAction.add(Pattern.compile("^.*?(?:(?:playing)|(?:listening (?:to)?)):? (.+)$"));
		patternsMessage.add(Pattern.compile("^np: (.*)$"));
	}
	public void onDisable() {
		patternsAction.clear();
		patternsMessage.clear();
		Command.removeCommands(cmd);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		
		for (Pattern p : patternsMessage) {
			Matcher m = p.matcher(event.getMessage());
			if (m.find()) {
				String s = m.group(1);
				if (s.startsWith("http://") || s.startsWith("www.//") || s.startsWith("youtu.be/") || s.startsWith("youtube/")) return;
				String result = getVideoSearch(s,!Data.forChannel(event.getChannel()).getBoolean("yt-otherbot"),true);
				if (result == null) return;
				s = Utils.mungeAllNicks(event.getChannel(),0,result);
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+s);
				break;
			}
		}
		
		if (!Data.forChannel(event.getChannel()).getBoolean("yt-otherbot")) {
			Matcher m = patternURL.matcher(event.getMessage());
			while (m.find()) {
				String vID = m.group(1);
				if (vID == null) vID = m.group(2);
				String result = getVideoInfo(vID);
				if (result == null) return;
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+result);
			}
		}
	}
	public void onAction(ActionEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		
		for (Pattern p : patternsAction) {
			Matcher m = p.matcher(event.getAction());
			if (m.find()) {
				String s = m.group(1);
				if (s.startsWith("http://") || s.startsWith("www.//") || s.startsWith("youtu.be/") || s.startsWith("youtube/")) return;
				String result = getVideoSearch(s,!Data.forChannel(event.getChannel()).getBoolean("yt-otherbot"),true);
				if (result == null) return;
				s = Utils.mungeAllNicks(event.getChannel(),0,result);
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+s);
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
				search = Utils.mungeAllNicks(params.channel,0,search,params.sender.getNick());
				callback.append(search);
			} else {
				callback.type = EType.Notice;
				callback.append("No results were found.");
			}
		}
	}
}
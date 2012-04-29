import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.HTTPQuery;
import pl.shockah.JSONObject;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;

public class ModuleYoutube extends Module {
	protected Command cmd;
	private ArrayList<Pattern> patternsAction = new ArrayList<Pattern>(), patternsMessage = new ArrayList<Pattern>();
	private Pattern patternURL = Pattern.compile("https?://(?:(?:(?:www\\.)?youtube\\.com/watch\\?.*?v=([a-zA-Z0-9_\\-]+))|(?:(?:www\\.)?youtu\\.be/([a-zA-Z0-9_\\-]+)))");
	
	public String getVideoInfo(String vID) {
		HTTPQuery q = null;
		
		try {
			q = new HTTPQuery("http://gdata.youtube.com/feeds/api/videos/"+URLEncoder.encode(vID,"UTF8")+"?v=2&alt=jsonc","GET");
			q.connect(true,false);
		} catch (Exception e) {e.printStackTrace();}
		
		JSONObject jItem = JSONObject.deserialize(q.readWhole()).getJSONObject("data");
		q.close();
		
		String vUploader = jItem.getString("uploader");
		String vTitle = jItem.getString("title");
		int vDuration = jItem.getInt("duration");
		double vRating = jItem.exists("rating") ? jItem.getDouble("rating") : -1;
		int vViewCount = jItem.getInt("viewCount");
		
		int iDh = vDuration/3600, iDm = (vDuration/60) % 60, iDs = vDuration % 60;
		
		return vTitle+" | length "+(vDuration >= 3600 ? iDh+"h " : "")+(vDuration >= 60 ? iDm+"m " : "")+iDs+"s | rated "
			+(vRating != -1 ? String.format("%.2f",vRating).replace(",",".")+"/5.00 | " : "")+vViewCount+" view"+(vViewCount != 1 ? "s" : "") +" | by "+vUploader;
	}
	public String getVideoSearch(String query, boolean data, boolean url) {
		HTTPQuery q = null;
		
		try {
			q = new HTTPQuery("http://gdata.youtube.com/feeds/api/videos?max-results=1&v=2&alt=jsonc&q="+URLEncoder.encode(query,"UTF8"),"GET");
			q.connect(true,false);
		} catch (Exception e) {e.printStackTrace();}
		
		JSONObject jItem = JSONObject.deserialize(q.readWhole()).getJSONObject("data").getJSONObjectArray("items")[0];
		q.close();
		
		String vID = jItem.getString("id");
		String vUploader = jItem.getString("uploader");
		String vTitle = jItem.getString("title");
		int vDuration = jItem.getInt("duration");
		double vRating = jItem.exists("rating") ? jItem.getDouble("rating") : -1;
		int vViewCount = jItem.getInt("viewCount");
		
		int iDh = vDuration/3600, iDm = (vDuration/60) % 60, iDs = vDuration % 60;
		
		return (data ? vTitle+" | length "+(vDuration >= 3600 ? iDh+"h " : "")+(vDuration >= 60 ? iDm+"m " : "")+iDs+"s | rated "
			+(vRating != -1 ? String.format("%.2f",vRating).replace(",",".")+"/5.00 | " : "")+vViewCount+" view"+(vViewCount != 1 ? "s" : "")
			+" | by "+vUploader+(url ? " | " : "") : "")+(url ? "http://youtu.be/"+vID : "");
	}
	
	public String name() {return "youtube";}
	public void load() {
		Data.config.setNotExists("yt-otherbot",false);
		Command.addCommands(cmd = new CmdYoutube());
		
		patternsAction.add(Pattern.compile("^.*(?:(?:playing)|(?:listening))(?: to)?:? (.*)$"));
		patternsMessage.add(Pattern.compile("^np: (.*)$"));
	}
	public void unload() {
		patternsAction.clear();
		patternsMessage.clear();
		Command.removeCommands(cmd);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.getBlacklistNicks().contains(event.getUser().getNick().toLowerCase())) return;
		
		for (Pattern p : patternsMessage) {
			Matcher m = p.matcher(event.getMessage());
			if (m.find()) {
				String s = m.group(1);
				if (s.startsWith("http://") || s.startsWith("www.//") || s.startsWith("youtu.be/") || s.startsWith("youtube/")) return;
				s = Utils.mungeAllNicks(event.getChannel(),getVideoSearch(s,!Data.config.getBoolean("yt-otherbot"),true));
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+s);
				break;
			}
		}
		
		if (!Data.config.getBoolean("yt-otherbot")) {
			Matcher m = patternURL.matcher(event.getMessage());
			while (m.find()) {
				String vID = m.group(1);
				if (vID == null) vID = m.group(2);
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+getVideoInfo(vID));
			}
		}
	}
	public void onAction(ActionEvent<PircBotX> event) {
		if (Data.getBlacklistNicks().contains(event.getUser().getNick().toLowerCase())) return;
		
		for (Pattern p : patternsAction) {
			Matcher m = p.matcher(event.getAction());
			if (m.find()) {
				String s = m.group(1);
				if (s.startsWith("http://") || s.startsWith("www.//") || s.startsWith("youtu.be/") || s.startsWith("youtube/")) return;
				s = Utils.mungeAllNicks(event.getChannel(),getVideoSearch(s,!Data.config.getBoolean("yt-otherbot"),true));
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+s);
				break;
			}
		}
	}
	
	public class CmdYoutube extends Command {
		public String command() {return "youtube";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("youtube/you/yt/y");
			sb.append("\nyoutube {query} - returns the first YouTube search result");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("you") || cmd.equals("yt") || cmd.equals("y");}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length == 1) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}
			
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				if (i != 1) sb.append(" ");
				sb.append(args[i]);
			}
			
			String msg = (type == EType.Channel ? sender.getNick()+": " : "")+getVideoSearch(sb.toString(),!Data.config.getBoolean("yt-otherbot"),true);
			msg = Utils.mungeAllNicks(channel,msg,sender.getNick());
			Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,msg);
		}
	}
}
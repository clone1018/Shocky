import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.XMLObject;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleTwitter extends Module {
	private final static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy",Locale.US);
	protected Command cmd;
	
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private final Pattern statusUrl = Pattern.compile("https?://(?:www.)?(?:[a-z]+?\\.)?twitter\\.com/(#!/)?[^/]+/status(es)?/([0-9]+)");
	
	public String name() {return "twitter";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("twitter-dateformat","dd.MM.yyyy HH:mm:ss");
		Command.addCommands(this, cmd = new CmdTwitter());
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		
		for (String url : Utils.getAllUrls(event.getMessage())) {
			Matcher m = statusUrl.matcher(url);
			if (!m.find()) continue;
			//String id = url.substring(url.length()-new StringBuilder(url).reverse().indexOf("/"),url.length());
			//String id = url.substring(url.lastIndexOf('/'));
			String id = m.group(3);
			
			HTTPQuery q = null;
			XMLObject xBase = null;
			try {
				q = HTTPQuery.create("http://api.twitter.com/1/statuses/show.xml?trim_user=false&id="+id);
				q.connect(true,false);
				xBase = XMLObject.deserialize(q.readWhole());
			} catch (Exception e) {
				e.printStackTrace();
				return;
			} finally {
				q.close();
			}
			XMLObject status = xBase.getElement("status");
			XMLObject user = status.getElement("user");
			
			try {
				String author = user.getElement("name").getValue();
				author += " (@"+user.getElement("screen_name").getValue()+")";
				String tweet = StringTools.unescapeHTML(status.getElement("text").getValue());
				Date date = sdf.parse(status.getElement("created_at").getValue());
				Shocky.sendChannel(event.getBot(),event.getChannel(),Utils.mungeAllNicks(event.getChannel(),0,event.getUser().getNick()+": "+author+", "+Utils.timeAgo(date)+": "+tweet,event.getUser().getNick()));
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	
	public class CmdTwitter extends Command {
		public String command() {return "twitter";}
		public String help(Parameters params) {
			return "twitter\ntwitter {nick} [index] - returns a tweet";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount < 1 || params.tokenCount > 2) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String nick = params.tokens.nextToken();
			int index = 1;
			if (params.tokenCount == 2) {
				String indexString = params.tokens.nextToken();
				try {
					index = Integer.parseInt(indexString);
				}
				catch (NumberFormatException e) {}
			}
			
			if (index < 1 || index > 20) {
				callback.type = EType.Notice;
				callback.append("index must be 1-20");
				return;
			}
			
			HTTPQuery q = HTTPQuery.create("http://api.twitter.com/1/statuses/user_timeline.xml?"+HTTPQuery.parseArgs("trim_user","false","screen_name",nick));
			q.connect(true,false);
			XMLObject xBase = XMLObject.deserialize(q.readWhole()).getElement("statuses");
			q.close();
			
			XMLObject status = xBase.getElements("status").get(index-1);
			XMLObject user = status.getElement("user");
			
			try {
				String author = user.getElement("name").getValue();
				author += " (@"+user.getElement("screen_name").getValue()+")";
				String tweet = status.getElement("text").getValue();
				Date date = sdf.parse(status.getElement("created_at").getValue());
				callback.append(Utils.mungeAllNicks(params.channel,0,author+", "+Utils.timeAgo(date)+": "+tweet,params.sender.getNick()));
			} catch (Exception e) {e.printStackTrace();}
		}
	}
}
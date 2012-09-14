import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.HTTPQuery;
import pl.shockah.XMLObject;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleTwitter extends Module {
	private final static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy",Locale.US);
	protected Command cmd;
	
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private final Pattern statusUrl = Pattern.compile("https?://(?:www.)?(?:[a-z]+?\\.)?twitter\\.com/(#!/)?[^/]+/status(es)?/[0-9]+");
	
	public String name() {return "twitter";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("twitter-dateformat","dd.MM.yyyy HH:mm:ss");
		Command.addCommands(this, cmd = new CmdTwitter());
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		
		for (String url : Utils.getAllUrls(event.getMessage())) {
			if (!statusUrl.matcher(url).find()) continue;
			String id = url.substring(url.length()-new StringBuilder(url).reverse().indexOf("/"),url.length());
			
			HTTPQuery q = HTTPQuery.create("http://api.twitter.com/1/statuses/show.xml?trim_user=false&id="+id);
			q.connect(true,false);
			XMLObject xBase = XMLObject.deserialize(q.readWhole());
			q.close();
			XMLObject status = xBase.getElement("status").get(0);
			XMLObject user = status.getElement("user").get(0);
			
			try {
				String author = user.getElement("name").get(0).getValue();
				author += " (@"+user.getElement("screen_name").get(0).getValue()+")";
				String tweet = status.getElement("text").get(0).getValue();
				Date date = sdf.parse(status.getElement("created_at").get(0).getValue());
				Shocky.sendChannel(event.getBot(),event.getChannel(),Utils.mungeAllNicks(event.getChannel(),event.getUser().getNick()+": "+author+", "+Utils.timeAgo(date)+": "+tweet,event.getUser().getNick()));
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	
	public class CmdTwitter extends Command {
		public String command() {return "twitter";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "twitter\ntwitter {nick} [index] - returns a tweet";
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2 || args.length > 3) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String nick = args[1];
			int index = args.length == 3 ? Integer.parseInt(args[2]) : 1;
			
			if (index < 1 || index > 20) {
				callback.type = EType.Notice;
				callback.append("index must be 1-20");
				return;
			}
			
			HTTPQuery q = HTTPQuery.create("http://api.twitter.com/1/statuses/user_timeline.xml?"+HTTPQuery.parseArgs("trim_user","false","screen_name",nick));
			q.connect(true,false);
			XMLObject xBase = XMLObject.deserialize(q.readWhole()).getElement("statuses").get(0);
			q.close();
			
			XMLObject status = xBase.getElement("status").get(index-1);
			XMLObject user = status.getElement("user").get(0);
			
			try {
				String author = user.getElement("name").get(0).getValue();
				author += " (@"+user.getElement("screen_name").get(0).getValue()+")";
				String tweet = status.getElement("text").get(0).getValue();
				Date date = sdf.parse(status.getElement("created_at").get(0).getValue());
				Shocky.sendChannel(bot,channel,Utils.mungeAllNicks(channel,sender.getNick()+": "+author+", "+Utils.timeAgo(date)+": "+tweet,sender.getNick()));
			} catch (Exception e) {e.printStackTrace();}
		}
	}
}
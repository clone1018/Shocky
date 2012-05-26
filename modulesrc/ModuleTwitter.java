import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.HTTPQuery;
import pl.shockah.XMLObject;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;

public class ModuleTwitter extends Module {
	private final static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy",Locale.US);
	
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private final Pattern statusUrl = Pattern.compile("https?://twitter\\.com/(#!/)?[^/]+/status(es)?/[0-9]+");
	
	public String name() {return "twitter";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("twitter-dateformat","dd.MM.yyyy HH:mm:ss");
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		
		for (String url : Utils.getAllUrls(event.getMessage())) {
			if (!statusUrl.matcher(url).find()) continue;
			String id = url.substring(url.length()-new StringBuilder(url).reverse().indexOf("/"),url.length());
			
			HTTPQuery q = new HTTPQuery("http://api.twitter.com/1/statuses/show.xml?trim_user=false&id="+id,"GET");
			q.connect(true,false);
			XMLObject xBase = XMLObject.deserialize(q.readWhole());
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
}
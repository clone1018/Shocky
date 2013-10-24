import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import pl.shockah.HTTPQuery;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;


public class ModuleSpotify extends Module {
	
	private Pattern target = Pattern.compile("https?://(?:(?:(?:www\\.)?open\\.spotify\\.com/track/([a-zA-Z0-9]+)))");

	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		
		if (!Data.forChannel(event.getChannel()).getBoolean("spotify-otherbot")) {
			Matcher m = this.target.matcher(event.getMessage());
			while (m.find()) {
				String id = m.group(1);
				if (id == null) id = m.group(2);
				String result = this.info(id);
				if (result == null) return;
				Shocky.sendChannel(event.getBot(),event.getChannel(),event.getUser().getNick()+": "+result);
			}
		}
	}
	
	public String info(String id) {
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
			for(int i = 0; i < length; i++) {
				artists[i] = artistsArray.getJSONObject(i).getString("name");
			}
			int duration = (int)jItem.getDouble("length");
			
			StringBuilder sb = new StringBuilder();
			sb.append(Colors.BOLD + track + Colors.NORMAL);
			sb.append(" | length ").append(Colors.BOLD + Utils.timeAgo(duration) + Colors.NORMAL);
			sb.append(" | released ").append(Colors.BOLD + released + Colors.NORMAL);
			sb.append(" | by ").append(Colors.BOLD + ModuleSpotify.join(artists, ", "));
			return sb.toString();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	@Override
	public String name() {
		return "spotify";
	}
	
	public static String join(String r[],String d) {
	        if (r.length == 0) return "";
	        StringBuilder sb = new StringBuilder();
	        int i;
	        for(i=0;i<r.length-1;i++) sb.append(r[i]+d);
	        return sb.toString()+r[i];
	}

}

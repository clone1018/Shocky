import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.IAcceptURLs;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class ModuleTwitter extends Module implements IAcceptURLs {
	private final static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy",Locale.US);
	protected Command cmd;
	
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private final Pattern statusUrl = Pattern.compile("/status(es)?/([0-9]+)");
	
	public String name() {return "twitter";}
	public boolean isListener() {return false;}
	public void onEnable(File dir) {
		Data.config.setNotExists("twitter-dateformat","dd.MM.yyyy HH:mm:ss");
		Data.config.setNotExists("twitter-consumerkey","");
		Data.config.setNotExists("twitter-consumersecret","");
		Data.protectedKeys.add("twitter-consumerkey");
		Data.protectedKeys.add("twitter-consumersecret");
		Data.protectedKeys.add("twitter-accesstoken");
		
		Command.addCommands(this, cmd = new CmdTwitter());
	}
	
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String getAccessToken(boolean useCache) {
		if (useCache && Data.config.exists("twitter-accesstoken"))
			return Data.config.getString("twitter-accesstoken");
		String key = Data.config.getString("twitter-consumerkey");
		String secret = Data.config.getString("twitter-consumersecret");
		if (key.isEmpty() || secret.isEmpty())
			return null;
		HTTPQuery q;
		try {
			q = HTTPQuery.create("https://api.twitter.com/oauth2/token",HTTPQuery.Method.POST);
		} catch (MalformedURLException e1) {return null;}
		try {
			String credentials = String.format("%s:%s", URLEncoder.encode(key, "UTF-8"),URLEncoder.encode(secret, "UTF-8"));
			q.connect(true,true);
			q.setHeaderProperty("Authorization", "Basic "+Base64.encodeBase64String(StringUtils.getBytesUtf8(credentials)));
			q.setHeaderProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			q.write("grant_type=client_credentials");
			JSONObject json = new JSONObject(q.readWhole());
			if (!json.getString("token_type").contentEquals("bearer"))
				return null;
			String token = json.getString("access_token");
			Data.config.set("twitter-accesstoken", token);
			return token;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			q.close();
		}
		return null;
	}
	
	public Object getJSON(String url) {
		HTTPQuery q = null;
		String token = getAccessToken(true);
		if (token == null)
			return null;
		try {
			q = HTTPQuery.create(url);
			q.connect(true,false);
			q.setHeaderProperty("Authorization", "Bearer "+token);
			String s = q.readWhole();
			if (s.isEmpty())
				return null;
			if (s.charAt(0)=='[')
				return new JSONArray(s);
			JSONObject json = new JSONObject(s);
			if (json.has("errors")) {
				JSONArray errors = json.getJSONArray("errors");
				Data.config.remove("twitter-accesstoken");
				System.out.print("Twitter errors for ");
				System.out.println(url);
				for (int i = 0; i < errors.length(); ++i) {
					JSONObject error = errors.getJSONObject(i);
					System.out.format("Code: %d Message: %s",error.getInt("code"),error.getString("message")).println();
				}
				return null;
			}
			return json;
		} catch (Exception e) {
			e.printStackTrace();
			Data.config.remove("twitter-accesstoken");
		} finally {
			q.close();
		}
		return null;
	}
	
	@Override
	public boolean shouldAcceptURL(URL u) {
		if (u == null)
			return false;
		if (!u.getProtocol().startsWith("http"))
			return false;
		return u.getHost().endsWith("twitter.com");
	}
	@Override
	public void handleURL(PircBotX bot, Channel channel, User sender, List<URL> urls) {
		if (bot == null || urls == null || urls.isEmpty() || (channel == null && sender == null))
			return;
		if (channel != null && !isEnabled(channel.getName()))
			return;
		
		StringBuilder sb = new StringBuilder();
		Iterator<URL> iter = urls.iterator();
		while (iter.hasNext()) {
			URL u = iter.next();
			Matcher m = statusUrl.matcher(u.getPath());
			if (!m.find())
				continue;
		
			JSONObject json = (JSONObject)getJSON("https://api.twitter.com/1.1/statuses/show.json?"+HTTPQuery.parseArgs("trim_user","false","id",m.group(2)));
			if (json == null)
				continue;
		
			try {
				JSONObject user = json.getJSONObject("user");
				String author = String.format("%s (@%s)",user.getString("name"),user.getString("screen_name"));
				String tweet = StringTools.unescapeHTML(json.getString("text"));
				Date date = sdf.parse(json.getString("created_at"));
				sb.append(author).append(", ").append(Utils.timeAgo(date)).append(": ").append(tweet);
				if (iter.hasNext())
					sb.append('\n');
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (sb.length() == 0)
			return;
		String result = StringTools.formatLines(sb);
		if (channel != null)
			bot.sendMessage(channel, Utils.mungeAllNicks(channel,0,sender.getNick()+": "+result,sender));
		else if (sender != null)
			bot.sendMessage(sender, result);
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
			
			String nick = params.nextParam();
			int index = 1;
			if (params.tokenCount == 2) {
				String indexString = params.nextParam();
				try {
					index = Integer.parseInt(indexString);
				}
				catch (NumberFormatException e) {}
			}
			
			if (index < 1 || index > 200) {
				callback.type = EType.Notice;
				callback.append("index must be 1-200");
				return;
			}
			
			JSONArray array = (JSONArray)getJSON("https://api.twitter.com/1.1/statuses/user_timeline.json?"+HTTPQuery.parseArgs("trim_user","false","exclude_replies","true","screen_name",nick,"count",Integer.toString(index)));
			if (array == null||array.length()==0)
				return;
			
			if (index > array.length())
				index = array.length();

			try {
				JSONObject json = array.getJSONObject(index-1);
				JSONObject user = json.getJSONObject("user");
				String author = String.format("%s (@%s)",user.getString("name"),user.getString("screen_name"));
				String tweet = StringTools.unescapeHTML(json.getString("text"));
				tweet = Utils.mungeAllNicks(params.channel,0,tweet,params.sender);
				Date date = sdf.parse(json.getString("created_at"));
				callback.append(author).append(", ").append(Utils.timeAgo(date)).append(": ");
				callback.append(StringTools.formatLines(tweet));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleGoogle extends Module {
	protected Command cmd1;
	protected Command cmd2;

	@Override
	public String name() {return "google";}
	@Override
	public void onEnable() {
		Command.addCommands(cmd1 = new CmdGoogle());
		Command.addCommands(cmd2 = new CmdGoogleImg());
	}
	
	@Override
	public void onDisable() {
		Command.removeCommands(cmd1);
		Command.removeCommands(cmd2);
	}
	
	public void doSearch(Command cmd, PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		if (args.length == 1) {
			callback.type = EType.Notice;
			callback.append(cmd.help(bot,type,channel,sender));
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			if (i != 1) sb.append(" ");
			sb.append(args[i]);
		}
		
		HTTPQuery q;
		StringBuilder result = new StringBuilder();
		try {
			q = new HTTPQuery("http://ajax.googleapis.com/ajax/services/search/"+(cmd instanceof CmdGoogleImg?"images":"web")+"?v=1.0&safe=off&q=" + URLEncoder.encode(sb.toString(), "UTF8"), "GET");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		q.connect(true, false);
		String line = q.readWhole();
		
		try {
			JSONObject json = new JSONObject(line);
			JSONArray results = json.getJSONObject("responseData").getJSONArray("results");
			if (results.length() == 0) {
				callback.append("No results.");
				return;
			}
			JSONObject r = results.getJSONObject(0);
			String title = StringTools.ircFormatted(r.getString("titleNoFormatting"),true);
			String url = StringTools.ircFormatted(r.getString("unescapedUrl"),false);
			String content = StringTools.ircFormatted(r.getString("content"),true);
			result.append(url);
			result.append(" -- ");result.append(title);
			result.append(": ");
			if (!content.isEmpty())
				result.append(content);
			else
				result.append("No description available.");
			callback.append(result);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public class CmdGoogle extends Command {
		public String command() {return "google";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("google/g");
			sb.append("\ngoogle {query} - returns the first Google search result");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("g");}
		@Override
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			doSearch(this, bot, type, callback, channel, sender, message);
		}
	}
	
	public class CmdGoogleImg extends Command {
		public String command() {return "gis";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("gis/gi");
			sb.append("\ngis {query} - returns the first Google Image search result");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("gi");}
		@Override
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			doSearch(this, bot, type, callback, channel, sender, message);
		}
	}
}
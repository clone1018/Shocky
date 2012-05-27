import java.net.URLEncoder;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleUrban extends Module {
	protected Command cmd;

	@Override
	public String name() {return "urban";}
	@Override
	public void onEnable() {
		Command.addCommands(cmd = new CmdUrban());
	}
	
	@Override
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdUrban extends Command {
		public String command() {return "urban";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("urban/ur/u");
			sb.append("\nurban {query} - returns the first Urban dictionary search result");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("ur") || cmd.equals("u");}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length == 1) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
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
				q = new HTTPQuery("http://www.urbandictionary.com/iphone/search/define?term=" + URLEncoder.encode(sb.toString(), "UTF8"), "GET");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			q.connect(true, false);
			String line = q.readWhole();
			
			try {
				JSONObject json = new JSONObject(line);
				String resulttype = json.getString("result_type");
				if (resulttype.contentEquals("no_results")) {
					callback.append("No results.");
					return;
				}
				JSONObject entry = json.getJSONArray("list").getJSONObject(0);
				String word = entry.getString("word");
				String definition = entry.getString("definition");
				String example = entry.getString("example");
				String permalink = entry.getString("permalink");
				result.append(Utils.shortenUrl(permalink));
				result.append(" \u0002");
				result.append(word);
				result.append("\u0002: ");
				result.append(definition);
				if (example != null && example.length()>0) {
					result.append(" Example: ");
					result.append(example);
				}
				String output = StringTools.ircFormatted(result, true);
				callback.append(output);
			} catch (Exception e) {e.printStackTrace();}
		}
	}
}
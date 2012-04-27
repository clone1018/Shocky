import java.net.URLEncoder;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.HTTPQuery;
import pl.shockah.JSONObject;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;

public class ModuleUrban extends Module {
	protected Command cmd;

	@Override
	public String name() {return "urban";}
	@Override
	public void load() {
		Command.addCommands(cmd = new CmdUrban());
	}
	
	@Override
	public void unload() {
		Command.removeCommands(cmd);
	}
	
	public class CmdUrban extends Command {
		public String command() {return "urban";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("urban/u");
			sb.append("\nurban {query} - returns the first Urban dictionary search result");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("u");}
		
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
			JSONObject json = JSONObject.deserialize(line);
			String resulttype = json.getString("result_type");
			if (resulttype.contentEquals("no_results")) {
				Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,"No results.");
				return;
			}
			JSONObject entry = json.getJSONObjectArray("list")[0];
			String word = entry.getString("word");
			String definition = entry.getString("definition");
			String example = entry.getString("example");
			result.append("\u0002");
			result.append(word);
			result.append("\u0002: ");
			result.append(definition);
			if (example != null && example.length()>0) {
				result.append(" Example: ");
				result.append(example);
			}
			String output = result.toString().replaceAll("\\\\r", "").replaceAll("\\\\n", "").replaceAll("\\\\\"", "\"");
			Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,output);
		}
	}
}
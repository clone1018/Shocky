import java.net.URLEncoder;
import org.json.JSONObject;
import org.pircbotx.Colors;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleUrban extends Module {
	protected Command cmd;

	@Override
	public String name() {return "urban";}
	@Override
	public void onEnable() {
		Command.addCommands(this, cmd = new CmdUrban());
	}
	
	@Override
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdUrban extends Command {
		public String command() {return "urban";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("urban");
			sb.append("\nurban {query} - returns the first Urban dictionary search result");
			return sb.toString();
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			HTTPQuery q;
			StringBuilder result = new StringBuilder();
			try {
				q = HTTPQuery.create("http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(params.input, "UTF8"));
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			q.connect(true, false);
			String line = q.readWhole();
			q.close();
			
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
				result.append(" ");
				result.append(Colors.BOLD);
				result.append(word);
				result.append(Colors.BOLD);
				result.append(": ");
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
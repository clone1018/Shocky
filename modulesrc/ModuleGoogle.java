import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleGoogle extends Module {
	protected Command cmd1;
	protected Command cmd2;

	@Override
	public String name() {return "google";}
	@Override
	public void onEnable() {
		Command.addCommands(this, cmd1 = new CmdGoogle());
		Command.addCommands(this, cmd2 = new CmdGoogleImg());
		Command.addCommand(this, "g", cmd1);
	}
	
	@Override
	public void onDisable() {
		Command.removeCommands(cmd1);
		Command.removeCommands(cmd2);
	}
	
	public void doSearch(Command cmd, Parameters params, CommandCallback callback) {
		if (params.tokenCount == 0) {
			callback.type = EType.Notice;
			callback.append(cmd.help(params));
			return;
		}
		
		HTTPQuery q;
		try {
			q = HTTPQuery.create("http://ajax.googleapis.com/ajax/services/search/"+(cmd instanceof CmdGoogleImg?"images":"web")+"?v=1.0&safe=off&q=" + URLEncoder.encode(params.input, "UTF8"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		q.connect(true, false);
		String line = q.readWhole();
		q.close();
		
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
			callback.append(url);
			callback.append(" -- ");
			callback.append(title);
			callback.append(": ");
			if (!content.isEmpty())
				callback.append(content);
			else
				callback.append("No description available.");
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public class CmdGoogle extends Command {
		public String command() {return "google";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("google/g");
			sb.append("\ngoogle {query} - returns the first Google search result");
			return sb.toString();
		}
		@Override
		public void doCommand(Parameters params, CommandCallback callback) {
			doSearch(this, params, callback);
		}
	}
	
	public class CmdGoogleImg extends Command {
		public String command() {return "gis";}
		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("gis");
			sb.append("\ngis {query} - returns the first Google Image search result");
			return sb.toString();
		}
		@Override
		public void doCommand(Parameters params, CommandCallback callback) {
			doSearch(this, params, callback);
		}
	}
}
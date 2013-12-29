import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.pircbotx.Colors;
import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.ILua;

public class ModuleUrban extends Module implements ILua {
	protected Command cmd;

	@Override
	public String name() {return "urban";}
	@Override
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdUrban());
	}
	
	@Override
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public JSONObject getJSON(String input) throws IOException, JSONException {
		HTTPQuery q = HTTPQuery.create("http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(input, "UTF8"));
		q.connect(true, false);
		String line = q.readWhole();
		q.close();
		JSONObject json = new JSONObject(line);
		String resulttype = json.getString("result_type");
		if (resulttype.contentEquals("no_results"))
			return null;
		return json.getJSONArray("list").getJSONObject(0);
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
			
			try {
				JSONObject entry = getJSON(params.input);
				if (entry == null) {
					callback.append("No results.");
					return;
				}
				
				String word = entry.getString("word");
				String definition = entry.getString("definition");
				String example = entry.getString("example");
				String permalink = entry.getString("permalink");
				
				StringBuilder result = new StringBuilder();
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
	
	public class Function extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs arg) {
			try {
				JSONObject entry = getJSON(arg.arg1().checkjstring());
				if (entry != null) {
					LuaValue[] a = new LuaValue[] {
						valueOf(entry.getString("definition")),
						valueOf(entry.getString("example")),
						valueOf(entry.getString("permalink"))
					};
					return varargsOf(a);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return NONE;
		}
	}

	@Override
	public void setupLua(LuaTable env) {
		env.rawset("urban", new Function());
	}
}
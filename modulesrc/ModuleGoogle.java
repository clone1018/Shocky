import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import pl.shockah.HTTPQuery;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.interfaces.ILua;

public class ModuleGoogle extends Module implements ILua {
	protected Command cmd1;
	protected Command cmd2;

	@Override
	public String name() {return "google";}
	@Override
	public void onEnable(File dir) {
		Command.addCommands(this, cmd1 = new CmdGoogle(), cmd2 = new CmdGoogleImg());
		Command.addCommand(this, "g", cmd1);
	}
	
	@Override
	public void onDisable() {
		Command.removeCommands(cmd1,cmd2);
	}
	
	public JSONArray getJSON(boolean images, String search) throws IOException, JSONException {
		HTTPQuery q = HTTPQuery.create("http://ajax.googleapis.com/ajax/services/search/"+(images?"images":"web")+"?v=1.0&safe=off&q=" + URLEncoder.encode(search, "UTF8"));
		try {
			q.connect(true, false);
			JSONObject json = new JSONObject(q.readWhole());
			return json.getJSONObject("responseData").getJSONArray("results");
		} finally {
			q.close();
		}
	}
	
	public void doSearch(Command cmd, Parameters params, CommandCallback callback) {
		if (params.tokenCount == 0) {
			callback.type = EType.Notice;
			callback.append(cmd.help(params));
			return;
		}
		
		try {
			JSONArray results = getJSON(cmd instanceof CmdGoogleImg, params.input);
			if (results.length() == 0) {
				callback.append("No results.");
				return;
			}
			JSONObject r = results.getJSONObject(0);
			String title = StringTools.ircFormatted(r.getString("titleNoFormatting"),true);
			String url = StringTools.ircFormatted(r.getString("unescapedUrl"),false);
			String content = StringTools.ircFormatted(r.getString("content"),false);
			callback.append(url);
			callback.append(" -- ");
			callback.append(title);
			callback.append(": ");
			if (!content.isEmpty())
				callback.append(content);
			else
				callback.append("No description available.");
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	public class Function extends OneArgFunction {
		private final boolean images;
		public Function(boolean images) {
			this.images = images;
		}
		
		@Override
		public LuaValue call(LuaValue arg) {
			try {
				JSONArray results = getJSON(images, arg.checkjstring());
				LuaValue[] values = new LuaValue[results.length()];
				for (int i = 0;i < values.length;++i)
					values[i] = getResultTable(results.getJSONObject(i));
				return listOf(values);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return NIL;
		}
		
		private LuaValue getResultTable(JSONObject result) throws JSONException {
			LuaTable t = new LuaTable();
			t.rawset("url", result.getString("unescapedUrl"));
			t.rawset("title", result.getString("titleNoFormatting"));
			t.rawset("desc", StringTools.stripHTMLTags(result.getString("content")));
			return t;
		}
	}

	@Override
	public void setupLua(LuaTable env) {
		env.rawset("gs", new Function(false));
		env.rawset("gis", new Function(true));
	}
}
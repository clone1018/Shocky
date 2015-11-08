import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.HTTPQuery;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;

public class ModulePython extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "python";}
	public String identifier() {return "py";}
	public void onEnable(File dir) {
		Data.config.setNotExists("python-url","http://eval.appspot.com/eval");
		Command.addCommands(this, cmd = new CmdPython());
		Data.protectedKeys.add("python-url");
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		StringBuilder sb = new StringBuilder();
		buildInit(sb,getParams(bot, channel, sender, message, factoid).entrySet());
		
		sb.append(code);
		
		HTTPQuery q;
		try {
			q = HTTPQuery.create(Data.forChannel(channel).getString("python-url")+'?'+HTTPQuery.parseArgs("statement",sb.toString()));
		} catch (MalformedURLException e) {
			return "python-url is invalid";
		}
		q.connect(true,false);
		
		sb = new StringBuilder();
		ArrayList<String> result = q.readLines();
		q.close();
		if (result.size()>0 && result.get(0).contentEquals("Traceback (most recent call last):"))
			return result.get(result.size()-1);
		
		for (String line : result) {
			if (sb.length() != 0) sb.append('\n');
			sb.append(line);
		}
		
		return sb.toString();
	}
	
	private void buildInit(StringBuilder sb, Iterable<Map.Entry<String,Object>> set) {
		for (Map.Entry<String,Object> pair : set) {
			sb.append(pair.getKey()).append('=');
			appendObject(sb, pair.getValue());
			sb.append(';');
		}
	}
	
	private void appendObject(StringBuilder sb, Object obj) {
		if (obj == null) {
			sb.append("None");
		} else if (obj.getClass().isArray()) {
			Object[] a = (Object[])obj;
			sb.append("[");
			for (int i = 0; i < a.length; ++i) {
				if (i > 0)
					sb.append(',');
				appendObject(sb, a[i]);
			}
			sb.append(']');
		}  else if (obj instanceof Map) {
			Map<?,?> a = (Map<?,?>)obj;
			sb.append("{");
			int i = 0;
			for (Entry<?, ?> entry : a.entrySet()) {
				if (i++ > 0)
					sb.append(',');
				appendObject(sb, entry.getKey());
				sb.append(':');
				appendObject(sb, entry.getValue());
			}
			sb.append('}');
		} else if (obj instanceof String) {
			String s = (String)obj;
			boolean unicode = false;
			for (int i = s.length()-1; i >= 0; --i) {
				if (s.charAt(i) >= 128) {
					unicode = true;
					break;
				}
			}
			if (unicode)
				sb.append('u');
			appendEscape(sb,s);
		} else if (obj instanceof Number) {
			sb.append(obj.toString());
		}
	}
	
	public class CmdPython extends ScriptCommand {
		public String command() {return "python";}
		public String help(Parameters params) {
			return "python\npython {code} - runs Python code";
		}
	}
}
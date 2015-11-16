import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.apache.commons.lang3.SystemUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LuaState;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.pircbotx.*;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import pl.shockah.*;
import pl.shockah.shocky.*;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.AuthorizationException;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.interfaces.IFactoid;
import pl.shockah.shocky.interfaces.IFactoidData;
import pl.shockah.shocky.interfaces.IFactoidRegistry;
import pl.shockah.shocky.interfaces.ILua;
import pl.shockah.shocky.interfaces.IRollback;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.sql.CriterionNumber;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.sql.Factoid.Token;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.QueryUpdate;
import pl.shockah.shocky.sql.SQL;

public class ModuleFactoid extends Module implements IFactoid, ILua {

	protected Command cmdR, cmdF, cmdU, cmdFCMD, cmdManage, cmdFMap;
	private final Map<CmdFactoid, String> fcmds = new HashMap<CmdFactoid, String>();
	private final HashMap<String, Function> functions = new HashMap<String, Function>();
	private static final Pattern functionPattern = Pattern.compile("(?<!\\\\)\\$([a-zA-Z_][a-zA-Z0-9_]*)\\(.*?\\)");
	private static final String factoidHash = "factoid";
	private static final String factoidFuncHash = "factoidfunc";
	private static final String sqlHash = "sql";
	private static final String getFactoidHash = "getFactoid";
	private static final String getChannelFactoidHash = "getChannelFactoid";
	private static final String getFactoidForgetHash = "getFactoidForget";
	private static final String getChannelFactoidForgetHash = "getChannelFactoidForget";
	
	public Map<String,FactoidRegistry> factoidRegistry = new HashMap<String,FactoidRegistry>();
	public static final File registryDirectory = new File("data", "factoid").getAbsoluteFile();

	public String name() {
		return "factoid";
	}

	public boolean isListener() {
		return true;
	}

	public void onEnable(File dir) {
		Data.config.setNotExists("factoid-char", "?!");
		Data.config.setNotExists("factoid-charraw", "+");
		Data.config.setNotExists("factoid-charby", "-");
		Data.config.setNotExists("factoid-charchain", ">");
		Data.config.setNotExists("factoid-show", true);
		Data.config
				.setNotExists("php-url", "http://localhost/shocky/shocky.php");
		Data.config.setNotExists("python-url", "http://eval.appspot.com/eval");
		Data.protectedKeys.add("php-url");
		Data.protectedKeys.add("python-url");
		
		SQL.raw("CREATE TABLE IF NOT EXISTS "
				+ SQL.getTable("factoid")
				+ " (id INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,channel varchar(50) DEFAULT NULL,factoid text NOT NULL,author text NOT NULL,rawtext text NOT NULL,stamp int(10) unsigned NOT NULL,locked int(1) unsigned NOT NULL DEFAULT '0',forgotten int(1) unsigned NOT NULL DEFAULT '0',PRIMARY KEY (id),INDEX channel (channel)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");

		Command.addCommands(this, cmdR = new CmdRemember(), cmdF = new CmdForget(), cmdU = new CmdUnforget(), cmdFCMD = new CmdFactoidCmd(), cmdManage = new CmdManage(), cmdFMap = new CommandRegistry());

		Command.addCommand(this, "r", cmdR);
		Command.addCommand(this, "f", cmdF);
		Command.addCommand(this, "fcmd", cmdFCMD);
		Command.addCommand(this, "fmanage", cmdManage);
		Command.addCommand(this, "fmng", cmdManage);

		ArrayList<String> lines = FileLine.read(new File(dir, "factoidCmd.cfg"));
		for (int i = 0; i < lines.size(); i += 2) {
			String name = lines.get(i);
			String names[] = name.split(";");
			String factoid = lines.get(i + 1);
			CmdFactoid cmd = new CmdFactoid(names[0], factoid);
			fcmds.put(cmd, name);
			Command.addCommand(this, cmd.command(), cmd);
			for (int o = 1; o < names.length; o++) {
				Command.addCommand(this, names[o], cmd);
			}
		}
		
		registryDirectory.mkdir();
		for (File f : registryDirectory.listFiles()) {
			String name = f.getName();
			int i = name.lastIndexOf('.');
			if (!name.substring(i).contentEquals(".txt"))
				continue;
			name = name.substring(0, i);
			FactoidRegistry reg = new FactoidRegistry();
			try {
				if (reg.load(f))
					factoidRegistry.put(name, reg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Function func;

		func = new Function() {
			public String name() {
				return "ucase";
			}

			public String result(String arg) {
				return arg.toUpperCase();
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "lcase";
			}

			public String result(String arg) {
				return arg.toLowerCase();
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "reverse";
			}

			public String result(String arg) {
				return new StringBuilder(arg).reverse().toString();
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "munge";
			}

			public String result(String arg) {
				return Utils.mungeNick(arg);
			}
		};
		functions.put(func.name(), func);

		func = new FunctionMultiArg() {
			public String name() {
				return "repeat";
			}

			public String result(String[] arg) {
				if (arg.length != 2)
					return "[Wrong number of arguments to function " + name()
							+ ", expected 2, got " + arg.length + "]";
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < Integer.parseInt(arg[1]); i++)
					sb.append(arg[0]);
				return sb.toString();
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "bitflip";
			}

			public String result(String arg) throws Exception {
				byte[] array = arg.getBytes(Helper.utf8);
				for (int i = 0; i < array.length; i++)
					array[i] = (byte) (~array[i] - 0x80 & 0xFF);
				return new String(array, Helper.utf8)
						.replaceAll("[\\r\\n]", "");
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "flip";
			}

			public String result(String arg) {
				return Utils.flip(arg);
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "odd";
			}

			public String result(String arg) {
				return Utils.odd(arg);
			}
		};
		functions.put(func.name(), func);

		func = new Function() {
			public String name() {
				return "rot13";
			}

			public String result(String arg) {
				char[] out = new char[arg.length()];
				for (int i = 0; i < arg.length(); i++) {
					char c = arg.charAt(i);
					if (c >= 'a' && c <= 'm')
						c += 13;
					else if (c >= 'n' && c <= 'z')
						c -= 13;
					else if (c >= 'A' && c <= 'M')
						c += 13;
					else if (c >= 'A' && c <= 'Z')
						c -= 13;
					out[i] = c;
				}
				return new String(out);
			}
		};
		functions.put(func.name(), func);

		func = new FunctionMultiArg() {
			public String name() {
				return "if";
			}

			public String result(String[] arg) {
				if (arg.length < 2 || arg.length > 3)
					throw new RuntimeException("[Wrong number of arguments to function " + name()
							+ ", expected 2 or 3, got " + arg.length + "]");
				if (arg[0].length() > 0) {
					return arg.length == 2 ? arg[0] : arg[1];
				} else {
					return arg.length == 2 ? arg[1] : arg[2];
				}
			}
		};
		functions.put(func.name(), func);
		
		func = new FunctionMultiArg() {
			public String name() {
				return "rnd";
			}

			public String result(String[] arg) {
				return arg[new Random().nextInt(arg.length)];
			}
		};
		functions.put(func.name(), func);
		
		func = new FunctionMultiArg() {
			public String name() {
				return "sub";
			}

			public String result(String[] arg) {
				int start, end;
				if (arg.length < 2 || arg.length > 3)
					throw new RuntimeException("[Wrong number of arguments to function " + name()
							+ ", expected 2 or 3, got " + arg.length + "]");
				if (arg.length == 2)
					end = arg[0].length();
				else
					end = Integer.parseInt(arg[2]);
				start = Integer.parseInt(arg[1]);
				return arg[0].substring(start, end);
			}
		};
		functions.put(func.name(), func);
		
		func = new Function() {
			public String name() {
				return "len";
			}

			public String result(String arg) {
				return Integer.toString(arg.length());
			}
		};
		functions.put(func.name(), func);
		
		func = new Function() {
			public String name() {
				return "mid";
			}

			public String result(String arg) {
				return Integer.toString(arg.length()/2);
			}
		};
		functions.put(func.name(), func);
		
		func = new FunctionMultiArg() {
			public String name() {
				return "tr";
			}

			public String result(String[] arg) {
				if (arg.length != 3)
					throw new RuntimeException("[Wrong number of arguments to function " + name() + ", expected 3, got " + arg.length + "]");
				String search = StringTools.build_translate(arg[0]);
				String replace = StringTools.build_translate(arg[1]);
				String source = arg[2];
				return StringTools.translate(search, replace, source);
			}
		};
		
		functions.put(func.name(), func);
		
		func = new Function() {
			public String name() {
				return "url";
			}

			public String result(String arg) throws UnsupportedEncodingException {
				return URLEncoder.encode(arg, "UTF8");
			}
		};
		functions.put(func.name(), func);
	}

	public void onDisable() {
		functions.clear();
		Command.removeCommands(fcmds.keySet().toArray(new Command[0]));
		fcmds.clear();
		Command.removeCommands(cmdR, cmdF, cmdU, cmdFCMD, cmdManage, cmdFMap);
	}

	public void onDataSave(File dir) {
		ArrayList<String> lines = new ArrayList<String>();
		for (Entry<CmdFactoid, String> fcmd : fcmds.entrySet()) {
			lines.add(fcmd.getValue());
			lines.add(fcmd.getKey().factoid);
		}
		FileLine.write(new File(dir, "factoidCmd.cfg"), lines);
		
		registryDirectory.mkdir();
		for (Entry<String,FactoidRegistry> entry : factoidRegistry.entrySet()) {
			File f = new File(registryDirectory,entry.getKey()+".txt");
			entry.getValue().save(f);
		}
	}

	public void onMessage(MessageEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser()))
			return;
		onMessage(event.getBot(), event.getChannel(), event.getUser(), event.getMessage());
	}

	public void onPrivateMessage(PrivateMessageEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser()))
			return;
		onMessage(event.getBot(), null, event.getUser(), event.getMessage());
	}

	public void onNotice(NoticeEvent<ShockyBot> event) {
		if (Data.isBlacklisted(event.getUser()))
			return;
		onMessage(event.getBot(), null, event.getUser(), event.getMessage());
	}
	
	private static boolean charExists(CharSequence chars, CharSequence msg) {
		for (int i = 0; i < chars.length(); i++)
			if (msg.charAt(0) == chars.charAt(i))
				return true;
		return false;
	}

	public void onMessage(PircBotX bot, Channel channel, User sender, String msg) {
		msg = StringTools.trimWhitespace(msg).toString();
		if (msg.length() < 2)
			return;

		Config config = channel == null ? Data.config : Data.forChannel(channel);
		if (!charExists(config.getString("factoid-char"), msg))
			return;
		Command.EType msgtype = (channel == null ? Command.EType.Private : Command.EType.Channel);
		
		User target = sender;
		String targetName = null;
		String ping = null;

		msg = redirectMessage(channel, sender, msg);
		StringTokenizer strtok = new StringTokenizer(msg," ");
		String factoid = strtok.nextToken().toLowerCase();
		msg = msg.substring(factoid.length());
		int tokens = strtok.countTokens();
		int i = 0;
		StringBuilder sb = new StringBuilder();
		while (strtok.hasMoreTokens()) {
			++i;
			String token = strtok.nextToken();
			if (token.length() != 1) {
				sb.append(token).append(' ');
				continue;
			}
			char c = token.charAt(0);
			if (i == tokens && c == '<')
			{
				targetName = sender.getNick();
				break;
			} else if (i == tokens - 1)
			{
				if (c == '>')
				{
					targetName = strtok.nextToken();
					break;
				} else if (c == '|')
				{
					ping = strtok.nextToken();
					break;
				}
			}
			sb.append(c).append(' ');
		}
		String message = StringTools.trimWhitespace(sb).toString();
		/*if (args.length >= 2 && args[args.length - 2].equals(">")) {
			targetName = args[args.length - 1];
			msg = StringTools.implode(args, 0, args.length - 3, " ");
		} else if (args.length >= 1 && args[args.length - 1].equals("<")) {
			targetName = sender.getNick();
			msg = StringTools.implode(args, 0, args.length - 2, " ");
		} else if (args.length >= 2 && args[args.length - 2].equals("|")) {
			ping = args[args.length - 1];
			msg = StringTools.implode(args, 0, args.length - 3, " ");
			args = msg.split(" ");
		}*/

		if (targetName != null) {
			if (channel == null)
				return;
			boolean found = false;
			for (User user : channel.getUsers())
				if (user.getNick().equalsIgnoreCase(targetName)) {
					target = user;
					found = true;
					break;
				}
			if (!found)
				return;
			msgtype = Command.EType.Notice;
		}
		Cache cache = new Cache();
		if (charExists(config.getString("factoid-charraw"), factoid)) {
			factoid = factoid.substring(1);
			Factoid f = getFactoid(cache, channel, factoid, false);
			if (f == null)
				return;
			sb = new StringBuilder();
			if (targetName == null && ping != null)
				sb.append(ping).append(": ");
			sb.append(factoid).append(": ");
			sb.append(StringTools.formatLines(f.rawtext));
			Shocky.send(bot, msgtype, channel, target, StringTools.limitLength(sb));
			return;
		}
		if (charExists(config.getString("factoid-charby"), factoid)) {
			factoid = factoid.substring(1);
			Factoid f = getFactoid(cache, channel, factoid, false);
			if (f == null)
				return;
			sb = new StringBuilder();
			if (targetName == null && ping != null)
				sb.append(ping).append(": ");
			sb.append(factoid).append(", last edited by ").append(f.author);
			Shocky.send(bot, msgtype, channel, target, sb.toString());
			return;
		}

		String[] chain = factoid.split(config.getString("factoid-charchain"));
		for (i = 0; i < chain.length; i++) {
			Object key = chain[i];
			Factoid f = null;
			if (cache != null && cache.containsKey(factoidHash, key)) {
				Object obj = cache.get(factoidHash, key);
				if (obj instanceof Factoid)
					f = (Factoid) obj;
			}
			if (f == null)
				f = getFactoid(cache, channel, chain[i], false);
			if (f == null)
				return;
			if (cache != null && !cache.containsKey(factoidHash, key))
				cache.put(factoidHash, key, f);
		}

		try {
			for (i = 0; i < chain.length; i++)
				message = runFactoid(cache, bot, channel, sender, chain[i] + ' ' + message);
		} catch (Throwable e) {
			while (e.getCause() != null)
				e = e.getCause();
			if (e instanceof AuthorizationException && sender != null)
				msgtype = EType.Notice;
			message = e.getMessage();
		}

		if (message != null && message.length() > 0) {
			sb = new StringBuilder(StringTools.formatLines(message));
			if (msgtype == EType.Channel && targetName == null && ping != null)
				sb.insert(0, ": ").insert(0, ping);
			message = StringTools.limitLength(sb);

			Shocky.send(bot, msgtype, channel, target, message);
		}
	}

	public String runFactoid(Cache cache, PircBotX bot, Channel channel, User sender, String message) throws Exception {
		message = StringTools.trimWhitespace(message).toString();
		Set<String> checkRecursive = new HashSet<String>();
		while (true) {
			int i = message.indexOf(' ');
			String factoid;
			String args;
			if (i > 0) {
				factoid = message.substring(0, i);
				args = message.substring(i+1);
			} else {
				factoid = message;
				args = "";
			}
			Object key = factoid;

			Factoid f = null;
			if (cache != null && cache.containsKey(factoidHash, key)) {
				Object obj = cache.get(factoidHash, key);
				if (obj instanceof Factoid)
					f = (Factoid) obj;
			}
			if (f == null)
				f = getFactoid(cache, channel, factoid, false);
			if (f == null)
				break;
			if (cache != null && !cache.containsKey(factoidHash, key))
				cache.put(factoidHash, key, f);
			String raw = f.rawtext;
			if (raw.startsWith("<alias>")) {
				raw = raw.substring(7);
				message = processTokens(bot, channel, sender, args, tokenize(f, raw));
				if (checkRecursive.contains(message))
					break;
				checkRecursive.add(message);
				continue;
			} else {
				return parse(cache, bot, channel, sender, args, f, raw);
			}
		}
		return null;
	}

	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, String message, Factoid f, String raw) throws Exception {
		if (raw == null || raw.length() == 0 || raw.startsWith("<noreply>"))
			return "";
		String type = null;
		int closingIndex = -1;
		if (raw.charAt(0) == '<') {
			closingIndex = raw.indexOf('>');
			if (closingIndex != -1)
				type = raw.substring(1, closingIndex);
		}
		
		if (type != null) {
			int commaIndex = type.indexOf(',');
			if (commaIndex >= 0) {
				String args = type.substring(commaIndex + 1);
				type = type.substring(0, commaIndex);
				int equalIndex = args.indexOf('=');
				if (equalIndex >= 0) {
					String key = args.substring(0, equalIndex);
					String value = args.substring(equalIndex + 1);

					if (key.contentEquals("map")
							&& factoidRegistry.containsKey(value))
						f.registry = factoidRegistry.get(value);
				}
			}
		}
		
		ScriptModule sModule = null;
		if (type != null)
			sModule = Module.getScriptingModule(type);
		if (sModule != null) {
			if (channel != null && !sModule.isEnabled(channel.getName()))
				return "";
			raw = raw.substring(closingIndex + 1);
			raw = processTokens(bot, channel, sender, message, tokenize(f, raw));
			String parsed = sModule.parse(cache, bot, channel, sender, f, raw, message);
			return parse(cache, bot, channel, sender, message, null, parsed);
		} else if (type != null && type.contentEquals("cmd")) {
			CommandCallback callback = new CommandCallback();
			raw = raw.substring(closingIndex + 1);
			String[] args = raw.split("\\s+", 2);
			Command cmd = Command.getCommand(bot, sender, channel, EType.Channel, callback, args[0]);
			if (cmd != null && !(cmd instanceof CmdFactoid)) {
				EType etype = (channel == null) ? EType.Notice : EType.Channel;
				raw = (args.length == 1) ? "" : processTokens(bot, channel, sender, message, tokenize(f, args[1]));
				Parameters params = new Parameters(bot, etype, channel, sender, raw);
				cmd.doCommand(params, callback);
				if (callback.type == EType.Channel)
					return callback.toString();
			}
			return "";
		} else {
			return processTokens(bot, channel, sender, message, tokenize(f, raw));
		}
	}

	private static final Pattern argPattern = Pattern.compile("%([A-Za-z]+)([0-9]+)?(-)?([0-9]+)?%");

	public static String processTokens(PircBotX bot, Channel channel, User sender, String message, Token[] tokens) throws Exception {
		StringTokenizer strtok = new StringTokenizer(message," ");
		String[] args = new String[strtok.countTokens()];
		int i = 0;
		while (strtok.hasMoreTokens())
			args[i++] = strtok.nextToken();
		StringBuilder sb = new StringBuilder();
		for (i = 0; i < tokens.length; ++i) {
			CharSequence seq = tokens[i].process(bot, channel, sender, message, args);
			if (seq != null)
				sb.append(seq);
		}
		if (sb.indexOf("<action>")==0)
			sb.delete(0, 8).insert(0, "\001ACTION ").append('\001');
		return sb.toString();
	}

	public static String redirectMessage(Channel channel, User sender, String message) {
		StringTokenizer strtok = new StringTokenizer(message);
		int tokens = strtok.countTokens();
		if (tokens >= 2 && tokens <= 3 && channel != null) {
			String factoid = strtok.nextToken();
			String arrow = strtok.nextToken();
			if (charExists("^", arrow)) {
				IRollback module = (IRollback) Module.getModule("rollback");
				try {
					if (module != null) {
						User user = null;
						if (strtok.hasMoreTokens()) {
							String name = strtok.nextToken();
							for (User target : channel.getUsers()) {
								if (target.getNick().equalsIgnoreCase(name)) {
									user = target;
									break;
								}
							}
						}
						List<LineMessage> lines = module.getRollbackLines(LineMessage.class, channel.getName(), user != null ? user.getNick() : null, null, message, true, 1, 0);
						if (lines.size() == 1) {
							StringBuilder msg = new StringBuilder(factoid);
							msg.append(' ');
							msg.append(lines.get(0).text);
							message = msg.toString();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return message.substring(1);
	}
	
	public static class TextToken implements Token {
		public final CharSequence string;

		public TextToken(CharSequence string) {
			this.string = string;
		}

		@Override
		public CharSequence process(PircBotX bot, Channel channel, User sender, String message, String[] args) throws Exception {
			return this.string;
		}

		@Override
		public String toString() {
			return this.string.toString();
		}
	}
	
	public static class ParameterToken implements Token {
		public final String raw;
		public final String tag;
		public final int start;
		public final int end;
		public final boolean range;

		public ParameterToken(Matcher m) {
			this.raw = m.group();
			this.tag = m.group(1);
			String num1str = m.group(2);
			String num2str = m.group(4);

			this.start = (num1str != null) ? (Integer.parseInt(num1str)) : Integer.MIN_VALUE;
			this.end = (num2str != null) ? (Integer.parseInt(num2str)) : Integer.MAX_VALUE;
			this.range = m.group(3) != null;
		}

		@Override
		public CharSequence process(PircBotX bot, Channel channel, User sender, String message, String[] args) throws Exception {
			if (tag.contentEquals("arg")) {
				int req = 0;
				if (range) {
					int min = start != Integer.MIN_VALUE ? start : 0;
					int max = end != Integer.MAX_VALUE ? end : args.length - 1;
					req = Math.max(req, Math.max(min, max));
					if (args.length > req)
						return StringTools.implode(args, min, max, " ");
				} else if (start != Integer.MIN_VALUE) {
					req = Math.max(req, start);
					if (args.length > req)
						return args[start];
				}
				throw new RuntimeException("Not enough args.");
			} else if (tag.contentEquals("inp"))
				return StringTools.implode(args, 0, " ");
			else if (tag.contentEquals("ioru"))
				return (message == null || message.isEmpty()) ? sender.getNick() : message;
			else if (tag.contentEquals("bot"))
				return bot.getName();
			else if (channel != null && tag.contentEquals("chan"))
				return channel.getName();
			else if (tag.contentEquals("user"))
				return sender.getNick();
			else if (channel != null && tag.contentEquals("rndn")) {
				User[] users = channel.getUsers().toArray(new User[0]);
				return users[new Random().nextInt(users.length)].getNick();
			} else if (tag.contentEquals("host"))
				return sender.getHostmask();
			else if (tag.contentEquals("acc"))
				return Whois.getWhoisLogin(sender);
			return raw;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(tag);
			if (start != Integer.MIN_VALUE)
				sb.append(start);
			if (range)
				sb.append('-');
			if (end != Integer.MAX_VALUE)
				sb.append(end);
			return sb.toString();
		}
	}
	
	public static class FunctionToken implements Token {
		public final Function func;
		public final Token[][] params;

		public FunctionToken(Function func, Token[][] params) {
			this.func = func;
			this.params = params;
		}

		@Override
		public CharSequence process(PircBotX bot, Channel channel, User sender, String message, String[] args) throws Exception {
			if (func instanceof FunctionMultiArg) {
				FunctionMultiArg multiFunc = (FunctionMultiArg)func;
				String[] prepared = new String[params.length];
				for (int i = 0; i < prepared.length; ++i)
					prepared[i] = processTokens(bot, channel, sender, message, params[i]);
				return multiFunc.result(prepared);
			} else {
				return func.result(processTokens(bot, channel, sender, message, params[0]));
			}
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(func.name()).append('(');
			for (int i = 0; i < params.length; ++i) {
				if (i > 0)
					sb.append(',');
				sb.append(params.toString());
			}
			sb.append(')');
			return sb.toString();
		}
	}

	public Token[] tokenize(Factoid f, CharSequence input) {
		if (f != null && f.tokens != null)
			return f.tokens;
		List<Token> tokens = new LinkedList<Token>();
		Matcher m = functionPattern.matcher(input);
		int pos = 0;
		while (m.find(pos)) {
			if (pos < m.start())
				tokens.add(new TextToken(input.subSequence(pos, m.start())));
			String fName = m.group(1);
			Function func = null;
			if (functions.containsKey(fName))
				func = functions.get(fName);
			if (func != null) {
				int start = m.end(1) + 1;
				int end = Integer.MIN_VALUE;
				int expected = 1;
				for (int i = start; i < input.length(); i++) {
					char c = input.charAt(i);
					if (c == '\\')
					{
						++i;
						continue;
					}
					if (c == '(')
						expected++;
					else if (c == ')')
						expected--;
					if (expected == 0) {
						end = i;
						pos = end + 1;
						break;
					}
				}
				if (end == Integer.MIN_VALUE)
					throw new RuntimeException("Unclosed function.");
				Token[] params = tokenize(null, input.subSequence(start, end));
				if (func instanceof FunctionMultiArg)
					tokens.add(new FunctionToken(func,splitArgs(params)));
				else
					tokens.add(new FunctionToken(func,new Token[][] {params}));
			} else {
				tokens.add(new TextToken(m.group()));
				pos = m.end();
			}
		}
		if (pos < input.length())
			tokens.add(new TextToken(input.subSequence(pos, input.length())));
		ListIterator<Token> iter = tokens.listIterator();
		while (iter.hasNext()) {
			Token token = iter.next();
			if (token instanceof TextToken) {
				TextToken text = (TextToken)token;
				m = argPattern.matcher(text.string);
				int start = 0;
				LinkedList<Token> add = new LinkedList<Token>();
				while (m.find()) {
					if (m.start() > start)
						add.add(new TextToken(text.string.subSequence(start, m.start())));
					add.add(new ParameterToken(m));
					start = m.end();
				}
				if (add.isEmpty())
					continue;
				if (start < text.string.length())
					add.add(new TextToken(text.string.subSequence(start, text.string.length())));
				iter.remove();
				Iterator<Token> addIter = add.iterator();
				while(addIter.hasNext())
					iter.add(addIter.next());
			}
		}
		Token[] array = tokens.toArray(new Token[0]);
		if (f != null && f.tokens == null)
			f.tokens = array;
		return array;
	}
	
	private static PreparedStatement prepareStatement(Cache cache, boolean hasChannel, boolean hasForget) throws SQLException {
		String key;
		if (hasForget) {
			if (hasChannel)
				key = getChannelFactoidHash;
			else
				key = getFactoidHash;
		} else {
			if (hasChannel)
				key = getChannelFactoidForgetHash;
			else
				key = getFactoidForgetHash;
		}
		PreparedStatement p = null;
		if (cache != null && cache.containsKey(sqlHash, key)) {
			Object obj = cache.get(sqlHash, key);
			if ((obj instanceof PreparedStatement)&& !((PreparedStatement) obj).isClosed())
				p = (PreparedStatement) obj;
		}

		if (p == null) {
			if (hasForget) {
				if (hasChannel)
					p = SQL.getSQLConnection().prepareStatement("SELECT * FROM factoid WHERE ((channel IS NULL OR channel=?) AND factoid=? AND forgotten=?) ORDER BY channel DESC, stamp DESC LIMIT ?");
				else
					p = SQL.getSQLConnection().prepareStatement("SELECT * FROM factoid WHERE (channel IS NULL AND factoid=? AND forgotten=?) ORDER BY stamp DESC LIMIT ?");
			} else {
				if (hasChannel)
					p = SQL.getSQLConnection().prepareStatement("SELECT * FROM factoid WHERE ((channel IS NULL OR channel=?) AND factoid=?) ORDER BY channel DESC, stamp DESC LIMIT ?");
				else
					p = SQL.getSQLConnection().prepareStatement("SELECT * FROM factoid WHERE (channel IS NULL AND factoid=?) ORDER BY stamp DESC LIMIT ?");
			}
			if (cache != null && !cache.containsKey(sqlHash, key))
				cache.put(sqlHash, key, p);
		}
		return p;
	}

	public Factoid getFactoid(Cache cache, Channel channel, String factoid) {
		Factoid f = null;
		ResultSet j;
		boolean hasChannel = channel != null;
		try {
			PreparedStatement p = prepareStatement(cache, hasChannel, false);
			synchronized (p) {
				int i = 1;
				if (hasChannel)
					p.setString(i++, channel.getName().toLowerCase());
				p.setString(i++, factoid.toLowerCase());
				p.setInt(i++, 1);
				j = p.executeQuery();
				p.clearParameters();
			}
			if (j == null || j.isClosed())
				return null;

			f = Factoid.fromResultSet(j);
			if (cache == null)
				p.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return f;
	}

	public Factoid getFactoid(Cache cache, Channel channel, String factoid, boolean forgotten) {
		Factoid f = null;
		ResultSet j;
		boolean hasChannel = (channel != null);
		try {
			PreparedStatement p = prepareStatement(cache, hasChannel, true);
			synchronized (p) {
				int i = 1;
				if (hasChannel)
					p.setString(i++, channel.getName().toLowerCase());
				p.setString(i++, factoid.toLowerCase());
				p.setInt(i++, forgotten ? 1 : 0);
				p.setInt(i++, 1);
				j = p.executeQuery();
				p.clearParameters();
			}
			if (j == null || j.isClosed())
				return null;

			f = Factoid.fromResultSet(j);
			if (cache == null)
				p.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return f;
	}

	public Factoid[] getFactoids(Cache cache, int max, Channel channel, String factoid) {
		Factoid[] f = null;
		ResultSet j;
		boolean hasChannel = (channel != null);
		try {
			PreparedStatement p = prepareStatement(cache, hasChannel, false);
			synchronized (p) {
				int i = 1;
				if (hasChannel)
					p.setString(i++, channel.getName().toLowerCase());
				p.setString(i++, factoid.toLowerCase());
				p.setInt(i++, Math.max(Math.min(max, 50), 1));
				j = p.executeQuery();
				p.clearParameters();
			}
			if (j == null || j.isClosed())
				return null;
			
			f = Factoid.arrayFromResultSet(j);
			if (cache == null)
				p.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return f;
	}

	public Factoid[] getFactoids(Cache cache, int max, Channel channel, String factoid, boolean forgotten) {
		Factoid[] f = null;
		ResultSet j;
		boolean hasChannel = (channel != null);
		try {
			PreparedStatement p = prepareStatement(cache, hasChannel, true);
			synchronized (p) {
				int i = 1;
				if (hasChannel)
					p.setString(i++, channel.getName().toLowerCase());
				p.setString(i++, factoid.toLowerCase());
				p.setInt(i++, forgotten ? 1 : 0);
				p.setInt(i++, Math.max(Math.min(max, 50), 1));
				j = p.executeQuery();
				p.clearParameters();
			}
			if (j == null || j.isClosed())
				return null;
			
			f = Factoid.arrayFromResultSet(j);
			if (cache == null)
				p.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return f;
	}
	
	public static boolean setForgotten(Factoid f, boolean forget) {
		if (f.locked)
			return false;
		QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
		q.addCriterions(new CriterionNumber("id", CriterionNumber.Operation.Equals, f.id));
		q.set("forgotten", forget ? 1 : 0);
		SQL.update(q);
		return true;
	}

	public static abstract class Function {
		public abstract String name();

		public abstract String result(String arg) throws Exception;

		@Override
		public String toString() {
			return name();
		}
	}

	public static abstract class FunctionMultiArg extends Function {
		@Override
		public final String result(String arg) throws Exception {
			return null;
		}

		public abstract String result(String[] arg) throws Exception;
	}
	
	public static Token[][] splitArgs(Token[] params) {
		
		ArrayList<Token[]> collection = new ArrayList<Token[]>();
		ArrayList<Token> current = new ArrayList<Token>();
		for (Token token : params) {
			if (!(token instanceof TextToken)) {
				current.add(token);
				continue;
			}
			TextToken text = (TextToken)token;
			StringBuilder sb = new StringBuilder(text.string);
			int o = 0;
			for (int i = 0; i < sb.length();++i) {
				char c = sb.charAt(i);
				if (c == '\\') {
					sb.deleteCharAt(i);
				} else if (c == ',') {
					if (i > o)
						current.add(new TextToken(sb.substring(o, i)));
					collection.add(current.toArray(new Token[0]));
					current.clear();
					o = i+1;
				}
			}
			if (o < sb.length())
				current.add(new TextToken(sb.substring(o, sb.length())));
		}
		collection.add(current.toArray(new Token[0]));

		return collection.toArray(new Token[0][]);
	}

	public class CmdRemember extends Command {
		public String command() {
			return "remember";
		}

		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("remember/r");
			sb.append("\nremember [.] {name} {raw} - remembers a factoid (use \".\" for local factoids)");
			return sb.toString();
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			if (params.tokenCount < 2) {
				callback.append(help(params));
				return;
			}

			String name = params.nextParam();
			boolean local = false;
			if (name.equals(".")) {
				if (params.tokenCount < 3)
					return;
				local = true;
				name = params.nextParam();
			}

			String rem = params.getParams(0);
			if (rem.isEmpty())
				return;
			Factoid f = getFactoid(null, params.channel, name, false);
			if (f != null && f.locked && f.channel != null && f.channel.equals(params.channel.getName()))
			{
				callback.append("Factoid is locked");
				return;
			}
			if (!local) {
				f = getFactoid(null, null, name, false);
				if (f != null && f.locked) {
					callback.append("Factoid is locked");
					return;
				}
			}
			QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
			if (local)
				q.add("channel", params.channel.getName());
			q.add("factoid", name);
			q.add("author", params.sender.getNick());
			q.add("rawtext", rem);
			q.add("stamp", System.currentTimeMillis() / 1000);
			SQL.insert(q);
			callback.append("Done.");
		}
	}

	public class CmdForget extends Command {
		public String command() {
			return "forget";
		}

		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("forget/f");
			sb.append("\nforget [.] {name} - forgets a factoid (use \".\" for local factoids)");
			return sb.toString();
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			if (params.tokenCount < 1) {
				callback.append(help(params));
				return;
			}

			String name = params.nextParam();
			Channel channel = null;
			if (name.equals(".")) {
				if (params.tokenCount < 2)
					return;
				channel = params.channel;
				name = params.nextParam();
			}

			Factoid f = getFactoid(null, channel, name, false);
			if (f == null || (channel != null && f.channel == null))
				callback.append("No such factoid");
			else {
				if (f.locked)
					callback.append("Factoid is locked");
				else {
					setForgotten(f, true);
					callback.append("Done. Forgot ID ").append(f.id).append(": ").append(f.rawtext);
				}
			}
		}
	}

	public class CmdUnforget extends Command {
		public String command() {
			return "unforget";
		}

		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("unforget/f");
			sb.append("\nunforget [.] {name} - unforgets a factoid (use \".\" for local factoids)");
			return sb.toString();
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			if (params.tokenCount < 1) {
				callback.append(help(params));
				return;
			}

			String name = params.nextParam();
			Channel channel = null;
			if (name.equals(".")) {
				if (params.tokenCount < 2)
					return;
				channel = params.channel;
				name = params.nextParam();
			}

			Factoid f = getFactoid(null, channel, name, true);
			if (f == null || (channel != null && f.channel == null))
				callback.append("No such factoid");
			else {
				if (f.locked)
					callback.append("Factoid is locked");
				else {
					setForgotten(f, false);
					callback.append("Done. Unforgot ID ").append(f.id).append(": ").append(f.rawtext);
				}
			}
		}
	}

	public class CmdFactoidCmd extends Command {
		public String command() {
			return "factoidcmd";
		}

		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("factoidcmd/fcmd");
			sb.append("\nfactoidcmd - lists commands being aliases for factoids");
			sb.append("\nfactoidcmd add {command};{alias1};{alias2};(...) {factoid} - makes a new command being an alias");
			sb.append("\nfactoidcmd remove {command} - removes a command being an alias");
			return sb.toString();
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			params.checkController();
			callback.type = EType.Notice;

			if (params.tokenCount == 0) {
				StringBuilder sb = new StringBuilder();
				for (Entry<CmdFactoid, String> cmd : fcmds.entrySet()) {
					if (sb.length() != 0)
						sb.append(", ");
					sb.append(cmd.getValue() + "->" + cmd.getKey().factoid);
				}
				callback.append(sb);
				return;
			} else if (params.tokenCount >= 2) {
				String method = params.nextParam();
				String name = params.nextParam();
				if (method.equalsIgnoreCase("remove")) {
					for (Entry<CmdFactoid, String> c : fcmds.entrySet()) {
						for (String s : c.getValue().split(";"))
							if (s.equals(name)) {
								Command.removeCommands(c.getKey());
								fcmds.remove(c.getKey());
								callback.append("Removed.");
								return;
							}
					}
					return;
				} else if (params.tokenCount == 3
						&& method.equalsIgnoreCase("add")) {
					String factoid = params.nextParam();
					for (Iterator<Entry<CmdFactoid, String>> iter = fcmds
							.entrySet().iterator(); iter.hasNext();) {
						Entry<CmdFactoid, String> c = iter.next();
						for (String s : c.getValue().split(";"))
							if (s.equals(name)) {
								Command.removeCommands(c.getKey());
								iter.remove();
								break;
							}
					}
					String names[] = name.split(";");
					CmdFactoid cmd = new CmdFactoid(names[0], factoid.toLowerCase());
					fcmds.put(cmd, name);
					Command.addCommand(this, cmd.command(), cmd);
					for (int i = 1; i < names.length; i++) {
						Command.addCommand(this, names[i], cmd);
					}
					callback.append("Added.");
					return;
				}
			}

			callback.append(help(params));
		}
	}

	public class CmdFactoid extends Command {
		public final String cmd;
		public final String factoid;

		public CmdFactoid(String command, String factoid) {
			this.cmd = command;
			this.factoid = factoid;
		}

		public String command() {
			return cmd;
		}

		public String help(Parameters params) {
			return "";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel)
				return;
			StringBuilder sb = new StringBuilder();
			sb.append(Data.forChannel(params.channel).getString("factoid-char")
					.charAt(0));
			sb.append(factoid);
			if (!params.input.isEmpty()) {
				sb.append(' ');
				sb.append(params.input);
			}
			onMessage(params.bot, params.channel, params.sender, sb.toString());
		}
	}

	public class CmdManage extends Command {
		public String command() {
			return "factoidmanage";
		}

		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("factoidmanage/fmanage/fmng");
			sb.append("\n[r:op/controller] factoidmanage lock [.] {factoid} - locks a factoid");
			sb.append("\n[r:op/controller] factoidmanage unlock [.] {factoid} - unlocks a factoid");
			return sb.toString();
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			params.checkAny();
			callback.type = EType.Notice;

			if (params.tokenCount >= 2) {
				String method = params.nextParam();
				String factoid = params.nextParam();
				boolean local = false;
				if (params.tokenCount >= 3 && factoid.equals(".")) {
					local = true;
					factoid = params.nextParam();
				}
				factoid = factoid.toLowerCase();
				boolean lock = method.equalsIgnoreCase("lock");
				boolean unlock = method.equalsIgnoreCase("unlock");
				if (lock || unlock) {
					if (local)
						params.checkOp();
					else
						params.checkController();
					Factoid f = getFactoid(null, local ? params.channel : null, factoid, false);
					if (f == null) {
						callback.append("No such factoid");
						return;
					}
					if (lock && f.locked) {
						callback.append("Already locked");
						return;
					} else if (unlock && !f.locked) {
						callback.append("Already unlocked");
						return;
					}

					QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
					q.set("locked", lock ? 1 : 0);
					q.addCriterions(new CriterionNumber("id", CriterionNumber.Operation.Equals, f.id));
					SQL.update(q);
					callback.append("Done");
					return;
				}
			}

			callback.append(help(params));
		}
	}
	
	public static class FactoidData extends LuaValue {
		@Override
		public int type() {
			return LuaValue.TUSERDATA;
		}

		@Override
		public String typename() {
			return "userdata";
		}

		@Override
		public LuaValue get(LuaValue key) {
			/*LuaFunction parent = LuaThread.getCallstackFunction(LuaThread.getCallstackDepth());
			LuaValue env = parent.getfenv();
			if (env == null)
				return NIL;
			LuaValue obj = env.get("state");
			if (!(obj instanceof LuaState))
				return NIL;
			LuaState state = (LuaState) obj;*/
			LuaState state = LuaState.getState();
			if (state == null)
				return NIL;
			IFactoid module = state.getFactoidModule();
			if (module == null)
				return NIL;
			return FactoidFunction.create(state, key.checkjstring());
		}
	}

	private static class FactoidFunction extends VarArgFunction {
		public final String factoid;
		private LuaValue factoidHistory = null;

		private FactoidFunction(String factoid) {
			this.factoid = factoid;
		}

		public synchronized static LuaValue create(LuaState state, String factoid) {
			if (factoid == null || factoid.isEmpty())
				return NIL;

			Object obj = state.get(factoidFuncHash, factoid);
			if (obj instanceof FactoidFunction)
				return (FactoidFunction) obj;

			FactoidFunction function = new FactoidFunction(factoid);
			state.put(factoidFuncHash, factoid, function);
			return function;
		}

		private LuaState getState() {
			/*if (env == null)
				return null;
			LuaValue obj = env.get("state");
			if (!(obj instanceof LuaState))
				return null;
			LuaState state = (LuaState) obj;*/
			LuaState state = LuaState.getState();
			if (state == null)
				return null;
			IFactoid module = state.getFactoidModule();
			if (module == null)
				return null;
			return state;
		}

		private Factoid getFactoid(LuaState state) {
			IFactoid module = state.getFactoidModule();
			if (module == null)
				return null;
			Object obj = state.get(factoidHash, factoid);
			Factoid f = null;
			if (obj instanceof Factoid)
				f = (Factoid) obj;
			if (f == null && state.chan != null)
				f = module.getFactoid(state.cache, state.chan, factoid, false);
			if (f == null)
				f = module.getFactoid(state.cache, null, factoid, false);
			state.put(factoidHash, factoid, f);
			return f;
		}

		public ScriptModule getScriptModule(Factoid f) {
			String raw = f.rawtext;
			String type = null;
			if (!raw.isEmpty() && raw.charAt(0) == '<') {
				int closingIndex = raw.indexOf('>');
				if (closingIndex != -1)
					type = raw.substring(1, closingIndex);
			}
			if (type != null) {
				int commaIndex = type.indexOf(',');
				if (commaIndex >= 0)
					type = type.substring(0, commaIndex);
				return Module.getScriptingModule(type);
			}
			return null;
		}

		@Override
		public Varargs invoke(Varargs args) {
			LuaState state = getState();
			if (state == null)
				return NIL;
			IFactoid module = state.getFactoidModule();
			if (module == null)
				return NIL;
			StringBuilder message = new StringBuilder(factoid);
			for (int i = 1; i <= args.narg(); ++i) {
				String arg = args.optjstring(i, null);
				if (arg != null && !arg.isEmpty())
					message.append(' ').append(arg);
			}
			try {
				String s = module.runFactoid(state.cache, state.bot, state.chan, state.user, message.toString());
				return s != null ? valueOf(s) : NIL;
			} catch (Exception e) {
				throw new LuaError(e);
			}
		}

		@Override
		public LuaValue get(LuaValue key) {
			String name = key.checkjstring();
			if (name == null)
				return NIL;
			int op = 0;
			if (name.equals("src"))
				op = 1;
			else if (name.equals("author"))
				op = 2;
			else if (name.equals("time"))
				op = 3;
			else if (name.equals("data"))
				op = 4;
			else if (name.equals("history"))
				op = 5;
			if (op > 0) {
				LuaState state = getState();
				if (state == null)
					return NIL;
				Factoid f = getFactoid(state);
				if (f == null)
					return NIL;
				switch (op) {
				case 1: return valueOf(f.rawtext);
				case 2: return valueOf(f.author);
				case 3: return valueOf(f.stamp);
				case 4: 
					ScriptModule sModule = getScriptModule(f);
					if (sModule != null && sModule instanceof IFactoidData) {
						IFactoidData dModule = (IFactoidData) sModule;
						String s = dModule.getData(f, false);
						if (s != null)
							return valueOf(s);
					}
					return NIL;
				case 5:
					if (factoidHistory == null)
						factoidHistory = FactoidHistory
								.getHistory(state, factoid);
					return factoidHistory;
				}
			}
			return super.get(key);
		}

		@Override
		public void set(LuaValue key, LuaValue value) {
			String name = key.checkjstring();
			if (name == null)
				return;
			if (name.equals("data")) {
				String data = value.checkjstring();
				if (data == null)
					return;
				LuaState state = getState();
				if (state == null || !state.isController())
					return;
				Factoid f = getFactoid(state);
				if (f == null)
					return;
				ScriptModule sModule = getScriptModule(f);
				if (sModule != null && sModule instanceof IFactoidData)
					((IFactoidData) sModule).setData(f, data);
			}
		}
	}
	
	public static class ForgetFunction extends ZeroArgFunction {
		public final Factoid factoid;
		public final boolean forget;

		public ForgetFunction(Factoid factoid, boolean forget) {
			this.factoid = factoid;
			this.forget = forget;
		}

		@Override
		public LuaValue call() {
			return valueOf(setForgotten(factoid,forget));
		}
		
	}

	public static class FactoidHistory {
		public static LuaValue getHistory(LuaState state, String factoid) {
			IFactoid module = state.getFactoidModule();
			if (module == null)
				return LuaValue.NIL;
			Factoid[] factoids = module.getFactoids(state.cache, 50, state.chan, factoid);
			if (factoids == null || factoids.length == 0)
				return LuaValue.NIL;
			return LuaTable.listOf(getTable(factoids));
		}

		private static LuaValue[] getTable(Factoid[] factoids) {
			LuaValue[] a = new LuaValue[factoids.length];
			for (int i = 0; i < a.length; ++i)
				a[i] = getFactoid(factoids[i]);
			return a;
		}

		private static LuaValue getFactoid(Factoid f) {
			LuaTable t = new LuaTable();
			t.rawset("id", f.id);
			if (f.name != null)
				t.rawset("name", f.name);
			if (f.channel != null)
				t.rawset("channel", f.channel);
			if (f.author != null)
				t.rawset("author", f.author);
			if (f.rawtext != null)
				t.rawset("rawtext", f.rawtext);
			t.rawset("stamp", f.stamp);
			t.rawset("locked", f.locked ? LuaValue.TRUE : LuaValue.FALSE);
			t.rawset("forgotten", f.forgotten ? LuaValue.TRUE : LuaValue.FALSE);
			t.rawset("forget", new ForgetFunction(f,true));
			t.rawset("unforget", new ForgetFunction(f,false));
			return t;
		}
	}

	@Override
	public void setupLua(LuaTable env) {
		try {
			Class.forName("ModuleFactoid$ForgetFunction");
			Class.forName("ModuleFactoid$FactoidFunction");
			Class.forName("ModuleFactoid$FactoidHistory");
			
			Class.forName("ModuleFactoid$TextToken");
			Class.forName("ModuleFactoid$ParameterToken");
			Class.forName("ModuleFactoid$FunctionToken");
		} catch (Exception e) {
			e.printStackTrace();
		}
		env.set("factoid", new FactoidData());
	}
	
	public static class FactoidRegistry implements IFactoidRegistry, Comparator<String> {
		private Map<String, String> data = null;
		
		public FactoidRegistry() {
		}
		
		@Override
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
		
		public void put(String key, String value) {
			if (data == null)
				data = new TreeMap<String, String>(this);
			data.put(key, value);
		}
		
		public void remove(String key) {
			if (data != null)
				data.remove(key);
		}
		
		public int size() {
			if (data == null)
				return 0;
			return data.size();
		}
		
		public boolean save(File file) {
			File temp = null;
			try {
				try {
					temp = File.createTempFile("shocky", ".tmp");
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
				Pattern pattern = Pattern.compile("[\\\t\\\n\\\\]");
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(temp),Helper.utf8);
				if (data != null) {
					for (Entry<String, String> entry : data.entrySet()) {
						Matcher matcher = pattern.matcher(entry.getKey());
						StringBuffer sb = new StringBuffer();
						while (matcher.find())
							matcher.appendReplacement(sb, "\\\\$0");
						matcher.appendTail(sb);
						writer.append(sb).append("\t");
						matcher = pattern.matcher(entry.getValue());
						sb = new StringBuffer();
						while (matcher.find())
							matcher.appendReplacement(sb, "\\\\$0");
						matcher.appendTail(sb);
						writer.append(sb).append(SystemUtils.LINE_SEPARATOR);
					}
				}
				writer.close();
				
				if (file.exists())
					file.delete();
				temp.renameTo(file);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				if (temp != null && temp.exists())
					temp.delete();
			}
		}
		
		public boolean load(File file) throws IOException {
			InputStreamReader reader = null;
			try {
				reader = new InputStreamReader(new FileInputStream(file),Helper.utf8);
				Map<String, String> tmp = new TreeMap<String, String>(this);
				StringBuilder sb = new StringBuilder();
				int c;
				String key = null;
				while ((c = reader.read()) >= 0) {
					switch (c) {
					case '\t':
						if (key == null) {
							key = sb.toString();
							sb = new StringBuilder();
						}
						break;
					case '\n':
						if (key != null) {
							tmp.put(key, sb.toString());
							sb = new StringBuilder();
							key = null;
						}
						break;
					case '\r':
						break;
					case '\\':
						if ((c = reader.read()) >= 0)
							sb.append((char)c);
						break;
					default:
						sb.append((char)c);
						break;
					}
				}
				data = tmp;
				return true;
			} finally {
				if (reader != null)
					reader.close();
			}
		}
		
		public Map<String, String> getMap() {
			return Collections.unmodifiableMap(data);
		}
	}
	
	public class CommandRegistry extends Command {

		@Override
		public String command() {return "fmap";}

		@Override
		public String help(Parameters params) {
			return "fmap set name key value\nfmap remove name key\n[r:controller] fmap new name\n[r:controller] fmap delete name";
		}

		@Override
		public void doCommand(Parameters params, CommandCallback callback) {
			callback.type = EType.Notice;
			
			if (params.hasMoreParams()) {
				String cmd = params.nextParam();
				boolean setKey = cmd.contentEquals("set");
				boolean removeKey = cmd.contentEquals("remove");
				boolean newName = cmd.contentEquals("new");
				boolean deleteName = cmd.contentEquals("delete");
				if (params.hasMoreParams()
						&& (setKey || removeKey || newName || deleteName)) {
					String name = params.nextParam();
					if (newName || deleteName) {
						params.checkController();
						if (newName) {
							if (!factoidRegistry.containsKey(name))
								factoidRegistry.put(name, new FactoidRegistry());
							else {
								callback.append("A map by that name already exists.");
								return;
							}
						} else if (deleteName) {
							if (factoidRegistry.containsKey(name)) {
								new File(registryDirectory,name+".txt").delete();
								factoidRegistry.remove(name);
							}
							else {
								callback.append("A map by that name does not exist.");
								return;
							}
						}
						callback.append("Done.");
						return;
					} else if (params.hasMoreParams() && (setKey || removeKey)) {
						if (!factoidRegistry.containsKey(name))
							callback.append("A map by that name does not exist.");
						else {
							String key = params.nextParam();
							if (setKey) {
								if (!params.hasMoreParams()) {
									callback.append(help(params));
									return;
								}
								String value = params.getParams(0);
								if (factoidRegistry.get(name).size() > 500) {
									callback.append("A maximum of 500 items is allowed.");
									return;
								}
								if (key.length() >= 32 || value.length() >= 256) {
									callback.append("Key length must be less than 32 characters and value length must be less than 256 characters.");
									return;
								}
								factoidRegistry.get(name).put(key, value);
							} else if (removeKey)
								factoidRegistry.get(name).remove(key);
							callback.append("Done.");
						}
						return;
					}
				}
			}
			
			callback.append(help(params));
		}
	}
}
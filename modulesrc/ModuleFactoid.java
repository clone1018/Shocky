import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LuaState;
import org.luaj.vm2.lib.OneArgFunction;
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
import pl.shockah.shocky.interfaces.ILua;
import pl.shockah.shocky.interfaces.IRollback;
import pl.shockah.shocky.lines.Line;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.sql.CriterionNumber;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.QueryUpdate;
import pl.shockah.shocky.sql.SQL;

public class ModuleFactoid extends Module implements IFactoid, ILua {

	protected Command cmdR, cmdF, cmdU, cmdFCMD, cmdManage;
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
		/*
		 * old code for importing factoids in old format if (new
		 * File("data","factoid.cfg").exists()) { Config config = new Config();
		 * config.load(new File("data","factoid.cfg"));
		 * 
		 * ArrayList<String> cfgs = new ArrayList<String>(); cfgs.add(null);
		 * cfgs.addAll(config.getKeysSubconfigs());
		 * 
		 * for (String subc : cfgs) { Config cfg = subc == null ? config :
		 * config.getConfig(subc); ArrayList<String> factoids = new
		 * ArrayList<String>(); for (String s : cfg.getKeys()) if
		 * (s.startsWith("r_")) factoids.add(s); for (String s : factoids) { s =
		 * s.substring(2); QueryInsert q = new
		 * QueryInsert(SQL.getTable("factoid")); q.add("channel",subc == null ?
		 * "" : subc); q.add("factoid",s);
		 * q.add("author",cfg.getString("b_"+s));
		 * q.add("rawtext",cfg.getString("r_"+s)); q.add("stamp",0);
		 * SQL.insert(q); if (cfg.exists("l_"+s)) q.add("locked",1); } }
		 * 
		 * new File("data","factoid.cfg").delete(); }
		 */

		/*
		 * old code for importing crow's factoids if (new
		 * File("data","crowdb.txt").exists()) { ArrayList<String> odd = new
		 * ArrayList<String>(); try { JSONArray base = new
		 * JSONArray(FileLine.readString(new File("data","crowdb.txt"))); for
		 * (int i = 0; i < base.length(); i++) { JSONObject j =
		 * base.getJSONObject(i); String fFactoid = j.getString("name"); String
		 * fRaw = j.getString("data"); String fAuthor =
		 * j.getString("last_changed_by"); boolean fLocked = false; boolean
		 * ignore = false;
		 * 
		 * if (fFactoid.equals("$ioru")) continue; if (fFactoid.equals("$user"))
		 * continue;
		 * 
		 * fRaw.trim(); while (!fRaw.isEmpty() && fRaw.charAt(0) == '<') { if
		 * (fRaw.startsWith("<reply>")) { fRaw = fRaw.substring(7).trim(); }
		 * else if (fRaw.startsWith("<locked")) { fLocked = true; fRaw =
		 * fRaw.substring(fRaw.indexOf('>')+1).trim(); } else if
		 * (fRaw.startsWith("<forgotten>")) { ignore = true; break; } else if
		 * (fRaw.startsWith("<command") || fRaw.startsWith("<pyexec")) {
		 * odd.add(fFactoid+" | "+fAuthor+" | "+fRaw); ignore = true; break; }
		 * else break; }
		 * 
		 * if (ignore) continue;
		 * 
		 * fRaw = fRaw.replace("$inp","%inp%"); fRaw =
		 * fRaw.replace("$ioru","%ioru%"); fRaw =
		 * fRaw.replace("$user","%user%"); fRaw =
		 * fRaw.replace("$chan","%chan%");
		 * 
		 * Factoid f = getLatest(null,fFactoid,true); if (f == null) {
		 * QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
		 * q.add("channel",""); q.add("factoid",fFactoid);
		 * q.add("author",fAuthor); q.add("rawtext",fRaw); q.add("stamp",0); if
		 * (fLocked) q.add("locked",1); SQL.insert(q); } } } catch (Exception e)
		 * {e.printStackTrace();}
		 * 
		 * FileLine.write(new File("data","crowdbodd.txt"),odd); new
		 * File("data","crowdb.txt").delete(); }
		 */

		/*
		 * old code for importing crow's factoids if (new
		 * File("data","crowdbodd.txt").exists()) { ArrayList<String> lines =
		 * FileLine.read(new File("data","crowdbodd.txt")); ArrayList<String>
		 * odd2 = new ArrayList<String>();
		 * 
		 * for (String s : lines) { String[] spl = s.split("|"); String fFactoid
		 * = spl[0].trim(); String fAuthor = spl[1].trim(); String fRaw =
		 * StringTools.implode(spl,2," ").trim(); if
		 * (fRaw.startsWith("<command")) { odd2.add(s); continue; } else if
		 * (fRaw.startsWith("<pyexec>")) fRaw = "<py>"+fRaw.substring(8);
		 * 
		 * QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
		 * q.add("channel",""); q.add("factoid",fFactoid);
		 * q.add("author",fAuthor); q.add("rawtext",fRaw); q.add("stamp",0);
		 * SQL.insert(q); }
		 * 
		 * FileLine.write(new File("data","crowdbodd2.txt"),odd2); new
		 * File("data","crowdbodd.txt").delete(); }
		 */

		Command.addCommands(this, cmdR = new CmdRemember(), cmdF = new CmdForget(), cmdU = new CmdUnforget(), cmdFCMD = new CmdFactoidCmd(), cmdManage = new CmdManage());

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

		func = new Function() {
			public String name() {
				return "escape";
			}

			public String result(String arg) {
				return arg.replace(",", "\\,").replace("(", "\\(")
						.replace(")", "\\)").replace("\\", "\\\\");
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
					return "[Wrong number of arguments to function " + name()
							+ ", expected 2 or 3, got " + arg.length + "]";
				if (arg[0].length() > 0) {
					return arg.length == 2 ? arg[0] : arg[1];
				} else {
					return arg.length == 2 ? arg[1] : arg[2];
				}
			}
		};
		functions.put(func.name(), func);
	}

	public void onDisable() {
		functions.clear();
		Command.removeCommands(fcmds.keySet().toArray(new Command[0]));
		fcmds.clear();
		Command.removeCommands(cmdR, cmdF, cmdU, cmdFCMD, cmdManage);
	}

	public void onDataSave(File dir) {
		ArrayList<String> lines = new ArrayList<String>();
		for (Entry<CmdFactoid, String> fcmd : fcmds.entrySet()) {
			lines.add(fcmd.getValue());
			lines.add(fcmd.getKey().factoid);
		}
		FileLine.write(new File(dir, "factoidCmd.cfg"), lines);
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
		msg = StringTools.trimWhitespace(msg);
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
		String[] args = msg.split(" ");
		if (args.length >= 2 && args[args.length - 2].equals(">")) {
			targetName = args[args.length - 1];
			msg = StringTools.implode(args, 0, args.length - 3, " ");
			args = msg.split(" ");
		} else if (args.length >= 1 && args[args.length - 1].equals("<")) {
			targetName = sender.getNick();
			msg = StringTools.implode(args, 0, args.length - 2, " ");
			args = msg.split(" ");
		} else if (args.length >= 2 && args[args.length - 2].equals("|")) {
			ping = args[args.length - 1];
			msg = StringTools.implode(args, 0, args.length - 3, " ");
			args = msg.split(" ");
		}
		
		String factoid = args[0].toLowerCase();

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
			StringBuilder sb = new StringBuilder();
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
			StringBuilder sb = new StringBuilder();
			if (targetName == null && ping != null) {
				sb.append(ping);
				sb.append(": ");
			}
			sb.append(factoid).append(", last edited by ").append(f.author);
			if (f != null && !f.forgotten)
				Shocky.send(bot, msgtype, channel, target, sb.toString());
			return;
		}

		String[] chain = factoid.split(config.getString("factoid-charchain"));
		for (int i = 0; i < chain.length; i++) {
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

		String message = StringTools.implode(args, 1, " ");
		for (int i = 0; i < chain.length; i++) {
			msg = chain[i] + ' ' + message;
			message = runFactoid(cache, bot, channel, sender, msg);
		}

		if (message != null && message.length() > 0) {
			StringBuilder sb = new StringBuilder(StringTools.formatLines(message));
			if (targetName == null && ping != null) {
				sb.insert(0, ": ");
				sb.insert(0, ping);
			}
			message = StringTools.limitLength(sb);

			Shocky.send(bot, msgtype, channel, target, message);
		}
	}

	public String runFactoid(Cache cache, PircBotX bot, Channel channel, User sender, String message) {
		message = StringTools.trimWhitespace(message);
		LinkedList<String> checkRecursive = new LinkedList<String>();
		while (true) {
			String factoid = message.split(" ")[0].toLowerCase();
			Object key = factoid;

			Factoid f = null;
			if (cache != null && cache.containsKey(factoidHash, key)) {
				Object obj = cache.get(factoidHash, key);
				if (obj instanceof Factoid)
					f = (Factoid) obj;
			}
			f = getFactoid(cache, channel, factoid, false);
			if (f == null)
				break;
			if (cache != null && !cache.containsKey(factoidHash, key))
				cache.put(factoidHash, key, f);
			String raw = f.rawtext;
			if (raw.startsWith("<alias>")) {
				raw = raw.substring(7);
				message = parseVariables(bot, channel, sender, message, raw);
				StringBuilder sb = new StringBuilder();
				parseFunctions(message, sb);
				message = sb.toString();
				if (checkRecursive.contains(message))
					break;
				checkRecursive.add(message);
				continue;
			} else {
				return parse(cache, bot, channel, sender, message, f, raw);
			}
		}
		return null;
	}

	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, String message, Factoid f, String raw) {
		if (raw.startsWith("<noreply>"))
			return "";
		String type = null;
		int closingIndex = -1;
		if (raw.startsWith("<")) {
			closingIndex = raw.indexOf(">");
			if (closingIndex != -1)
				type = raw.substring(1, closingIndex);
		}
		ScriptModule sModule = Module.getScriptingModule(type);
		if (sModule != null) {
			if (channel != null && !sModule.isEnabled(channel.getName()))
				return "";
			raw = raw.substring(closingIndex + 1);
			raw = parseVariables(bot, channel, sender, message, raw);
			String parsed = sModule.parse(cache, bot, channel, sender, f, raw, message);
			return parse(cache, bot, channel, sender, message, f, parsed);
		} else if (type != null && type.contentEquals("cmd")) {
			CommandCallback callback = new CommandCallback();
			raw = raw.substring(closingIndex + 1);
			String[] args = raw.split("\\s+", 2);
			Command cmd = Command.getCommand(bot, sender, channel, EType.Channel, callback, args[0]);
			if (cmd != null && !(cmd instanceof CmdFactoid)) {
				EType etype = (channel == null) ? EType.Notice : EType.Channel;
				raw = (args.length == 1) ? "" : parseVariables(bot, channel, sender, message, args[1]);
				Parameters params = new Parameters(bot, etype, channel, sender, raw);
				try {
					cmd.doCommand(params, callback);
					if (callback.type == EType.Channel)
						return callback.toString();
				} catch (AuthorizationException e) {
				}
			}
			return "";
		} else {
			boolean action = type != null && type.contentEquals("action");
			if (action)
				raw = raw.substring(closingIndex + 1);
			raw = parseVariables(bot, channel, sender, message, raw);
			StringBuilder output = new StringBuilder();
			parseFunctions(raw, output);
			if (action) {
				output.insert(0, "ACTION ");
				output.insert(0, '\001');
				output.append('\001');
			}
			return output.toString();
		}
	}

	private static final Pattern argPattern = Pattern
			.compile("%([A-Za-z]+)([0-9]+)?(-)?([0-9]+)?%");

	public String parseVariables(PircBotX bot, Channel channel, User sender, String message, String raw) {
		StringBuilder escapedMsg = new StringBuilder(message);
		for (int i = 0; i < escapedMsg.length(); i++) {
			switch (escapedMsg.charAt(i)) {
			case '\\':
			case '$':
				escapedMsg.insert(i++, '\\');
			}
		}
		message = escapedMsg.toString();
		String[] args = message.split(" ");
		int req = 0;

		Random rnd = null;
		User[] users = null;

		Matcher m = argPattern.matcher(raw);
		StringBuffer ret = new StringBuffer();
		while (m.find()) {
			String tag = m.group(1);
			String num1str = m.group(2);
			String num2str = m.group(4);

			int num1 = Integer.MIN_VALUE;
			int num2 = Integer.MIN_VALUE;
			if (num1str != null)
				num1 = Integer.parseInt(num1str) + 1;
			if (num2str != null)
				num2 = Integer.parseInt(num2str) + 1;

			boolean range = m.group(3) != null;

			if (tag.contentEquals("arg")) {
				if (range) {
					int min = num1 != Integer.MIN_VALUE ? num1 : 1;
					int max = num2 != Integer.MIN_VALUE ? num2
							: args.length - 1;
					req = Math.max(req, Math.max(min, max));
					if (args.length > req)
						m.appendReplacement(ret, StringTools
								.implode(args, min, max, " "));
				} else if (num1 != Integer.MIN_VALUE) {
					req = Math.max(req, num1);
					if (args.length > req)
						m.appendReplacement(ret, args[num1]);
				}
			} else if (tag.contentEquals("inp"))
				m.appendReplacement(ret, StringTools.implode(args, 1, " "));
			else if (tag.contentEquals("ioru"))
				m.appendReplacement(ret, args.length > 1 ? StringTools
						.implode(args, 1, " ") : sender.getNick());
			else if (tag.contentEquals("bot"))
				m.appendReplacement(ret, bot.getName());
			else if (channel != null && tag.contentEquals("chan"))
				m.appendReplacement(ret, channel.getName());
			else if (tag.contentEquals("user"))
				m.appendReplacement(ret, sender.getNick());
			else if (channel != null && tag.contentEquals("rndn")) {
				if (users == null) {
					rnd = new Random();
					users = channel.getUsers().toArray(new User[0]);
				}
				m.appendReplacement(ret, users[rnd.nextInt(users.length)]
						.getNick());
			}
		}
		m.appendTail(ret);

		if (args.length <= req)
			return String.format("This factoid requires at least %d args", req);

		return ret.toString();
	}

	public String redirectMessage(Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		if (args.length >= 2 && args.length <= 3 && args[1].contentEquals("^") && channel != null) {
			IRollback module = (IRollback) Module.getModule("rollback");
			try {
				if (module != null) {
					User user = null;
					if (args.length == 3) {
						for (User target : channel.getUsers()) {
							if (target.getNick().equalsIgnoreCase(args[2])) {
								user = target;
								break;
							}
						}
					}
					List<LineMessage> lines = module.getRollbackLines(LineMessage.class, channel.getName(), user != null ? user.getNick() : null, null, message, true, 1, 0);
					if (lines.size() == 1) {
						Line line = lines.get(0);
						StringBuilder msg = new StringBuilder(args[0]);
						msg.append(' ');
						msg.append(((LineMessage) line).text);
						message = msg.toString();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return message.substring(1);
	}

	public void parseFunctions(CharSequence input, StringBuilder output) {
		Matcher m = functionPattern.matcher(input);
		int pos = 0;
		while (m.find(pos)) {
			output.append(input.subSequence(pos, m.start()));
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
				if (end == Integer.MIN_VALUE) {
					return;
				} else {
					CharSequence inside = input.subSequence(start, end);
					StringBuilder funcOutput = new StringBuilder();
					parseFunctions(inside, funcOutput);
					try {
						output.append(func.result(funcOutput.toString()));
					} catch (Exception e) {
					}
				}
			} else {
				output.append(m.group());
				pos = m.end();
			}
		}
		output.append(input.subSequence(pos, input.length()));
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

	public abstract class Function {
		public abstract String name();

		public abstract String result(String arg) throws Exception;
	}

	public abstract class FunctionMultiArg extends Function {
		public final String result(String arg) throws Exception {
			ArrayList<String> spl = new ArrayList<String>(Arrays.asList(arg
					.split(",")));
			for (int i = 0; i < spl.size(); i++) {
				String s = spl.get(i);
				spl.set(i, s.length() > 1 ? s.substring(0, s.length() - 1)
						.replace("\\\\", "" + (char) 6)
						+ s.substring(s.length() - 1) : s);
			}
			for (int i = 0; i < spl.size(); i++)
				if (spl.size() - 1 > i && spl.get(i).endsWith("\\")) {
					spl.set(i, spl.get(i).substring(0, spl.get(i).length() - 1)
							+ "," + spl.get(i + 1));
					spl.remove(i + 1);
					i--;
				}

			return result(spl.toArray(new String[spl.size()]));
		}

		public abstract String result(String[] arg) throws Exception;
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

	private static class FactoidFunction extends OneArgFunction {
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
			if (raw.startsWith("<")) {
				int closingIndex = raw.indexOf(">");
				if (closingIndex != -1)
					type = raw.substring(1, closingIndex);
			}
			if (type != null)
				return Module.getScriptingModule(type);
			return null;
		}

		@Override
		public LuaValue call(LuaValue arg) {
			LuaState state = getState();
			if (state == null)
				return NIL;
			IFactoid module = state.getFactoidModule();
			if (module == null)
				return NIL;
			String args = arg.optjstring(null);
			StringBuilder message = new StringBuilder(factoid);
			if (args != null)
				message.append(' ').append(args);
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
			boolean src = name.equals("src");
			boolean author = false;
			boolean time = false;
			boolean data = false;
			boolean history = false;
			if (!src)
				author = name.equals("author");
			if (!src && !author)
				time = name.equals("time");
			if (!src && !author && !time)
				data = name.equals("data");
			if (!src && !author && !time && !data)
				history = name.equals("history");
			if (src || author || time || data || history) {
				LuaState state = getState();
				if (state == null)
					return NIL;
				Factoid f = getFactoid(state);
				if (f == null)
					return NIL;
				if (src)
					return valueOf(f.rawtext);
				if (author)
					return valueOf(f.author);
				if (time)
					return valueOf(f.stamp);
				if (data) {
					ScriptModule sModule = getScriptModule(f);
					if (sModule != null && sModule instanceof IFactoidData) {
						IFactoidData dModule = (IFactoidData) sModule;
						String s = dModule.getData(f);
						if (s != null)
							return valueOf(s);
					}
					return NIL;
				}
				if (history) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		env.set("factoid", new FactoidData());
	}
}
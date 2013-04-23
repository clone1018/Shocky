import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;
import org.pircbotx.*;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import pl.shockah.*;
import pl.shockah.shocky.*;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.lines.Line;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.prototypes.IFactoid;
import pl.shockah.shocky.prototypes.IRollback;
import pl.shockah.shocky.sql.CriterionNumber;
import pl.shockah.shocky.sql.CriterionString;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.QueryUpdate;
import pl.shockah.shocky.sql.SQL;

public class ModuleFactoid extends Module implements IFactoid {

	protected Command cmdR, cmdF, cmdU, cmdFCMD, cmdManage;
	private final Map<CmdFactoid,String> fcmds = new HashMap<CmdFactoid,String>();
	private final HashMap<String,Function> functions = new HashMap<String,Function>();
	private static final Pattern functionPattern = Pattern.compile("(?<!\\\\)\\$([a-zA-Z_][a-zA-Z0-9_]*)\\(.*?\\)");
	private static final int factoidHash = "factoid".hashCode();
	
	public String name() {return "factoid";}
	public boolean isListener() {return true;}
	public void onEnable() {
		Data.config.setNotExists("factoid-char","?!");
		Data.config.setNotExists("factoid-charraw","+");
		Data.config.setNotExists("factoid-charby","-");
		Data.config.setNotExists("factoid-charchain",">");
		Data.config.setNotExists("factoid-show",true);
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Data.config.setNotExists("python-url","http://eval.appspot.com/eval");
		Data.protectedKeys.add("php-url");
		Data.protectedKeys.add("python-url");
		
		SQL.raw("CREATE TABLE IF NOT EXISTS "+SQL.getTable("factoid")+" (channel varchar(50) DEFAULT NULL,factoid text NOT NULL,author text NOT NULL,rawtext text NOT NULL,stamp int(10) unsigned NOT NULL,locked int(1) unsigned NOT NULL DEFAULT '0',forgotten int(1) unsigned NOT NULL DEFAULT '0',KEY channel (channel)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
		/* old code for importing factoids in old format
		 * if (new File("data","factoid.cfg").exists()) {
			Config config = new Config();
			config.load(new File("data","factoid.cfg"));
			
			ArrayList<String> cfgs = new ArrayList<String>();
			cfgs.add(null);
			cfgs.addAll(config.getKeysSubconfigs());
			
			for (String subc : cfgs) {
				Config cfg = subc == null ? config : config.getConfig(subc);
				ArrayList<String> factoids = new ArrayList<String>();
				for (String s : cfg.getKeys()) if (s.startsWith("r_")) factoids.add(s);
				for (String s : factoids) {
					s = s.substring(2);
					QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
					q.add("channel",subc == null ? "" : subc);
					q.add("factoid",s);
					q.add("author",cfg.getString("b_"+s));
					q.add("rawtext",cfg.getString("r_"+s));
					q.add("stamp",0);
					SQL.insert(q);
					if (cfg.exists("l_"+s)) q.add("locked",1);
				}
			}
			
			new File("data","factoid.cfg").delete();
		}*/
		
		/* old code for importing crow's factoids
		 * if (new File("data","crowdb.txt").exists()) {
			ArrayList<String> odd = new ArrayList<String>();
			try {
				JSONArray base = new JSONArray(FileLine.readString(new File("data","crowdb.txt")));
				for (int i = 0; i < base.length(); i++) {
					JSONObject j = base.getJSONObject(i);
					String fFactoid = j.getString("name");
					String fRaw = j.getString("data");
					String fAuthor = j.getString("last_changed_by");
					boolean fLocked = false; boolean ignore = false;
					
					if (fFactoid.equals("$ioru")) continue;
					if (fFactoid.equals("$user")) continue;
					
					fRaw.trim();
					while (!fRaw.isEmpty() && fRaw.charAt(0) == '<') {
						if (fRaw.startsWith("<reply>")) {
							fRaw = fRaw.substring(7).trim();
						} else if (fRaw.startsWith("<locked")) {
							fLocked = true;
							fRaw = fRaw.substring(fRaw.indexOf('>')+1).trim();
						} else if (fRaw.startsWith("<forgotten>")) {
							ignore = true;
							break;
						} else if (fRaw.startsWith("<command") || fRaw.startsWith("<pyexec")) {
							odd.add(fFactoid+" | "+fAuthor+" | "+fRaw);
							ignore = true;
							break;
						} else break;
					}
					
					if (ignore) continue;
					
					fRaw = fRaw.replace("$inp","%inp%");
					fRaw = fRaw.replace("$ioru","%ioru%");
					fRaw = fRaw.replace("$user","%user%");
					fRaw = fRaw.replace("$chan","%chan%");
					
					Factoid f = getLatest(null,fFactoid,true);
					if (f == null) {
						QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
						q.add("channel","");
						q.add("factoid",fFactoid);
						q.add("author",fAuthor);
						q.add("rawtext",fRaw);
						q.add("stamp",0);
						if (fLocked) q.add("locked",1);
						SQL.insert(q);
					}
				}
			} catch (Exception e) {e.printStackTrace();}
			
			FileLine.write(new File("data","crowdbodd.txt"),odd);
			new File("data","crowdb.txt").delete();
		}*/
		
		/* old code for importing crow's factoids
		 * if (new File("data","crowdbodd.txt").exists()) {
			ArrayList<String> lines = FileLine.read(new File("data","crowdbodd.txt"));
			ArrayList<String> odd2 = new ArrayList<String>();
			
			for (String s : lines) {
				String[] spl = s.split("|");
				String fFactoid = spl[0].trim();
				String fAuthor = spl[1].trim();
				String fRaw = StringTools.implode(spl,2," ").trim();
				if (fRaw.startsWith("<command")) {
					odd2.add(s);
					continue;
				} else if (fRaw.startsWith("<pyexec>")) fRaw = "<py>"+fRaw.substring(8);
				
				QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
				q.add("channel","");
				q.add("factoid",fFactoid);
				q.add("author",fAuthor);
				q.add("rawtext",fRaw);
				q.add("stamp",0);
				SQL.insert(q);
			}
			
			FileLine.write(new File("data","crowdbodd2.txt"),odd2);
			new File("data","crowdbodd.txt").delete();
		}*/
		
		Command.addCommands(this, cmdR = new CmdRemember(),cmdF = new CmdForget(),cmdU = new CmdUnforget(),cmdFCMD = new CmdFactoidCmd(),cmdManage = new CmdManage());
		
		Command.addCommand(this, "r", cmdR);
		Command.addCommand(this, "f", cmdF);
		Command.addCommand(this, "fcmd", cmdFCMD);
		Command.addCommand(this, "fmanage", cmdManage);
		Command.addCommand(this, "fmng", cmdManage);
		
		ArrayList<String> lines = FileLine.read(new File("data","factoidCmd.cfg"));
		for (int i = 0; i < lines.size(); i += 2) {
			String name = lines.get(i);
			String names[] = name.split(";");
			String factoid = lines.get(i+1);
			CmdFactoid cmd = new CmdFactoid(names[0],factoid);
			fcmds.put(cmd, name);
			Command.addCommand(this, cmd.command(), cmd);
			for (int o = 1; o < names.length; o++) {
				Command.addCommand(this, names[o],cmd);
			}
		}

		Function func;
		
		func = new Function(){
			public String name() {return "ucase";}
			public String result(String arg) {return arg.toUpperCase();}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "lcase";}
			public String result(String arg) {return arg.toLowerCase();}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "reverse";}
			public String result(String arg) {return new StringBuilder(arg).reverse().toString();}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "munge";}
			public String result(String arg) {return Utils.mungeNick(arg);}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "escape";}
			public String result(String arg) {return arg.replace(",","\\,").replace("(","\\(").replace(")","\\)").replace("\\","\\\\");}
		};
		functions.put(func.name(), func);
		
		func = new FunctionMultiArg(){
			public String name() {return "repeat";}
			public String result(String[] arg) {
				if (arg.length != 2) return "[Wrong number of arguments to function "+name()+", expected 2, got "+arg.length+"]";
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < Integer.parseInt(arg[1]); i++) sb.append(arg[0]);
				return sb.toString();
			}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "bitflip";}
			public String result(String arg) throws Exception {
				byte[] array = arg.getBytes(Helper.utf8);
				for (int i = 0; i < array.length; i++) array[i] = (byte) (~array[i] - 0x80 & 0xFF);
				return new String(array, Helper.utf8).replaceAll("[\\r\\n]", "");
			}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "flip";}
			public String result(String arg) {return Utils.flip(arg);}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "odd";}
			public String result(String arg) {return Utils.odd(arg);}
		};
		functions.put(func.name(), func);
		
		func = new Function(){
			public String name() {return "rot13";}
			public String result(String arg) {
				char[] out = new char[arg.length()];
				for (int i = 0; i < arg.length(); i++) {
					char c = arg.charAt(i);
					if (c >= 'a' && c <= 'm') c += 13;
					else if (c >= 'n' && c <= 'z') c -= 13;
					else if (c >= 'A' && c <= 'M') c += 13;
					else if (c >= 'A' && c <= 'Z') c -= 13;
					out[i] = c;
				}
				return new String(out);
			}
		};
		functions.put(func.name(), func);
		
		func = new FunctionMultiArg(){
			public String name() {return "if";}
			public String result(String[] arg) {
				if (arg.length < 2 || arg.length > 3) return "[Wrong number of arguments to function "+name()+", expected 2 or 3, got "+arg.length+"]";
				if (arg[0].length()>0) {
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
		Command.removeCommands(cmdR,cmdF,cmdU,cmdFCMD,cmdManage);
	}
	public void onDataSave() {
		ArrayList<String> lines = new ArrayList<String>();
		for (Entry<CmdFactoid, String> fcmd : fcmds.entrySet()) {
			lines.add(fcmd.getValue());
			lines.add(fcmd.getKey().factoid);
		}
		FileLine.write(new File("data","factoidCmd.cfg"),lines);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		onMessage(event.getBot(),event.getChannel(),event.getUser(),event.getMessage());
	}
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		onMessage(event.getBot(),null,event.getUser(),event.getMessage());
	}
	public void onNotice(NoticeEvent<PircBotX> event) {
		if (Data.isBlacklisted(event.getUser())) return;
		onMessage(event.getBot(),null,event.getUser(),event.getMessage());
	}
	public void onMessage(PircBotX bot, Channel channel, User sender, String msg) {
		msg = StringTools.trimWhitespace(msg);
		if (msg.length() < 2) return;
		Config config = channel == null ? Data.config : Data.forChannel(channel);
		String chars = config.getString("factoid-char");
		
		for (int i = 0; i < chars.length(); i++) if (msg.charAt(0) == chars.charAt(i)) {
			msg = redirectMessage(channel, sender, msg);
			String charsraw = config.getString("factoid-charraw");
			String charsby = config.getString("factoid-charby");
			
			String[] args = msg.split(" ");
			String target = null;
			String ping = null;
			if (args.length >= 2 && args[args.length-2].equals(">")) {
				target = args[args.length-1];
				msg = StringTools.implode(args,0,args.length-3," ");
			} else if (args.length >= 1 && args[args.length-1].equals("<")) {
				target = sender.getNick();
				msg = StringTools.implode(args,0,args.length-2," ");
			} else if (args.length >= 2 && args[args.length-2].equals("|")) {
				ping = args[args.length-1];
				msg = StringTools.implode(args,0,args.length-3," ");
			}
			
			if (target != null) {
				if (channel == null) return;
				boolean found = false;
				for (User user : channel.getUsers()) if (user.getNick().equals(target)) {
					found = true;
					break;
				}
				if (!found) return;
			}
			
			for (i = 0; i < charsraw.length(); i++) if (msg.charAt(0) == charsraw.charAt(i)) {
				msg = new StringBuilder(msg).deleteCharAt(0).toString().split(" ")[0].toLowerCase();
				Factoid f = getFactoid(channel != null ? channel.getName() : null,msg,false);
				if (f == null)
					return;
				StringBuilder sb = new StringBuilder();
				if (target == null && ping != null) {
					sb.append(ping);
					sb.append(": ");
				}
				sb.append(msg);
				sb.append(": ");
				sb.append(StringTools.formatLines(f.rawtext));
				Shocky.send(bot,target != null?Command.EType.Notice:(channel == null ? Command.EType.Notice : Command.EType.Channel),channel,channel == null ? sender : Shocky.getUser(target),StringTools.limitLength(sb.toString()));
				return;
			}
			for (i = 0; i < charsby.length(); i++) if (msg.charAt(0) == charsby.charAt(i)) {
				msg = new StringBuilder(msg).deleteCharAt(0).toString().split(" ")[0].toLowerCase();
				Factoid f = getFactoid(channel != null ? channel.getName() : null,msg,false);
				if (f == null)
					return;
				StringBuilder sb = new StringBuilder();
				if (target == null && ping != null) {
					sb.append(ping);
					sb.append(": ");
				}
				sb.append(msg);
				sb.append(", last edited by ");
				sb.append(f.author);
				if (f != null && !f.forgotten) Shocky.send(bot,target != null?Command.EType.Notice:(channel == null ? Command.EType.Notice : Command.EType.Channel),channel,channel == null ? sender : Shocky.getUser(target),sb.toString());
				return;
			}
			
			args = msg.split(" ");
			String factoid = args[0].toLowerCase();
			String[] chain = factoid.split(config.getString("factoid-charchain"));
			HashMap<Integer,Object> cache = new HashMap<Integer,Object>();
			for (i = 0; i < chain.length; i++) {
				int key = factoidHash+chain[i].hashCode();
				Factoid f = null;
				if (cache != null) {
					if (cache.containsKey(key))
					{
						Object obj = cache.get(key);
						if (obj instanceof Factoid)
							f = (Factoid)obj;
					}
				}
				if (f == null)
					f = getFactoid(channel != null ? channel.getName() : null,chain[i],false);
				if (f == null)
					return;
				if (cache != null && !cache.containsKey(key))
					cache.put(key, f);
			}

			String message = StringTools.implode(args, 1," ");
			for (i = 0; i < chain.length; i++) {
				msg = chain[i] + ' ' + message;
				message = runFactoid(cache, bot, channel, sender, msg);
			}
			
			if (message != null && message.length() > 0) {
				StringBuilder sb = new StringBuilder(StringTools.formatLines(message));
				if (target == null && ping != null) {
					sb.insert(0, ": ");
					sb.insert(0, ping);
				}
				message = StringTools.limitLength(sb);
				
				Shocky.send(bot, target != null?Command.EType.Notice:(channel == null ? Command.EType.Notice : Command.EType.Channel), channel, channel == null ? sender : Shocky.getUser(target), message);
			}
		}
	}
	
	public String runFactoid(Map<Integer,Object> cache, PircBotX bot, Channel channel, User sender, String message) {
		message = StringTools.trimWhitespace(message);
		LinkedList<String> checkRecursive = new LinkedList<String>();
		while (true) {
			String factoid = message.split(" ")[0].toLowerCase();
			int key = factoidHash+factoid.hashCode();

			Factoid f = null;
			if (cache != null) {
				if (cache.containsKey(key))
				{
					Object obj = cache.get(key);
					if (obj instanceof Factoid)
						f = (Factoid)obj;
				}
			}
			f = getFactoid(channel != null ? channel.getName() : null,factoid,false);
			if (f == null)
				break;
			if (cache != null && !cache.containsKey(key))
				cache.put(key, f);
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
				return parse(cache, bot, channel, sender, message, raw);
			}
		}
		return null;
	}
	
	public String parse(Map<Integer,Object> cache, PircBotX bot, Channel channel, User sender, String message, String raw) {
		if (raw.startsWith("<noreply>"))
			return "";
		String type = null;
		if (raw.startsWith("<")) {
			int closingIndex = raw.indexOf(">");
			if (closingIndex != -1) {
				type = raw.substring(1, closingIndex);
				raw = raw.substring(closingIndex+1);
			}
		}
		ScriptModule sModule = Module.getScriptingModule(type);
		if (sModule != null) {
			raw = parseVariables(bot, channel, sender, message, raw);
			String parsed = sModule.parse(cache, bot, channel == null ? EType.Notice : EType.Channel, channel, sender, raw, message);
			return parse(cache, bot, channel, sender, message, parsed);
		} else if (type != null && type.contentEquals("cmd")) {
			CommandCallback callback = new CommandCallback();
			Command cmd = Command.getCommand(bot,sender,channel.getName(),EType.Channel,callback,raw);
			if (cmd != null && !(cmd instanceof CmdFactoid)) {
				raw = parseVariables(bot, channel, sender, message, raw);
				cmd.doCommand(bot,channel == null ? EType.Notice : EType.Channel,callback,channel,sender,raw);
				if (callback.type == EType.Channel)
					return callback.toString();
			}
			return "";
		} else {
			raw = parseVariables(bot, channel, sender, message, raw);
			StringBuilder output = new StringBuilder();
			parseFunctions(raw,output);
			if (type != null && type.contentEquals("action")) {
				output.insert(0,"ACTION ");
				output.insert(0,'\001');
				output.append('\001');
			}
			return output.toString();
		}
	}
	
	private static final Pattern argPattern = Pattern.compile("%([A-Za-z]+)([0-9]+)?(-)?([0-9]+)?%");
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
				num1 = Integer.parseInt(num1str)+1;
			if (num2str != null)
				num2 = Integer.parseInt(num2str)+1;
			
			boolean range = m.group(3) != null;
			
			if (tag.contentEquals("arg")) {
					if (range) {
						int min = num1 != Integer.MIN_VALUE ? num1 : 1;
						int max = num2 != Integer.MIN_VALUE ? num2 : args.length-1;
						req = Math.max(req, Math.max(min, max));
						if (args.length > req)
							m.appendReplacement(ret, StringTools.implode(args, min, max, " "));
					}
					else if (num1 != Integer.MIN_VALUE) {
						req = Math.max(req, num1);
						if (args.length > req)
							m.appendReplacement(ret, args[num1]);
					}
			}
			else if (tag.contentEquals("inp"))
				m.appendReplacement(ret, StringTools.implode(args,1," "));
			else if (tag.contentEquals("ioru"))
				m.appendReplacement(ret, args.length > 1 ? StringTools.implode(args,1," ") : sender.getNick());
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
				m.appendReplacement(ret, users[rnd.nextInt(users.length)].getNick());
			}
		}
		m.appendTail(ret);
		
		if (args.length <= req)
			return String.format("This factoid requires at least %d args",req);
		
		return ret.toString();
	}
	
	public String redirectMessage(Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		if (args.length >= 2 && args.length <= 3 && args[1].contentEquals("^") && channel != null) {
			IRollback module = (IRollback)Module.getModule("rollback");
			try {
			if (module != null) {
				User user = null;
				if (args.length == 3) {
					for (User target : channel.getUsers()) {
						if (target.getNick().contentEquals(args[2])) {
							user = target;
							break;
						}
					}
				}
				ArrayList<LineMessage> lines = module.getRollbackLines(LineMessage.class, channel.getName(), user != null ? user.getNick() : null, null, message, true, 1, 0);
				if (lines.size() == 1) {
					Line line = lines.get(0);
					StringBuilder msg = new StringBuilder(args[0]);
					msg.append(' ');
					msg.append(((LineMessage)line).text);
					message = msg.toString();
				}
			}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return message.substring(1);
	}
	
	public void parseFunctions(CharSequence input, StringBuilder output) {
		Matcher m = functionPattern.matcher(input);
		int pos = 0;
		while (m.find(pos)) {
			output.append(input.subSequence(pos,m.start()));
			String fName = m.group(1);
			Function func = null;
			if (functions.containsKey(fName))
				func = functions.get(fName);
			if (func != null) {
				int start = m.end(1)+1;
				int end = Integer.MIN_VALUE;
				int expected = 1;
				for (int i = start; i < input.length(); i++) {
					char c = input.charAt(i);
					if (c == '(')
						expected++;
					else if (c == ')')
						expected--;
					if (expected == 0)
					{
						end = i;
						pos = end + 1;
						break;
					}
				}
				if (end == Integer.MIN_VALUE) {
					return;
				}
				else {
					CharSequence inside = input.subSequence(start, end);
					StringBuilder funcOutput = new StringBuilder();
					parseFunctions(inside,funcOutput);
					try {
					output.append(func.result(funcOutput.toString()));
					} catch(Exception e) {
					}
				}
			}
			else {
				output.append(m.group());
				pos = m.end();
			}
		}
		output.append(input.subSequence(pos, input.length()));
	}
	
	public Factoid getFactoid(String channel, String factoid) {
		return getFactoid(channel, factoid, false);
	}
	
	public Factoid getFactoid(String channel, String factoid, boolean forgotten) {
		ResultSet j;
		PreparedStatement p;
		boolean hasChannel = (channel != null);
		String key;
		if (hasChannel)
			key = "factoid-getChannelFactoid";
		else
			key = "factoid-getFactoid";
		try {
			if (SQL.statements.containsKey(key) && !SQL.statements.get(key).isClosed()) {
				p = SQL.statements.get(key);
			} else {
				/*QuerySelect q = new QuerySelect(SQL.getTable("factoid"));
				q.addCriterions(new Wildcard("channel",Operation.Equals));
				q.addCriterions(new Wildcard("factoid",Operation.Equals));
				q.addCriterions(new Wildcard("forgotten",Operation.Equals));
				q.addOrder("stamp",false);
				q.setLimitCount(1);
				p = SQL.getSQLConnection().prepareStatement(q.getSQLQuery());*/
				if (hasChannel)
					p = SQL.getSQLConnection().prepareStatement("SELECT * FROM factoid WHERE ((channel IS NULL OR channel=?) AND factoid=? AND forgotten=?) ORDER BY channel DESC, stamp DESC LIMIT 1");
				else
					p = SQL.getSQLConnection().prepareStatement("SELECT * FROM factoid WHERE (channel IS NULL AND factoid=? AND forgotten=?) ORDER BY channel DESC, stamp DESC LIMIT 1");
				SQL.statements.put(key,p);
			}
			synchronized (p) {
				int i = 1;
				if (hasChannel)
					p.setString(i++, channel.toLowerCase());
				p.setString(i++, factoid.toLowerCase());
				p.setInt(i++, forgotten?1:0);
				j = p.executeQuery();
				p.clearParameters();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
			
		if (j == null)
			return null;
		return Factoid.fromResultSet(j);
	}
	
	public abstract class Function {
		public abstract String name();
		public abstract String result(String arg) throws Exception;
	}
	public abstract class FunctionMultiArg extends Function {
		public final String result(String arg) throws Exception {
			ArrayList<String> spl = new ArrayList<String>(Arrays.asList(arg.split(",")));
			for (int i = 0; i < spl.size(); i++) {
				String s = spl.get(i);
				spl.set(i,s.length() > 1 ? s.substring(0,s.length()-1).replace("\\\\",""+(char)6)+s.substring(s.length()-1) : s);
			}
			for (int i = 0; i < spl.size(); i++) if (spl.size()-1 > i && spl.get(i).endsWith("\\")) {
				spl.set(i,spl.get(i).substring(0,spl.get(i).length()-1)+","+spl.get(i+1));
				spl.remove(i+1);
				i--;
			}
			
			return result(spl.toArray(new String[spl.size()]));
		}
		public abstract String result(String[] arg) throws Exception;
	}
	
	public class CmdRemember extends Command {
		public String command() {return "remember";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("remember/r");
			sb.append("\nremember [.] {name} {raw} - remembers a factoid (use \".\" for local factoids)");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = StringTools.trimWhitespace(message).split(" ");
			callback.type = EType.Notice;
			if (args.length < 3 || (args.length == 3 && args[1].equals("."))) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String prefix = args[1].equals(".") ? channel.getName() : null;
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			String rem = StringTools.implode(args,args[1].equals(".") ? 3 : 2," ");
			
			Factoid f = getFactoid(prefix,name,false);
			if (f != null && f.locked) callback.append("Factoid is locked"); else {
				QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
				q.add("channel",prefix);
				q.add("factoid",name);
				q.add("author",sender.getNick());
				q.add("rawtext",rem);
				q.add("stamp",System.currentTimeMillis()/1000);
				SQL.insert(q);
				callback.append("Done.");
			}
		}
	}
	public class CmdForget extends Command {
		public String command() {return "forget";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("forget/f");
			sb.append("\nforget [.] {name} - forgets a factoid (use \".\" for local factoids)");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			callback.type = EType.Notice;
			if (args.length < 2 || (args.length == 2 && args[1].equals("."))) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String prefix = args[1].equals(".") ? channel.getName() : null;
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			
			Factoid f = getFactoid(prefix,name,false);
			if (f == null) callback.append("No such factoid"); else {
				if (f.locked) callback.append("Factoid is locked");
				else {
					QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
					q.addCriterions(new CriterionString("channel",prefix));
					q.addCriterions(new CriterionString("factoid",name));
					q.addCriterions(new CriterionString("rawtext",f.rawtext));
					q.addCriterions(new CriterionNumber("stamp",CriterionNumber.Operation.Equals,f.stamp/1000));
					q.set("forgotten",1);
					SQL.update(q);
					Shocky.sendNotice(bot,sender,"Done. Forgot: " + StringTools.limitLength(StringTools.formatLines(f.rawtext)));
				}
			}
		}
	}
	
	public class CmdUnforget extends Command {
		public String command() {return "unforget";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("unforget/f");
			sb.append("\nunforget [.] {name} - unforgets a factoid (use \".\" for local factoids)");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			callback.type = EType.Notice;
			if (args.length < 2 || (args.length == 2 && args[1].equals("."))) {
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String prefix = args[1].equals(".") ? channel.getName() : null;
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			
			Factoid f = getFactoid(prefix,name,true);
			if (f == null) callback.append("No such factoid"); else {
				if (f.locked) callback.append("Factoid is locked");
				else {
					QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
					q.addCriterions(new CriterionString("channel",prefix));
					q.addCriterions(new CriterionString("factoid",name));
					q.addCriterions(new CriterionString("rawtext",f.rawtext));
					q.addCriterions(new CriterionNumber("stamp",CriterionNumber.Operation.Equals,f.stamp/1000));
					q.set("forgotten",0);
					SQL.update(q);
					Shocky.sendNotice(bot,sender,"Done. Unforgot: " + StringTools.limitLength(StringTools.formatLines(f.rawtext)));
				}
			}
		}
	}
	
	public class CmdFactoidCmd extends Command {
		public String command() {return "factoidcmd";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("factoidcmd/fcmd");
			sb.append("\nfactoidcmd - lists commands being aliases for factoids");
			sb.append("\nfactoidcmd add {command};{alias1};{alias2};(...) {factoid} - makes a new command being an alias");
			sb.append("\nfactoidcmd remove {command} - removes a command being an alias");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (!canUseController(bot,type,sender)) return;
			callback.type = EType.Notice;
			
			String[] args = message.split(" ");
			if (args.length == 1) {
				StringBuilder sb = new StringBuilder();
				for (Entry<CmdFactoid, String> cmd : fcmds.entrySet()) {
					if (sb.length() != 0) sb.append(", ");
					sb.append(cmd.getValue()+"->"+cmd.getKey().factoid);
				}
				callback.append(sb);
				return;
			} else if (args.length == 3 && args[1].equals("remove")) {
				for (Entry<CmdFactoid, String> c : fcmds.entrySet()) {
					for (String s : c.getValue().split(";")) if (s.equals(args[2])) {
						Command.removeCommands(c.getKey());
						fcmds.remove(c.getKey());
						callback.append("Removed.");
						return;
					}
				}
				return;
			} else if (args.length == 4 && args[1].equals("add")) {
				for (Iterator<Entry<CmdFactoid, String>> iter = fcmds.entrySet().iterator(); iter.hasNext();) {
					Entry<CmdFactoid, String> c = iter.next();
					for (String s : c.getValue().split(";")) if (s.equals(args[2])) {
						Command.removeCommands(c.getKey());
						iter.remove();
						break;
					}
				}
				String name = args[2];
				String names[] = name.split(";");
				CmdFactoid cmd = new CmdFactoid(names[0],args[3].toLowerCase());
				fcmds.put(cmd, name);
				Command.addCommand(this, cmd.command(), cmd);
				for (int i = 1; i < names.length; i++) {
					Command.addCommand(this, names[i], cmd);
				}
				callback.append("Added.");
				return;
			}
			
			callback.append(help(bot,type,channel,sender));
		}
	}
	public class CmdFactoid extends Command {
		public final String cmd;
		public final String factoid;
		
		public CmdFactoid(String command, String factoid) {
			this.cmd = command;
			this.factoid = factoid;
		}
		
		public String command() {return cmd;}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {return "";}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (type != EType.Channel) return;
			String[] args = message.split(" ");
			onMessage(bot,channel,sender,""+Data.forChannel(channel).getString("factoid-char").charAt(0)+factoid+(args.length > 1 ? " "+StringTools.implode(args,1," ") : ""));
		}
	}
	public class CmdManage extends Command {
		public String command() {return "factoidmanage";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("factoidmanage/fmanage/fmng");
			sb.append("\n[r:op/controller] factoidmanage lock [.] {factoid} - locks a factoid");
			sb.append("\n[r:op/controller] factoidmanage unlock [.] {factoid} - unlocks a factoid");
			return sb.toString();
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (!canUseAny(bot,type,channel,sender)) return;
			callback.type = EType.Notice;
			
			String[] args = message.split(" ");
			if (args.length == 3 || args.length == 4) {
				boolean local = args[2].equals(".");
				if (args.length == 3+(local ? 1 : 0)) {
					String factoid = (local ? args[3] : args[2]).toLowerCase();
					if (args[1].equals("lock")) {
						if ((local && canUseOp(bot,type,channel,sender)) || (!local && canUseController(bot,type,sender))) {
							Factoid f = getFactoid(local ? channel.getName() : null,factoid,false);
							if (f == null) {
								callback.append("No such factoid");
								return;
							}
							if (f.locked) {
								callback.append("Already locked");
								return;
							}
							
							QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
							q.set("locked",1);
							q.addCriterions(new CriterionString("channel",local ? channel.getName() : null),new CriterionString("factoid",factoid));
							q.addOrder("stamp",false);
							q.setLimitCount(1);
							SQL.update(q);
							callback.append("Done");
							return;
						}
					} else if (args[1].equals("unlock")) {
						if ((local && canUseOp(bot,type,channel,sender)) || (!local && canUseController(bot,type,sender))) {
							Factoid f = getFactoid(local ? channel.getName() : null,factoid);
							if (f == null) {
								callback.append("No such factoid");
								return;
							}
							if (!f.locked) {
								callback.append("Already unlocked");
								return;
							}
							
							QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
							q.set("locked",0);
							q.addCriterions(new CriterionString("channel",local ? channel.getName() : null),new CriterionString("factoid",factoid));
							q.addOrder("stamp",false);
							q.setLimitCount(1);
							SQL.update(q);
							callback.append("Done");
							return;
						}
					}
				}
			}
			
			callback.append(help(bot,type,channel,sender));
		}
	}
}
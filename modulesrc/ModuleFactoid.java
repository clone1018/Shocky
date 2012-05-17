import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pircbotx.*;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.*;
import pl.shockah.shocky.*;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.lines.Line;
import pl.shockah.shocky.lines.LineMessage;
import pl.shockah.shocky.sql.CriterionNumber;
import pl.shockah.shocky.sql.CriterionStringEquals;
import pl.shockah.shocky.sql.QueryInsert;
import pl.shockah.shocky.sql.QuerySelect;
import pl.shockah.shocky.sql.QueryUpdate;
import pl.shockah.shocky.sql.SQL;

public class ModuleFactoid extends Module {
	protected Command cmdR, cmdF, cmdFCMD, cmdManage;
	private ArrayList<CmdFactoid> fcmds = new ArrayList<CmdFactoid>();
	private HashMap<String,Function> functions = new HashMap<String,Function>();
	private static Pattern functionPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\(.*?\\)");
	
	public String name() {return "factoid";}
	public void onEnable() {
		Data.config.setNotExists("factoid-char","?!");
		Data.config.setNotExists("factoid-charraw","+");
		Data.config.setNotExists("factoid-charby","-");
		Data.config.setNotExists("factoid-show",true);
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		Data.config.setNotExists("python-url","http://eval.appspot.com/eval");
		
		SQL.raw("CREATE TABLE IF NOT EXISTS "+SQL.getTable("factoid")+" (channel TEXT NOT NULL,factoid TEXT,author TEXT,rawtext TEXT,stamp INT(10) UNSIGNED NOT NULL,locked INT(1) UNSIGNED NOT NULL DEFAULT 0,forgotten INT(1) UNSIGNED NOT NULL DEFAULT 0)");
		
		if (new File("data","factoid.cfg").exists()) {
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
		}
		
		if (new File("data","crowdb.txt").exists()) {
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
		}
		
		if (new File("data","crowdbodd.txt").exists()) {
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
		}
		
		ArrayList<String> lines = FileLine.read(new File("data","factoidCmd.cfg"));
		for (int i = 0; i < lines.size(); i += 2) fcmds.add(new CmdFactoid(lines.get(i),lines.get(i+1)));
		
		Command.addCommands(cmdR = new CmdRemember(),cmdF = new CmdForget(),cmdFCMD = new CmdFactoidCmd(),cmdManage = new CmdManage());
		Command.addCommands(fcmds.toArray(new Command[fcmds.size()]));

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
			public String name() {return "flip";}
			public String result(String arg) {return Utils.flip(arg);}
		};
		functions.put(func.name(), func);
	}
	public void onDisable() {
		functions.clear();
		Command.removeCommands(fcmds.toArray(new Command[fcmds.size()]));
		fcmds.clear();
		Command.removeCommands(cmdR,cmdF,cmdFCMD,cmdManage);
	}
	public void onDataSave() {
		ArrayList<String> lines = new ArrayList<String>();
		for (CmdFactoid fcmd : fcmds) {
			StringBuilder sb = new StringBuilder();
			for (String s : fcmd.cmds) {
				if (sb.length() != 0) sb.append(";");
				sb.append(s);
			}
			lines.add(sb.toString());
			lines.add(fcmd.factoid);
		}
		FileLine.write(new File("data","factoidCmd.cfg"),lines);
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		if (Data.blacklistNicks.contains(event.getUser().getNick().toLowerCase())) return;
		onMessage(event.getBot(),event.getChannel(),event.getUser(),event.getMessage());
	}
	public void onMessage(PircBotX bot, Channel channel, User sender, String msg) {
		if (msg.length() < 2) return;
		String chars = Data.config.getString("factoid-char");
		
		for (int i = 0; i < chars.length(); i++) if (msg.charAt(0) == chars.charAt(i)) {
			msg = new StringBuilder(msg).deleteCharAt(0).toString();
			String charsraw = Data.config.getString("factoid-charraw");
			String charsby = Data.config.getString("factoid-charby");
			
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
				boolean found = false;
				for (User user : channel.getUsers()) if (user.getNick().equals(target)) {
					found = true;
					break;
				}
				if (!found) return;
			}
			
			for (i = 0; i < charsraw.length(); i++) if (msg.charAt(0) == charsraw.charAt(i)) {
				msg = new StringBuilder(msg).deleteCharAt(0).toString().split(" ")[0].toLowerCase();
				Factoid f = getLatest(channel.getName(),msg,true);
				if (target != null) Shocky.overrideTarget.put(Thread.currentThread(),new ImmutablePair<Command.EType,Command.EType>(Command.EType.Channel,Command.EType.Notice));
				if (f != null && !f.forgotten) Shocky.send(bot,Command.EType.Channel,channel,Shocky.getUser(target),msg+": "+f.rawtext);
				if (target != null) Shocky.overrideTarget.remove(Thread.currentThread());
				return;
			}
			for (i = 0; i < charsby.length(); i++) if (msg.charAt(0) == charsby.charAt(i)) {
				msg = new StringBuilder(msg).deleteCharAt(0).toString().split(" ")[0].toLowerCase();
				Factoid f = getLatest(channel.getName(),msg,true);
				if (target != null) Shocky.overrideTarget.put(Thread.currentThread(),new ImmutablePair<Command.EType,Command.EType>(Command.EType.Channel,Command.EType.Notice));
				if (f != null && !f.forgotten) Shocky.send(bot,Command.EType.Channel,channel,Shocky.getUser(target),msg+", last edited by "+f.author);
				if (target != null) Shocky.overrideTarget.remove(Thread.currentThread());
				return;
			}
			
			if (getLatest(channel.getName(),msg.split(" ")[0]) == null) return;
			
			LinkedList<String> checkRecursive = new LinkedList<String>();
			while (true) {
				String factoid = msg.split(" ")[0].toLowerCase();
				Factoid f = getLatest(channel.getName(),factoid,true);
				if (f != null) {
					if (f.forgotten) return;
					String raw = f.rawtext;
					if (raw.startsWith("<alias>")) {
						msg = parseVariables(bot, channel, sender, msg, raw);
						msg = msg.substring(7);
						if (checkRecursive.contains(msg)) return;
						checkRecursive.add(msg);
						continue;
					} else {
						if (target != null) Shocky.overrideTarget.put(Thread.currentThread(),new ImmutablePair<Command.EType,Command.EType>(Command.EType.Channel,Command.EType.Notice));
						String message = parse(bot,channel,sender,msg,raw);
						if (message != null && message.length() > 0) {
							if (target == null && ping != null) {
								StringBuilder sb = new StringBuilder();
								sb.append(ping);
								sb.append(": ");
								sb.append(message);
								message = sb.toString();
							}
							Shocky.send(bot,Command.EType.Channel,channel,Shocky.getUser(target),message);
						}
						if (target != null) Shocky.overrideTarget.remove(Thread.currentThread());
						break;
					}
				} else return;
			}
		}
	}
	
	public String parse(PircBotX bot, Channel channel, User sender, String message, String raw) {
		message = redirectMessage(channel, message);
		if (raw.startsWith("<noreply>")) {
			return "";
		} else if (raw.startsWith("<php>")) {
			String code = raw.substring(5);
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			
			StringBuilder sb = new StringBuilder("$channel = \""+channel.getName()+"\"; $bot = \""+bot.getNick().replace("\"","\\\"")+"\"; $sender = \""+sender.getNick().replace("\"","\\\"")+"\";");
			sb.append(" $argc = "+(args.length-1)+"; $args = \""+argsImp.replace("\"","\\\"")+"\"; $ioru = \""+(args.length-1 == 0 ? sender.getNick() : argsImp).replace("\"","\\\"")+"\";");
			
			User[] users = channel.getUsers().toArray(new User[channel.getUsers().size()]);
			sb.append(" $randnick = \""+users[new Random().nextInt(users.length)].getNick().replace("\"","\\\"")+"\";");
			
			sb.append("$arg = array(");
			for (int i = 1; i < args.length; i++) {
				if (i != 1) sb.append(",");
				sb.append("\""+args[i].replace("\"","\\\"")+"\"");
			}
			sb.append(");");
			
			code = sb.toString()+" "+code;
			
			HTTPQuery q = new HTTPQuery(Data.config.getString("php-url")+"?"+HTTPQuery.parseArgs("code",code));
			q.connect(true,false);
			
			sb = new StringBuilder();
			for (String line : q.read()) {
				if (sb.length() != 0) sb.append(" | ");
				sb.append(line);
			}
			
			return StringTools.limitLength(sb);
		} else if (raw.startsWith("<py>")) {
			String code = raw.substring(4);
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			
			StringBuilder sb = new StringBuilder("channel = \""+channel.getName()+"\"; bot = \""+bot.getNick().replace("\"","\\\"")+"\"; sender = \""+sender.getNick().replace("\"","\\\"")+"\";");
			sb.append(" argc = "+(args.length-1)+"; args = \""+argsImp.replace("\"","\\\"")+"\"; ioru = \""+(args.length-1 == 0 ? sender.getNick() : argsImp).replace("\"","\\\"")+"\";");
			
			User[] users = channel.getUsers().toArray(new User[channel.getUsers().size()]);
			sb.append(" randnick = \""+users[new Random().nextInt(users.length)].getNick().replace("\"","\\\"")+"\";");
			
			sb.append("arg = [");
			for (int i = 1; i < args.length; i++) {
				if (i != 1) sb.append(",");
				sb.append("\""+args[i].replace("\"","\\\"")+"\"");
			}
			sb.append("];");
			
			code = sb.toString()+" "+code;
			
			HTTPQuery q = new HTTPQuery(Data.config.getString("python-url")+"?"+HTTPQuery.parseArgs("statement",code),"GET");
			q.connect(true,false);
			
			sb = new StringBuilder();
			for (String line : q.read()) {
				if (sb.length() != 0) sb.append(" | ");
				sb.append(line);
			}
			
			return StringTools.limitLength(sb);
		} else if (raw.startsWith("<cmd>")) {
			Command cmd = Command.getCommand(bot,EType.Channel,""+Data.config.getString("main-cmdchar").charAt(0)+raw.substring(5));
			if (cmd != null && !(cmd instanceof CmdFactoid)) {
				raw = parseVariables(bot, channel, sender, message, raw);
				cmd.doCommand(bot,EType.Channel,channel,sender,raw.substring(5));
			}
			return "";
		} else {
			raw = parseVariables(bot, channel, sender, message, raw);
			StringBuilder output = new StringBuilder();
			parseFunctions(raw,output);
			return StringTools.limitLength(output);
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
			
			if (tag.contentEquals("arg") && num1 < args.length && num2 < args.length) {
					if (range) {
						int min = num1 != Integer.MIN_VALUE ? num1 : 1;
						int max = num2 != Integer.MIN_VALUE ? num2 : args.length-1;
						m.appendReplacement(ret, StringTools.implode(args, min, max, " "));
					}
					else if (num1 != Integer.MIN_VALUE)
						m.appendReplacement(ret, args[num1]);
			} else if (tag.contentEquals("req") && num1 != Integer.MIN_VALUE) {
				if (args.length <= num1)
					return String.format("This factoid requires at least %d args",num1);
				m.appendReplacement(ret, "");
			}
			else if (tag.contentEquals("inp"))
				m.appendReplacement(ret, StringTools.implode(args,1," "));
			else if (tag.contentEquals("ioru"))
				m.appendReplacement(ret, args.length > 1 ? StringTools.implode(args,1," ") : sender.getNick());
			else if (tag.contentEquals("bot"))
				m.appendReplacement(ret, bot.getName());
			else if (tag.contentEquals("chan"))
				m.appendReplacement(ret, channel.getName());
			else if (tag.contentEquals("user"))
				m.appendReplacement(ret, sender.getNick());
			else if (tag.contentEquals("rndn")) {
				if (users == null) {
					rnd = new Random();
					users = channel.getUsers().toArray(new User[0]);
				}
				m.appendReplacement(ret, users[rnd.nextInt(users.length)].getNick());
			}
		}
		m.appendTail(ret);
		return ret.toString();
	}
	
	public String redirectMessage(Channel channel, String message) {
		String[] args = message.split(" ");
		if (args.length >= 2 && args.length <= 3 && args[1].contentEquals("^")) {
			Module module = Module.getModule("rollback");
			try {
			if (module != null) {
				String user = null;
				if (args.length == 3) {
					for (User target : channel.getUsers()) {
						if (target.getNick().contentEquals(args[2])) {
							user = args[2];
							break;
						}
					}
				}
				Method method = module.getClass().getDeclaredMethod("getRollbackLines", String.class, String.class, String.class, boolean.class, int.class, int.class);
				int index = user != null ? 1 : 2;
				@SuppressWarnings("unchecked")
				ArrayList<Line> lines = (ArrayList<Line>) method.invoke(module, channel.getName(), user, null, true, index, 0);
				if (lines.size() == index) {
					Line line = lines.get(0);
					if (line instanceof LineMessage) {
						StringBuilder msg = new StringBuilder(args[0]);
						msg.append(' ');
						msg.append(((LineMessage)line).text);
						message = msg.toString();
					}
				}
			}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return message;
	}
	
	public void parseFunctions(String input, StringBuilder output) {
		Matcher m = functionPattern.matcher(input);
		int pos = 0;
		while (m.find(pos)) {
			output.append(input.substring(pos,m.start()));
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
					String inside = input.substring(start, end);
					StringBuilder funcOutput = new StringBuilder();
					parseFunctions(inside,funcOutput);
					output.append(func.result(funcOutput.toString()));
				}
			}
			else {
				output.append(m.group());
				pos = m.end();
			}
		}
		output.append(input.substring(pos));
	}
	
	private Factoid getLatest(String channel, String factoid) {
		return getLatest(channel,factoid,false);
	}
	private Factoid getLatest(String channel, String factoid, boolean withForgotten) {
		QuerySelect q = new QuerySelect(SQL.getTable("factoid"));
		if (channel != null) q.addCriterions(new CriterionStringEquals("channel",channel.toLowerCase()));
		q.addCriterions(new CriterionStringEquals("factoid",factoid.toLowerCase()));
		if (withForgotten) q.addCriterions(new CriterionNumber("forgotten",CriterionNumber.Operation.NotEquals,1));
		q.addOrder("stamp",false);
		q.setLimitCount(1);
		JSONObject j = SQL.select(q);
		return j != null && j.length() != 0 ? Factoid.fromJSONObject(j) : (channel == null ? null : getLatest(null,factoid));
	}
	
	@SuppressWarnings("unused") private static final class Factoid {
		private static Factoid fromJSONObject(JSONObject j) {
			try {
				 return new Factoid(j.getString("channel"),j.getString("author"),j.getString("rawtext"),Long.parseLong(j.getString("stamp"))*1000,j.getString("locked").equals("1"),j.getString("forgotten").equals("1"));
			} catch (Exception e) {e.printStackTrace();}
			return null;
		}
		
		private final String channel, author, rawtext;
		private final long stamp;
		private final boolean locked, forgotten;
		
		private Factoid(String channel, String author, String rawtext, long stamp) {
			this(channel,author,rawtext,stamp,false,false);
		}
		private Factoid(String channel, String author, String rawtext, long stamp, boolean locked, boolean forgotten) {
			this.channel = channel;
			this.author = author;
			this.rawtext = rawtext;
			this.stamp = stamp;
			this.locked = locked;
			this.forgotten = forgotten;
		}
	}
	
	public abstract class Function {
		public abstract String name();
		public abstract String result(String arg);
	}
	public abstract class FunctionMultiArg extends Function {
		public final String result(String arg) {
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
		public abstract String result(String[] arg);
	}
	
	public class CmdRemember extends Command {
		public String command() {return "remember";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			StringBuilder sb = new StringBuilder();
			sb.append("remember/rem/r");
			sb.append("\nremember [.] {name} {raw} - remembers a factoid (use \".\" for local factoids)");
			return sb.toString();
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			if (type != EType.Channel) return false;
			return cmd.equals(command()) || cmd.equals("rem") || cmd.equals("r");
		}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 3 || (args.length == 3 && args[1].equals("."))) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}
			
			String prefix = args[1].equals(".") ? channel.getName() : "";
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			String rem = StringTools.implode(args,args[1].equals(".") ? 3 : 2," ");
			
			Factoid f = getLatest(channel.getName(),name,true);
			if (f != null && f.locked) Shocky.sendNotice(bot,sender,"Factoid is locked"); else {
				QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
				q.add("channel",prefix);
				q.add("factoid",name);
				q.add("author",sender.getNick());
				q.add("rawtext",rem);
				q.add("stamp",new Date().getTime()/1000);
				SQL.insert(q);
				Shocky.sendNotice(bot,sender,"Done.");
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
		public boolean matches(PircBotX bot, EType type, String cmd) {
			if (type != EType.Channel) return false;
			return cmd.equals(command()) || cmd.equals("f");
		}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2 || (args.length == 2 && args[1].equals("."))) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}
			
			String prefix = args[1].equals(".") ? channel.getName() : "";
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			
			Factoid f = getLatest(channel.getName(),name);
			if (f == null) Shocky.sendNotice(bot,sender,"No such factoid"); else {
				if (f.locked) Shocky.sendNotice(bot,sender,"Factoid is locked");
				else {
					QueryInsert q = new QueryInsert(SQL.getTable("factoid"));
					q.add("channel",prefix);
					q.add("factoid",name);
					q.add("author",sender.getNick());
					q.add("rawtext",f.rawtext);
					q.add("stamp",new Date().getTime()/1000);
					q.add("forgotten",1);
					SQL.insert(q);
					Shocky.sendNotice(bot,sender,"Done.");
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
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command()) || cmd.equals("fcmd");
		}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			if (!canUseController(bot,type,sender)) return;
			
			String[] args = message.split(" ");
			if (args.length == 1) {
				ArrayList<CmdFactoid> list = new ArrayList<CmdFactoid>(fcmds);
				StringBuilder sb = new StringBuilder();
				for (CmdFactoid cmd : list) {
					if (sb.length() != 0) sb.append(", ");
					StringBuilder sb2 = new StringBuilder();
					for (String s : cmd.cmds) {
						if (sb2.length() != 0) sb2.append(";");
						sb2.append(s);
					}
					sb.append(sb2.toString()+"->"+cmd.factoid);
				}
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,sb.toString());
				return;
			} else if (args.length == 3 && args[1].equals("remove")) {
				for (int i = 0; i < fcmds.size(); i++) {
					CmdFactoid c = fcmds.get(i);
					for (String s : c.cmds) if (s.equals(args[2])) {
						Command.removeCommands(fcmds.get(i));
						fcmds.remove(i);
						Shocky.sendNotice(bot,sender,"Removed.");
						return;
					}
				}
				return;
			} else if (args.length == 4 && args[1].equals("add")) {
l1:				for (int i = 0; i < fcmds.size(); i++) {
					CmdFactoid c = fcmds.get(i);
					for (String s : c.cmds) if (s.equals(args[2])) {
						Command.removeCommands(fcmds.get(i));
						fcmds.remove(i--);
						break l1;
					}
				}
				CmdFactoid c = new CmdFactoid(args[2],args[3].toLowerCase());
				fcmds.add(c);
				Command.addCommands(c);
				Shocky.sendNotice(bot,sender,"Added.");
				return;
			}
			
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
		}
	}
	public class CmdFactoid extends Command {
		protected ArrayList<String> cmds = new ArrayList<String>();
		protected String factoid;
		
		public CmdFactoid(String command, String factoid) {
			super();
			cmds.addAll(Arrays.asList(command.split(Pattern.quote(";"))));
			this.factoid = factoid;
		}
		
		public String command() {return cmds.get(0);}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {return "";}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			if (type != EType.Channel) return false;
			for (String s : cmds) if (cmd.equals(s)) return true;
			return false;
		}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			onMessage(bot,channel,sender,""+Data.config.getString("factoid-char").charAt(0)+factoid+(args.length > 1 ? " "+StringTools.implode(args,1," ") : ""));
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
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command()) || cmd.equals("fmanage") || cmd.equals("fmng");
		}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			if (!canUseAny(bot,type,channel,sender)) return;
			
			String[] args = message.split(" ");
			if (args.length == 3 || args.length == 4) {
				boolean local = args[2].equals(".");
				if (args.length == 3+(local ? 1 : 0)) {
					String factoid = (local ? args[3] : args[2]).toLowerCase();
					if (args[1].equals("lock")) {
						if ((local && canUseOp(bot,type,channel,sender)) || (!local && canUseController(bot,type,sender))) {
							Factoid f = getLatest(local ? channel.getName() : null,factoid,true);
							if (f == null) {
								Shocky.sendNotice(bot,sender,"No such factoid");
								return;
							}
							if (f.locked) {
								Shocky.sendNotice(bot,sender,"Already locked");
								return;
							}
							
							QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
							q.set("locked",1);
							q.addCriterions(new CriterionStringEquals("channel",local ? channel.getName() : ""),new CriterionStringEquals("factoid",factoid));
							q.addOrder("stamp",false);
							q.setLimitCount(1);
							SQL.update(q);
							Shocky.sendNotice(bot,sender,"Done");
							return;
						}
					} else if (args[1].equals("unlock")) {
						if ((local && canUseOp(bot,type,channel,sender)) || (!local && canUseController(bot,type,sender))) {
							Factoid f = getLatest(local ? channel.getName() : null,factoid,true);
							if (f == null) {
								Shocky.sendNotice(bot,sender,"No such factoid");
								return;
							}
							if (!f.locked) {
								Shocky.sendNotice(bot,sender,"Already unlocked");
								return;
							}
							
							QueryUpdate q = new QueryUpdate(SQL.getTable("factoid"));
							q.set("locked",0);
							q.addCriterions(new CriterionStringEquals("channel",local ? channel.getName() : ""),new CriterionStringEquals("factoid",factoid));
							q.addOrder("stamp",false);
							q.setLimitCount(1);
							SQL.update(q);
							Shocky.sendNotice(bot,sender,"Done");
							return;
						}
					}
				}
			}
			
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
		}
	}
}
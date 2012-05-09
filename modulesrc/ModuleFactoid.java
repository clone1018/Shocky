import java.io.File;
import java.util.*;
import java.util.regex.*;
import org.pircbotx.*;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.*;
import pl.shockah.shocky.*;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleFactoid extends Module {
	protected Command cmdR, cmdF, cmdFCMD, cmdManage;
	private ArrayList<CmdFactoid> fcmds = new ArrayList<CmdFactoid>();
	private Config config = new Config();
	private HashMap<String,Function> functions = new HashMap<String,Function>();
	private static Pattern functionPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\(.*?\\)");
	
	public String name() {return "factoid";}
	public void load() {
		Data.config.setNotExists("factoid-char","?!");
		Data.config.setNotExists("factoid-charraw","+");
		Data.config.setNotExists("factoid-charby","-");
		Data.config.setNotExists("factoid-show",true);
		Data.config.setNotExists("php-url","http://localhost/shocky/shocky.php");
		
		config.load(new File("data","factoid.cfg"));
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
	public void unload() {
		functions.clear();
		Command.removeCommands(fcmds.toArray(new Command[fcmds.size()]));
		fcmds.clear();
		Command.removeCommands(cmdR,cmdF,cmdFCMD,cmdManage);
	}
	
	public void onDataSave() {
		config.save(new File("data","factoid.cfg"));
		
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
		if (Data.getBlacklistNicks().contains(event.getUser().getNick().toLowerCase())) return;
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
			if (args.length >= 2 && args[args.length-2].equals(">")) {
				target = args[args.length-1];
				msg = StringTools.implode(args,0,args.length-3," ");
			} else if (args.length >= 1 && args[args.length-1].equals("<")) {
				target = sender.getNick();
				msg = StringTools.implode(args,0,args.length-2," ");
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
				Config cfg = config; if (cfg.existsConfig(channel.getName())) {
					cfg = config.getConfig(channel.getName());
					if (!cfg.exists("r_"+msg)) cfg = config;
				}
				if (target != null) Shocky.overrideTarget.put(Thread.currentThread(),new Pair<Command.EType,Command.EType>(Command.EType.Channel,Command.EType.Notice));
				if (cfg.exists("r_"+msg)) Shocky.send(bot,Command.EType.Channel,channel,Shocky.getUser(target),msg+": "+cfg.getString("r_"+msg));
				if (target != null) Shocky.overrideTarget.remove(Thread.currentThread());
				return;
			}
			for (i = 0; i < charsby.length(); i++) if (msg.charAt(0) == charsby.charAt(i)) {
				msg = new StringBuilder(msg).deleteCharAt(0).toString().split(" ")[0].toLowerCase();
				Config cfg = config; if (cfg.existsConfig(channel.getName())) {
					cfg = config.getConfig(channel.getName());
					if (!cfg.exists("r_"+msg)) cfg = config;
				}
				if (target != null) Shocky.overrideTarget.put(Thread.currentThread(),new Pair<Command.EType,Command.EType>(Command.EType.Channel,Command.EType.Notice));
				if (cfg.exists("b_"+msg)) Shocky.send(bot,Command.EType.Channel,channel,Shocky.getUser(target),msg+", last edited by "+cfg.getString("b_"+msg));
				if (target != null) Shocky.overrideTarget.remove(Thread.currentThread());
				return;
			}
			
			Config cfg = config; if (cfg.existsConfig(channel.getName())) {
				cfg = config.getConfig(channel.getName());
				if (!cfg.exists("r_"+msg.split(" ")[0].toLowerCase())) cfg = config;
			}
			
			ArrayList<String> checkRecursive = new ArrayList<String>();
			while (true) {
				String factoid = msg.split(" ")[0].toLowerCase();
				if (cfg.exists("r_"+factoid)) {
					String raw = cfg.getString("r_"+factoid);
					if (raw.startsWith("<alias>")) {
						msg = parseVariables(bot, channel, sender, msg, raw);
						msg = msg.substring(7);
						if (checkRecursive.contains(msg)) return;
						checkRecursive.add(msg);
						continue;
					} else {
						if (target != null) Shocky.overrideTarget.put(Thread.currentThread(),new Pair<Command.EType,Command.EType>(Command.EType.Channel,Command.EType.Notice));
						Shocky.send(bot,Command.EType.Channel,channel,Shocky.getUser(target),parse(bot,channel,sender,msg,raw));
						if (target != null) Shocky.overrideTarget.remove(Thread.currentThread());
						break;
					}
				} else return;
			}
		}
	}
	
	public String parse(PircBotX bot, Channel channel, User sender, String message, String raw) {
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
			
			HTTPQuery q = new HTTPQuery(Data.config.getString("php-phpurl")+"?"+HTTPQuery.parseArgs("code",code));
			q.connect(true,false);
			
			sb = new StringBuilder();
			for (String line : q.read()) {
				if (sb.length() != 0) sb.append(" | ");
				sb.append(line);
			}
			
			return sb.toString();
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
			return output.toString();
		}
	}
	
	private static final Pattern argPattern = Pattern.compile("%([A-Za-z\\+]+)([0-9]+)?%");
	public String parseVariables(PircBotX bot, Channel channel, User sender, String message, String raw) {
		message = message.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$");
		String[] args = message.split(" ");
		Matcher m = argPattern.matcher(raw);
		StringBuffer ret = new StringBuffer();
		while (m.find()) {
			String tag = m.group(1);
			if (m.group(2) != null) {
				int i = Integer.parseInt(m.group(2))+1;
				if (tag.contentEquals("arg")) {
					if (i < args.length)
						m.appendReplacement(ret, args[i]);
				} else if (tag.contentEquals("arg+")) {
					if (i < args.length)
						m.appendReplacement(ret, StringTools.implode(args,i," "));
				} else if (tag.contentEquals("req")) {
					if (args.length <= i)
						return String.format("This factoid requires at least %d args",i);
					m.appendReplacement(ret, "");
				}
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
		}
		m.appendTail(ret);
		return ret.toString();
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
			
			String prefix = args[1].equals(".") ? channel.getName()+"->" : "";
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			String rem = StringTools.implode(args,args[1].equals(".") ? 3 : 2," ");
			
			if (config.exists(prefix+"l_"+name)) Shocky.sendNotice(bot,sender,"Factoid is locked"); else {
				config.set(prefix+"r_"+name,rem);
				config.set(prefix+"b_"+name,sender.getNick());
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
			
			String prefix = args[1].equals(".") ? channel.getName()+"->" : "";
			String name = args[args[1].equals(".") ? 2 : 1].toLowerCase();
			
			if (config.exists(prefix+"l_"+name)) Shocky.sendNotice(bot,sender,"Factoid is locked"); else {
				config.remove(prefix+"r_"+name);
				Shocky.sendNotice(bot,sender,config.remove(prefix+"b_"+name) ? "Done." : "No such factoid");
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
							Config cfg = config;
							if (local) cfg = cfg.getConfig(channel.getName());
							if (cfg == null || !cfg.exists("r_"+factoid)) {
								Shocky.sendNotice(bot,sender,"No such factoid");
								return;
							}
							if (cfg.exists("l_"+factoid)) {
								Shocky.sendNotice(bot,sender,"Already locked");
								return;
							}
							
							cfg.set("l_"+factoid,true);
							Shocky.sendNotice(bot,sender,"Done");
							return;
						}
					} else if (args[1].equals("unlock")) {
						if ((local && canUseOp(bot,type,channel,sender)) || (!local && canUseController(bot,type,sender))) {
							Config cfg = config;
							if (local) cfg = cfg.getConfig(channel.getName());
							if (cfg == null || !cfg.exists("r_"+factoid)) {
								Shocky.sendNotice(bot,sender,"No such factoid");
								return;
							}
							if (!cfg.exists("l_"+factoid)) {
								Shocky.sendNotice(bot,sender,"Already unlocked");
								return;
							}
							
							cfg.remove("l_"+factoid);
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
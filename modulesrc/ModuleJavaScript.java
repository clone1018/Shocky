import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.threads.SandboxSecurityManager;
import pl.shockah.shocky.threads.SandboxThreadFactory;
import pl.shockah.shocky.threads.SandboxThreadGroup;

public class ModuleJavaScript extends ScriptModule {
	protected Command cmd;
	protected SecurityManager secure = new SandboxSecurityManager();
	ThreadGroup sandboxGroup = new SandboxThreadGroup("javascript");
	ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);

	public String name() {return "javascript";}
	public String identifier() {return "js";}
	public void onEnable() {
		Command.addCommands(this, cmd = new CmdJavascript());
		Command.addCommand(this, "js",cmd);
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}

	public String parse(final PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
		if (code == null) return "";
		
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			engine.put("argc",(args.length-1));
			engine.put("args",argsImp.replace("\"","\\\""));
			engine.put("ioru",(args.length-1 == 0 ? sender.getNick() : argsImp).replace("\"","\\\""));
			engine.put("arg",Arrays.copyOfRange(args, 1, args.length));
		}
		
		engine.put("channel", channel.getName());
		engine.put("bot", bot.getNick());
		engine.put("sender", sender.getNick());
		
		Sandbox sandbox = new Sandbox(channel.getUsers().toArray(new User[0]));
		engine.put("bot", sandbox);

		JSRunner r = new JSRunner(engine, code);

		SecurityManager sysSecure = System.getSecurityManager();
		System.setSecurityManager(secure);
		String output = null;
		final ExecutorService service = Executors.newSingleThreadExecutor(sandboxFactory);
		try {
		    Future<String> f = service.submit(r);
		    output = f.get(30, TimeUnit.SECONDS);
		}
		catch(TimeoutException e) {
		    output = "Script timed out";
		}
		catch(Exception e) {
		    throw new RuntimeException(e);
		}
		finally {
		    service.shutdown();
		    System.setSecurityManager(sysSecure);
		}
		if (output == null || output.isEmpty())
			return null;
		
		StringBuilder sb = new StringBuilder();
		for(String line : output.split("\n")) {
			if (sb.length() != 0) sb.append(" | ");
			sb.append(line);
		}

		return StringTools.limitLength(sb);
	}

	public class CmdJavascript extends Command {
		public String command() {return "javascript";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "javascript/js\njavascript {code} - runs JavaScript code";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}

			System.out.println(message);
			String output = parse(bot,type,channel,sender,StringTools.implode(args,1," "),null);
			if (output != null && !output.isEmpty())
				callback.append(output);
		}
	}
	
	public class Sandbox {
		private Random rnd = new Random();
		private final User[] users;
		
		public Sandbox(User[] users) {
			this.users = users;
		}
		
		public String randnick() {
			return users[rnd.nextInt(users.length)].getNick();
		}
		
		public String format(String format, Object... args) {
			return String.format(format, args);
		}
		
		public String munge(String in) {
			return Utils.mungeNick(in);
		}
		
		public String odd(String in) {
			return Utils.odd(in);
		}
		
		public String flip(String in) {
			return Utils.flip(in);
		}
		
		public String reverse(String in) {
			return new StringBuilder(in).reverse().toString();
		}
		
		public String toString() {
			return "Yes it is a bot";
		}
	}
	
	public class JSRunner implements Callable<String> {
		
		private final ScriptEngine engine;
		private final String code;
		
		public JSRunner(ScriptEngine e, String c) {
			engine = e;
			code = c;
		}

		@Override
		public String call() throws Exception {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ScriptContext context = engine.getContext();
			context.setWriter(pw);
			context.setErrorWriter(pw);
			
			try {
				Object out = engine.eval(code);
				if (sw.getBuffer().length() != 0)
					return sw.toString();
				if (out != null)
					return out.toString();
			}
			catch(ScriptException ex) {
				return ex.getMessage();
			}
			return null;
		}
		
	}
}
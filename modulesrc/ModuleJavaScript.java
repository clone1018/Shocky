import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import com.sun.script.javascript.RhinoScriptEngine;

import pl.shockah.StringTools;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.threads.SandboxThreadFactory;
import pl.shockah.shocky.threads.SandboxThreadGroup;

public class ModuleJavaScript extends ScriptModule {
	protected Command cmd;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("javascript");
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);

	public String name() {return "javascript";}
	public String identifier() {return "js";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdJavascript());
		Command.addCommand(this, "js",cmd);
		
		try {
			Class.forName("ModuleJavaScript$Sandbox");
			eval(new RhinoScriptEngine(), "0");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}

	public synchronized String parse(Cache cache, final PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		RhinoScriptEngine engine = new RhinoScriptEngine();
		
		if (message != null) {
			String[] args = message.split(" ");
			String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
			engine.put("argc",(args.length-1));
			engine.put("args",argsImp);
			engine.put("ioru",(args.length-1 == 0 ? sender.getNick() : argsImp));
			engine.put("arg",Arrays.copyOfRange(args, 1, args.length));
		}
		Set<User> users;
		if (channel == null)
			users = Collections.emptySet();
		else {
			engine.put("channel", channel.getName());
			users = channel.getUsers();
		}
		engine.put("bot", bot.getNick());
		engine.put("sender", sender.getNick());
		engine.put("host", sender.getHostmask());
		Sandbox sandbox = new Sandbox(users);
		engine.put("bot", sandbox);
		
		return eval(engine, code);
	}
	
	public String eval(ScriptEngine engine, String code) {
		JSRunner r = new JSRunner(engine, code);

		final ExecutorService service = Executors.newSingleThreadExecutor(sandboxFactory);
		TimeUnit unit = TimeUnit.SECONDS;
		String output = null;
		try {
			Future<String> f = service.submit(r);
		    output = f.get(30, unit);
		}
		catch(TimeoutException e) {
		    output = "Script timed out";
		}
		catch(Exception e) {
			e.printStackTrace();
			output = e.getMessage();
		}
		finally {
		    service.shutdown();
		}
		if (output == null || output.isEmpty())
			return null;

		return output;
	}

	public class CmdJavascript extends ScriptCommand {
		public String command() {return "javascript";}
		public String help(Parameters params) {
			return "javascript/js\njavascript {code} - runs JavaScript code";
		}
	}
	
	public class Sandbox {
		private final Random rnd = new Random();
		private final Set<User> users;
		private User[] userArray;
		
		public Sandbox(Set<User> users) {
			this.users = users;
		}
		
		public String randnick() {
			if (users.isEmpty())
				return null;
			if (userArray == null)
				userArray = users.toArray(new User[users.size()]);
			return userArray[rnd.nextInt(userArray.length)].getNick();
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
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

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import com.sun.script.javascript.RhinoScriptEngine;

import pl.shockah.Reflection;
import pl.shockah.StringTools;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.threads.SandboxThreadFactory;
import pl.shockah.shocky.threads.SandboxThreadGroup;
import sun.org.mozilla.javascript.internal.ClassShutter;
import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.ContextFactory;
import sun.org.mozilla.javascript.internal.NativeJavaObject;
import sun.org.mozilla.javascript.internal.Scriptable;
import sun.org.mozilla.javascript.internal.WrapFactory;

public class ModuleJavaScript extends ScriptModule {
	protected Command cmd;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("javascript");
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);

	public String name() {return "javascript";}
	public String identifier() {return "js";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdJavascript());
		Command.addCommand(this, "js",cmd);
		Reflection.setPrivateValue(ContextFactory.class, "hasCustomGlobal", null, false);
		ContextFactory.initGlobal(new SandboxContextFactory());
		try {
			Class.forName("ModuleJavaScript$Sandbox");
			Class.forName("ModuleJavaScript$SandboxNativeJavaObject");
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
	
	public String eval(RhinoScriptEngine engine, String code) {
		CompiledScript cs;
		try {
			cs = engine.compile(code);
		} catch (ScriptException e) {
			return e.getMessage();
		}
		JSRunner r = new JSRunner(cs);

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
		if (output == null)
			output = "";

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
		
		private final CompiledScript cs;
		
		public JSRunner(CompiledScript cs) {
			this.cs = cs;
		}

		@Override
		public String call() throws Exception {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ScriptContext context = cs.getEngine().getContext();
				context.setWriter(pw);
				context.setErrorWriter(pw);
				
				try {
					Object out = cs.eval();
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
	
	public static class SandboxContextFactory extends ContextFactory implements ClassShutter {
		protected Context makeContext() {
			Context c = super.makeContext();
			c.setClassShutter(this);
			c.setWrapFactory(new SandboxWrapFactory());
			return c;
		}

		@Override
		public boolean visibleToScripts(String s) {
			return s.startsWith("adapter");
		}
	}
	
	public static class SandboxNativeJavaObject extends NativeJavaObject {
		private static final long serialVersionUID = -3702781847096599291L;

		public SandboxNativeJavaObject(Scriptable scope, Object javaObject, @SuppressWarnings("rawtypes") Class staticType) {
			super(scope, javaObject, staticType);
		}
	 
		@Override
		public Object get(String name, Scriptable start) {
			if (name.equals("getClass"))
				return NOT_FOUND;
	 
			return super.get(name, start);
		}
	}
	
	public static class SandboxWrapFactory extends WrapFactory {
		@Override
		public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, @SuppressWarnings("rawtypes") Class staticType) {
			return new SandboxNativeJavaObject(scope, javaObject, staticType);
		}
	}
}
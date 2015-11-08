import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
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

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.lookup.Lookup;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.JSType;
import jdk.nashorn.internal.runtime.Property;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.Reflection;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.js.ShockyScriptFunction;
import pl.shockah.shocky.js.ShockyScriptFunction.ShockyProperty;
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.threads.SandboxThreadFactory;
import pl.shockah.shocky.threads.SandboxThreadGroup;

public class ModuleJavaScript extends ScriptModule {
	protected Command cmd;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("javascript");
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);
	private NashornScriptEngineFactory engineFactory;
	
	private final static Field globalField = Reflection.getPrivateField(ScriptObjectMirror.class, "sobj");
	private final MethodHandle MUNGE = Lookup.MH.findStatic(MethodHandles.lookup(), ModuleJavaScript.class, "munge", Lookup.MH.type(String.class, Object.class, Object.class));
	private final MethodHandle ODD = Lookup.MH.findStatic(MethodHandles.lookup(), ModuleJavaScript.class, "odd", Lookup.MH.type(String.class, Object.class, Object.class));
	private final MethodHandle FLIP = Lookup.MH.findStatic(MethodHandles.lookup(), ModuleJavaScript.class, "flip", Lookup.MH.type(String.class, Object.class, Object.class));
	
	public String name() {return "javascript";}
	public String identifier() {return "js";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdJavascript());
		Command.addCommand(this, "js",cmd);
		engineFactory = new NashornScriptEngineFactory();
	}
	public void onDisable() {
		Command.removeCommands(cmd);
		engineFactory = null;
	}
	
	public static String munge(Object self, Object s) {
		Object obj = self;
		if (JSType.nullOrUndefined(obj))
			obj = s;
		return Utils.mungeNick(JSType.toString(obj));
	}
	
	public static String odd(Object self, Object s) {
		Object obj = self;
		if (JSType.nullOrUndefined(obj))
			obj = s;
		return Utils.odd(JSType.toString(obj));
	}
	
	public static String flip(Object self, Object s) {
		Object obj = self;
		if (JSType.nullOrUndefined(obj))
			obj = s;
		return Utils.flip(JSType.toString(obj));
	}

	public synchronized String parse(Cache cache, final PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (engineFactory == null || code == null) return "";
		
		NashornScriptEngine engine = (NashornScriptEngine)engineFactory.getScriptEngine(new String[] {"-strict", "--no-java", "--no-syntax-extensions"});
		
		Global global = null;
		try {
			global = (Global) globalField.get(engine.getBindings(100));
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
			e.printStackTrace();
		}
		
		int flags = Property.NOT_WRITABLE | Property.NOT_ENUMERABLE | Property.NOT_CONFIGURABLE;
		ShockyProperty mungeProperty = new ShockyScriptFunction.ShockyProperty("munge", flags, false, new ShockyScriptFunction("munge", MUNGE, global, null, 0));
		ShockyProperty oddProperty = new ShockyScriptFunction.ShockyProperty("odd", flags, false, new ShockyScriptFunction("odd", ODD, global, null, 0));
		ShockyProperty flipProperty = new ShockyScriptFunction.ShockyProperty("flip", flags, false, new ShockyScriptFunction("flip", FLIP, global, null, 0));
		
		ScriptFunction string = (ScriptFunction)global.string;
		ScriptObject obj = (ScriptObject)string.getPrototype();
		obj.addOwnProperty(mungeProperty.getProperty());
		obj.addOwnProperty(oddProperty.getProperty());
		obj.addOwnProperty(flipProperty.getProperty());
		
		Map<String,Object> params = getParams(bot, channel, sender, message, factoid);
		for (Map.Entry<String,Object> pair : params.entrySet())
			engine.put(pair.getKey(),pair.getValue());
		
		return eval(engine, code);
	}
	
	public String eval(NashornScriptEngine engine, String code) {
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
}
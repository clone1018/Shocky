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
import pl.shockah.shocky.sql.Factoid;
import pl.shockah.shocky.threads.SandboxThreadFactory;
import pl.shockah.shocky.threads.SandboxThreadGroup;

public class ModuleJavaScript extends ScriptModule {
	protected Command cmd;
	private final SandboxThreadGroup sandboxGroup = new SandboxThreadGroup("javascript");
	private final ThreadFactory sandboxFactory = new SandboxThreadFactory(sandboxGroup);
	private NashornScriptEngineFactory engineFactory;
	
	private final Field globalField = Reflection.getPrivateField(ScriptObjectMirror.class, "sobj");
	private final StringWrapper MUNGE = new StringWrapper(Lookup.MH.findStatic(MethodHandles.lookup(), Utils.class, "mungeNick", Lookup.MH.type(String.class, CharSequence.class)));
	private final StringWrapper ODD = new StringWrapper(Lookup.MH.findStatic(MethodHandles.lookup(), Utils.class, "odd", Lookup.MH.type(String.class, CharSequence.class)));
	private final StringWrapper FLIP = new StringWrapper(Lookup.MH.findStatic(MethodHandles.lookup(), Utils.class, "flip", Lookup.MH.type(String.class, CharSequence.class)));
	private final StringWrapper PASTE = new StringWrapper(Lookup.MH.findStatic(MethodHandles.lookup(), Utils.class, "paste", Lookup.MH.type(String.class, CharSequence.class)));
	
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

	public synchronized String parse(Cache cache, final PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (engineFactory == null || code == null) return "";
		
		NashornScriptEngine engine = (NashornScriptEngine)engineFactory.getScriptEngine(new String[] {"-strict", "--no-java", "--no-syntax-extensions"});
		
		Global global = null;
		try {
			global = (Global) globalField.get(engine.getBindings(100));
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
			e.printStackTrace();
		}
		
		ScriptFunction string = (ScriptFunction)global.string;
		ScriptObject obj = (ScriptObject)string.getPrototype();
		
		obj.addOwnProperty(MUNGE.makeProperty("munge", global));
		obj.addOwnProperty(ODD.makeProperty("odd", global));
		obj.addOwnProperty(FLIP.makeProperty("flip", global));
		obj.addOwnProperty(PASTE.makeProperty("paste", global));
		
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
	
	public static class StringWrapper {
		private static final MethodHandle run = Lookup.MH.findVirtual(MethodHandles.lookup(), StringWrapper.class, "run", Lookup.MH.type(Object.class, Object.class, Object.class));
		private final MethodHandle handle = run.bindTo(this);
		private MethodHandle method;
		
		public StringWrapper(MethodHandle method) {
			this.method = method;
		}

		public Object run(Object self, Object s) throws Throwable {
			return method.invoke(JSType.toString(JSType.nullOrUndefined(self) ? s : self));
		}
		
		public Property makeProperty(String name, Global global) {
			return new ShockyScriptFunction(name, handle, global, null, 0).makeProperty(Property.NOT_WRITABLE | Property.NOT_ENUMERABLE | Property.NOT_CONFIGURABLE, false).getProperty();
		}
	}
}
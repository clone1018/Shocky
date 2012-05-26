import java.io.StringWriter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleJavaScript extends Module {
	protected Command cmd;

	public String name() {return "javascript";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdJavascript());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}

	public String parse(final PircBotX bot, EType type, Channel channel, User sender, String code) {
		if (code == null) return "";
		
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		
		engine.put("channel", channel.getName());
		engine.put("bot", bot.getNick());
		engine.put("sender", sender.getNick());
		User[] users = channel.getUsers().toArray(new User[channel.getUsers().size()]);
		engine.put("randnick", users[new Random().nextInt(users.length)].getNick());

		JSRunner r = new JSRunner();
		r.setEngine(engine, code);

		String output = "";
		final ExecutorService service = Executors.newFixedThreadPool(1);
		try {
		    Future<Object> f = service.submit(Executors.callable(r));
		    f.get(30, TimeUnit.SECONDS);
		}
		catch(TimeoutException e) {
		    output = "Script timed out";
		}
		catch(Exception e) {
		    throw new RuntimeException(e);
		}
		finally {
		    service.shutdown();
		}
		if(output == null || output.length() < 1)
			output = r.output;
		
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
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command()) || cmd.equals("js");
		}

		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}

			System.out.println(message);
			Shocky.send(bot,type,channel,sender,parse(bot,type,channel,sender,StringTools.implode(args,1," ")));
		}
	}
	
	public class JSRunner implements Runnable {
		
		protected ScriptEngine engine;
		public String output = "Unknown error occurred";
		protected String code = "";
		public boolean finished = false;
		
		public void setEngine(ScriptEngine e, String c) {
			engine = e;
			code = c;
		}

		@Override
		public void run() {
			StringWriter writer = new StringWriter();
			StringWriter errorWriter = new StringWriter();
			ScriptContext context = engine.getContext();
			context.setWriter(writer);
			context.setErrorWriter(errorWriter);
			engine.setContext(context);
			
			try {
				Object out = engine.eval(code);
				output = writer.toString();
				if(out != null && output.length() < 1)
					output = (String) out;
				String errors = errorWriter.toString();
				if(errors != null && errors.length() > 0)
					output = errors;
			}
			catch(ScriptException ex) {
				output = ex.getMessage();
			}
		}
		
	}
}
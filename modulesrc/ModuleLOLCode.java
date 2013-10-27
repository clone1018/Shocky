import java.io.*;

import org.pircbotx.*;

import com.lolcode.Runtime;
import com.lolcode.parser.*;

import pl.shockah.shocky.*;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;

public class ModuleLOLCode extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "lolcode";}
	public String identifier() {return "lol";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdLOLCode());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		String lines = code.replace(';', '\n').replace('>', '\t');
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		StreamRun run = new StreamRun(stream);
		LolCode lolcode = new LolCode(new StringReader(lines));
		String s;
		try {
			lolcode.CompilationUnit().interpret(run);
			s = stream.toString();
		} catch (ParseException e) {
			s = e.getMessage();
		} catch (TokenMgrError e) {
			s = e.getMessage();
		} catch (Exception e) {
			return "";
		}
		
		return s;
	}
	
	public class CmdLOLCode extends ScriptCommand {
		public String command() {return "lolcode";}
		public String help(Parameters params) {
			return "lolcode\nlolcode {code} - runs LOLCode code";
		}
	}
	
	public class StreamRun extends Runtime {

		private final PrintStream stream;
		
		public StreamRun(OutputStream stream) {
			this.stream = new PrintStream(stream);
		}

		public PrintStream out() {
			return stream;
		}

		public PrintStream err() {
			return stream;
		}
	}
}
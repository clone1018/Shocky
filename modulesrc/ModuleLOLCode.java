import java.io.*;
import java.util.Map;

import org.pircbotx.*;

import com.lolcode.Runtime;
import com.lolcode.parser.*;

import pl.shockah.StringTools;
import pl.shockah.shocky.*;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.sql.Factoid;

public class ModuleLOLCode extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "lolcode";}
	public String identifier() {return "lol";}
	public void onEnable() {
		Command.addCommands(this, cmd = new CmdLOLCode());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Map<Integer,Object> cache, PircBotX bot, EType type, Channel channel, User sender, Factoid factoid, String code, String message) {
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
		} catch (Exception e) {
			return "";
		}
		
		return s;
	}
	
	public class CmdLOLCode extends Command {
		public String command() {return "lolcode";}
		public String help(Parameters params) {
			return "lolcode\nlolcode {code} - runs LOLCode code";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount < 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String output = parse(null,params.bot,params.type,params.channel,params.sender,null,params.input,null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
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
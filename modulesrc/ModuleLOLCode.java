import java.io.*;
import org.pircbotx.*;

import com.lolcode.Runtime;
import com.lolcode.parser.*;

import pl.shockah.StringTools;
import pl.shockah.shocky.*;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleLOLCode extends Module {
	protected Command cmd;
	
	public String name() {return "lolcode";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdLOLCode());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(PircBotX bot, EType type, Channel channel, User sender, String code) {
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
		
		s = StringTools.implode(s.split("[\r\n]+"), " | ");
		
		return StringTools.limitLength(s);
	}
	
	public class CmdLOLCode extends Command {
		public String command() {return "lolcode";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "lolcode/lol\nlolcode {code} - runs LOLCode code";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command()) || cmd.equals("lol");
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}
			
			System.out.println(message);
			String out = parse(bot,type,channel,sender,StringTools.implode(args,1," "));
			if (out != null && out.length()>0)
				callback.append(out);
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
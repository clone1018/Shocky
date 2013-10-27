import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.faabtech.brainfuck.BrainfuckEngine;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.Cache;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;

public class ModuleBrainfuck extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "brainfuck";}
	public String identifier() {return "bf";}
	public void onEnable(File dir) {
		cmd = new CmdBrainfuck();
		Command.addCommands(this, cmd);
		Command.addCommand(this, "bf", cmd);
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message) {
		if (code == null) return "";
		
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream is;
			if (message == null)
				is = new ZeroInputStream();
			else {
				String[] args = message.split(" ");
				String argsImp = StringTools.implode(args,1," "); if (argsImp == null) argsImp = "";
				is = new ByteArrayInputStream(argsImp.getBytes());
			}
			BrainfuckEngine bfe = new BrainfuckEngine(code.length(),os,is);
			bfe.interpret(code);
			return os.toString("UTF-8");
		} catch (Exception e) {e.printStackTrace();}
		return "";
	}
	
	public class CmdBrainfuck extends ScriptCommand {
		public String command() {return "brainfuck";}
		public String help(Parameters params) {
			return "brainfuck/bf\nbrainfuck {code} - runs brainfuck code";
		}
	}
}
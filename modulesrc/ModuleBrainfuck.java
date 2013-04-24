import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.faabtech.brainfuck.BrainfuckEngine;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.ScriptModule;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleBrainfuck extends ScriptModule {
	protected Command cmd;
	
	public String name() {return "brainfuck";}
	public String identifier() {return "bf";}
	public void onEnable() {
		cmd = new CmdBrainfuck();
		Command.addCommands(this, cmd);
		Command.addCommand(this, "bf", cmd);
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(Map<Integer,Object> cache, PircBotX bot, EType type, Channel channel, User sender, String code, String message) {
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
	
	public class CmdBrainfuck extends Command {
		public String command() {return "brainfuck";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "brainfuck/bf\nbrainfuck {code} - runs brainfuck code";
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String output = parse(null,bot,type,channel,sender,StringTools.implode(message,1," "),null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
		}
	}
}
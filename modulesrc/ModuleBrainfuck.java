import java.io.ByteArrayOutputStream;
import org.faabtech.brainfuck.BrainfuckEngine;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.ZeroInputStream;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Command.EType;

public class ModuleBrainfuck extends Module {
	protected Command cmd;
	
	public String name() {return "brainfuck";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdBrainfuck());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public String parse(PircBotX bot, EType type, Channel channel, User sender, String code) {
		if (code == null) return "";
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BrainfuckEngine bfe = new BrainfuckEngine(0,baos,new ZeroInputStream());
			bfe.interpret(code);
			return StringTools.limitLength(baos.toString());
		} catch (Exception e) {e.printStackTrace();}
		return "";
	}
	
	public class CmdBrainfuck extends Command {
		public String command() {return "brainfuck";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "brainfuck/bf\nbrainfuck {code} - runs brainfuck code";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command()) || cmd.equals("bf");
		}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length < 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			callback.append(parse(bot,type,channel,sender,StringTools.implode(args,1," ")));
		}
	}
}
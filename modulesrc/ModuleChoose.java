import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleChoose extends Module {
	protected Command cmd;
	
	public String name() {return "choose";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdChoose());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdChoose extends Command {
		public String command() {return "choose";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "choose {1} {2} ... {n} - makes a decision";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length == 1) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String txt = StringTools.implode(args,1," ");
			String[] choices = txt.split(";");
			if (choices.length == 1) choices = txt.split(",");
			if (choices.length == 1) choices = txt.split(" ");
			if (choices.length == 1) {
				callback.append("Definitely not "+choices[0]);
				return;
			}
			callback.append(choices[new Random().nextInt(choices.length)].trim());
		}
	}
}
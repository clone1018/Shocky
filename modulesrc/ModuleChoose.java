import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;

public class ModuleChoose extends Module {
	protected Command cmd;
	
	public String name() {return "choose";}
	public void load() {
		Command.addCommands(cmd = new CmdChoose());
	}
	public void unload() {
		Command.removeCommands(cmd);
	}
	
	public class CmdChoose extends Command {
		public String command() {return "choose";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "choose {1} {2} ... {n} - makes a decision";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length == 1) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
				return;
			}
			
			String txt = StringTools.implode(args,1," ");
			String[] choices = txt.split(";");
			if (choices.length == 1) choices = txt.split(",");
			if (choices.length == 1) choices = txt.split(" ");
			if (choices.length == 1) {
				Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,(type == EType.Channel ? sender.getNick()+": " : "")+"Definitely not "+choices[0]);
				return;
			}
			Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,(type == EType.Channel ? sender.getNick()+": " : "")+choices[new Random().nextInt(choices.length)].trim());
		}
	}
}
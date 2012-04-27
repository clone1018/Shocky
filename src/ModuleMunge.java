import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;

public class ModuleMunge extends Module {
	protected Command cmd;
	
	public String name() {return "munge";}
	public void load() {
		Command.addCommands(cmd = new CmdMunge());
	}
	public void unload() {
		Command.removeCommands(cmd);
	}
	
	public class CmdMunge extends Command {
		public String command() {return "munge";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "no help for you";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length >= 2) {
				Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,Utils.mungeAllNicks(channel,StringTools.implode(args,1," ")));
			} else Shocky.sendNotice(bot,sender,help(bot,type,channel,sender));
		}
	}
}
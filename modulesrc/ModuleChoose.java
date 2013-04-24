import java.util.Random;
import java.util.StringTokenizer;

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
		Command.addCommands(this, cmd = new CmdChoose());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdChoose extends Command {
		public String command() {return "choose";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "choose {1} {2} ... {n} - makes a decision";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length == 1) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String txt = StringTools.implode(message,1," ");
			String[] choices = tokenSplit(txt,";");
			if (choices.length == 1) choices = tokenSplit(txt,",");
			if (choices.length == 1) choices = tokenSplit(txt," ");
			if (choices.length == 1) {
				callback.append("Definitely not "+choices[0]);
				return;
			}
			callback.append(choices[new Random().nextInt(choices.length)].trim());
		}
		
		private String[] tokenSplit(String str, String delim) {
			StringTokenizer strtok = new StringTokenizer(str,delim);
			int count = strtok.countTokens();
			if (count == 1)
				return new String[] {str};
			String[] result = new String[count];
			int i = 0;
			while (strtok.hasMoreTokens())
				result[i++] = strtok.nextToken();
			return result;
		}
	}
}
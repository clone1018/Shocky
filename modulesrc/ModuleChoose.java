import java.io.File;
import java.util.Random;
import java.util.StringTokenizer;

import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleChoose extends Module {
	protected Command cmd;
	
	public String name() {return "choose";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdChoose());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdChoose extends Command {
		public String command() {return "choose";}
		public String help(Parameters params) {
			return "choose {1} {2} ... {n} - makes a decision";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount==0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String[] choices = tokenSplit(params.input,";");
			if (choices.length == 1) choices = tokenSplit(params.input,",");
			if (choices.length == 1) choices = tokenSplit(params.input," ");
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
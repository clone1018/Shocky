import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleSaspiron extends Module {
	protected Command cmd;

	public String name() {
		return "saspiron";
	}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdSaspiron());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}

	public class CmdSaspiron extends Command {
		public String command() {
			return "saspiron";
		}
		public String help(Parameters params) {
			return "saspiron - if you need to ask for help then you should be using it";
		}
		
		protected final String[] three = new String[]{"was","is","can","but","it"};
		protected final String[] four = new String[]{"awesome","fast","random","skrewed","brocken","misslead","fucked up"};
		public void doCommand(Parameters params, CommandCallback callback) {
			String[] args = params.input.split(" ");
			ArrayList<String> TheWords = new ArrayList<String>();
			ArrayList<String> TheOtherWords = new ArrayList<String>();
			Random rnd = new Random();
			for (int i = 0; i < args.length; i++) {
				if (args[i].length() < 4) TheWords.add(args[i]);
				else TheOtherWords.add(args[i]);
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				if (sb.length() == 0) sb.append(" ");
				if (args[i].length() < 4) {
					if (rnd.nextInt(10) == 0) sb.append(three[rnd.nextInt(three.length)]).append(" ");
					else sb.append(TheWords.remove(rnd.nextInt(TheWords.size()))).append(" ");
				} else {
					if (rnd.nextInt(30) == 0) sb.append(four[rnd.nextInt(four.length)]).append(" ");
					sb.append(TheOtherWords.remove(rnd.nextInt(TheOtherWords.size()))).append(" ");
				}
			}
			
			callback.append(sb);
		}
	}
}
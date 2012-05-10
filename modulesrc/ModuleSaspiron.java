import java.util.ArrayList;
import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;

public class ModuleSaspiron extends Module {
	protected Command cmd;

	public String name() {
		return "saspiron";
	}
	public void onEnable() {
		Command.addCommands(cmd = new CmdSaspiron());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}

	public class CmdSaspiron extends Command {
		public String command() {
			return "saspiron";
		}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "saspiron - if you need to ask for help then you should be using it";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {
			return cmd.equals(command());
		}
		
		protected final String[] three = new String[]{"was","is","can","but","it"};
		protected final String[] four = new String[]{"awesome","fast","random","skrewed","brocken","misslead","fucked up"};
		public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			ArrayList<String> TheWords = new ArrayList<String>();
			ArrayList<String> TheOtherWords = new ArrayList<String>();
			Random rnd = new Random();
			for (int i = 1; i < args.length; i++) {
				if (args[i].length() < 4) TheWords.add(args[i]);
				else TheOtherWords.add(args[i]);
			}
			String msg = "";
			for (int i = 1; i < args.length; i++) {
				if (!msg.isEmpty()) msg += " ";
				if (args[i].length() < 4) {
					if (rnd.nextInt(10) == 0) msg += three[rnd.nextInt(three.length)];
					else msg += TheWords.remove(rnd.nextInt(TheWords.size()));
				} else {
					if (rnd.nextInt(30) == 0) msg += four[rnd.nextInt(three.length)];
					msg += TheOtherWords.remove(rnd.nextInt(TheOtherWords.size()));
				}
			}
			msg += ". this message is aproved by Saspiron";
			Shocky.send(bot,type,EType.Channel,EType.Notice,EType.Notice,EType.Console,channel,sender,msg);
		}
	}
}
import java.util.Date;
import java.util.Random;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleProbability extends Module {
	protected Command cmd;
	protected final String[] insults = new String[]{
		"a troll","an idiot","a dumbass","retarded","7 years old","using Mibbit","lost","in the wrong channel","new here","going to get kicked",
		"dumb","confused","stupid","trolling","new to IRC","new to modding"
	};
	
	public String name() {return "probability";}
	public void onEnable() {
		Command.addCommands(cmd = new CmdProbability());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdProbability extends Command {
		public String command() {return "probability";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "probability {user} - check what the sensor is saying";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length != 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			User u = Shocky.getUser(bot,args[1]);
			if (u == null) {
				callback.type = EType.Notice;
				callback.append("No such user");
				return;
			}
			
			Random rnd = getRandom(u);
			String msg = "I am detecting a "+getRandomProbability(rnd)+"% probability that "+u.getNick()+" is "+getRandomAdjective(rnd);
			callback.append(msg);
		}
		
		public Random getRandom(User user) {
			boolean loggedIn = user.getLogin() != null && !user.getLogin().isEmpty();
			return new Random((new Date().getTime()/(1000*60*60))+(loggedIn ? user.getLogin() : user.getHostmask()).hashCode());
		}
		
		public String getRandomAdjective(Random rnd) {
			return insults[rnd.nextInt(insults.length)];
		}
		public int getRandomProbability(Random rnd) {
			return rnd.nextInt(21)*5;
		}
	}
}
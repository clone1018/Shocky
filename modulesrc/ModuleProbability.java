import java.io.File;
import java.util.Random;
import org.pircbotx.User;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleProbability extends Module {
	protected Command cmd;
	protected final String[] insults = new String[]{
		"a troll","an idiot","a dumbass","retarded","7 years old","using Mibbit","lost","in the wrong channel","new here","going to get kicked",
		"dumb","confused","stupid","trolling","new to IRC","new to modding"
	};
	
	public String name() {return "probability";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmd = new CmdProbability());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdProbability extends Command {
		public String command() {return "probability";}
		public String help(Parameters params) {
			return "probability {user} - check what the sensor is saying";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount != 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String username = params.nextParam();
			User user = params.bot.getUser(username);
			if (user == null) {
				callback.type = EType.Notice;
				callback.append("No such user");
				return;
			}
			
			Random rnd = getRandom(user);
			callback.append("I am detecting a ");
			callback.append(getRandomProbability(rnd));
			callback.append("% probability that ");
			callback.append(user.getNick());
			callback.append(" is ");
			callback.append(getRandomAdjective(rnd));
		}
		
		public Random getRandom(User user) {
			boolean loggedIn = user.getLogin() != null && !user.getLogin().isEmpty();
			return new Random((System.currentTimeMillis()/(1000*60*60))+(loggedIn ? user.getLogin() : user.getHostmask()).hashCode());
		}
		
		public String getRandomAdjective(Random rnd) {
			return insults[rnd.nextInt(insults.length)];
		}
		public int getRandomProbability(Random rnd) {
			return rnd.nextInt(21)*5;
		}
	}
}
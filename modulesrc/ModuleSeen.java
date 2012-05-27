import java.io.File;
import java.util.Date;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.Config;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ModuleSeen extends Module {
	private Config config;
	protected Command cmd;
	
	public String name() {return "seen";}
	public boolean isListener() {return true;}
	public void onEnable() {
		(config = new Config()).load(new File("data","seen.cfg"));
		Command.addCommands(cmd = new CmdSeen());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	public void onDataSave() {
		config.save(new File("data","seen.cfg"));
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		config.set("t_"+event.getUser().getNick().toLowerCase(),new Date().getTime());
		config.set("c_"+event.getUser().getNick().toLowerCase(),event.getChannel().getName());
		config.set("m_"+event.getUser().getNick().toLowerCase(),event.getMessage());
	}
	
	public class CmdSeen extends Command {
		public String command() {return "seen";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "seen {nick} - tells when the user was last active";
		}
		public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
		
		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			String[] args = message.split(" ");
			if (args.length != 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			}
			
			String low = args[1].toLowerCase();
			if (low.equals(bot.getNick().toLowerCase())) callback.append("Are your glasses not strong enough?");
			else if (low.equals(sender.getNick().toLowerCase())) callback.append("Schizophrenia, eh?");
			else {
				if (config.exists("t_"+low)) {
					callback.append(args[1]+" was last active "+Utils.timeAgo(new Date(config.getLong("t_"+low)))+" in "+config.getString("c_"+low)+" and said: "+config.getString("m_"+low));
				} else callback.append("I've never seen "+args[1]);
			}
		}
	}
}
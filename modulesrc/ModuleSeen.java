import java.io.File;
import java.util.Date;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import pl.shockah.Config;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.prototypes.ISeen;

public class ModuleSeen extends Module implements ISeen {
	private Config config;
	protected Command cmd;
	
	public String name() {return "seen";}
	public boolean isListener() {return true;}
	public void onEnable() {
		(config = new Config()).load(new File("data","seen.cfg"));
		Command.addCommands(this, cmd = new CmdSeen());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	public void onDataSave() {
		config.save(new File("data","seen.cfg"));
	}
	
	public void onMessage(MessageEvent<PircBotX> event) {
		String key = event.getUser().getNick().toLowerCase();
		config.set("t_"+key,System.currentTimeMillis());
		config.set("c_"+key,event.getChannel().getName());
		config.set("m_"+key,event.getMessage());
	}
	
	public class CmdSeen extends Command {
		public String command() {return "seen";}
		public String help(Parameters params) {
			return "seen {nick} - tells when the user was last active";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount != 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String username = params.tokens.nextToken();
			String low = username.toLowerCase();
			if (low.equals(params.bot.getNick().toLowerCase())) callback.append("Are your glasses not strong enough?");
			else if (low.equals(params.sender.getNick().toLowerCase())) callback.append("Schizophrenia, eh?");
			else {
				if (config.exists("t_"+low)) {
					callback.append(username).
					append(" was last active ").
					append(Utils.timeAgo(new Date(config.getLong("t_"+low)))).
					append(" in ").
					append(config.getString("c_"+low)).
					append(" and said: ").
					append(config.getString("m_"+low));
				} else callback.append("I've never seen ").append(username);
			}
		}
	}

	@Override
	public boolean hasSeen(String nick) {
		String low = nick.trim().toLowerCase();
		return config.exists("t_"+low);
	}
}
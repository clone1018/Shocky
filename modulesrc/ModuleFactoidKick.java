import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.Config;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.prototypes.IFactoid;

public class ModuleFactoidKick extends Module {
	protected Command cmd;
	
	public String name() {return "factoidkick";}
	public void onEnable() {
		Command.addCommands(this, cmd = new CmdFactoidKick());
	}
	public void onDisable() {
		Command.removeCommands(cmd);
	}
	
	public class CmdFactoidKick extends Command {
		public String command() {return "kick";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "kick {user} [factoid/message] - kicks the user";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (type != EType.Channel) return;
			if (!canUseOp(bot,type,channel,sender)) return;
			
			String[] spl = message.split(" ");
			if (spl.length == 1) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			} else {
				String kick = spl[1];
				if (spl.length == 2) {
					bot.kick(channel,bot.getUser(kick),kick);
				} else {
					Config cfg = Data.forChannel(channel);
					if (!cfg.exists("factoid-char")) cfg = Data.config;
					
					if (cfg.exists("factoid-char")) {
						String chars = cfg.getString("factoid-char");
						for (int i = 0; i < chars.length(); i++) if (chars.charAt(i) == spl[2].charAt(0)) {
							String factoid = StringTools.implode(spl,2," ").substring(1);
							IFactoid module = (IFactoid)Module.getModule("factoid");
							factoid = module.runFactoid(bot,channel,sender,factoid);
							bot.kick(channel,bot.getUser(kick),factoid == null || factoid.isEmpty() ? kick : factoid);
							return;
						}
					}
					bot.kick(channel,bot.getUser(kick),StringTools.implode(spl,2," "));
				}
			}
		}
	}
}
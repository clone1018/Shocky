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

public class ModuleOP extends Module {
	protected CmdKick cmdKick;
	protected CmdBan cmdBan;
	protected Command cmdKickban, cmdQuiet, cmdOp, cmdDeop;
	
	public String name() {return "op";}
	public void onEnable() {
		Command.addCommands(this,cmdKick = new CmdKick(),cmdBan = new CmdBan(),cmdKickban = new CmdKickban(),cmdQuiet = new CmdQuiet(),cmdOp = new CmdOp(),cmdDeop = new CmdDeop());
	}
	public void onDisable() {
		Command.removeCommands(cmdKick,cmdBan,cmdKickban,cmdQuiet,cmdOp,cmdDeop);
	}
	
	public class CmdKick extends Command {
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
							factoid = module.runFactoid(null,bot,channel,sender,factoid);
							bot.kick(channel,bot.getUser(kick),factoid == null || factoid.isEmpty() ? kick : factoid);
							return;
						}
					}
					bot.kick(channel,bot.getUser(kick),StringTools.implode(spl,2," "));
				}
			}
		}
	}
	public class CmdBan extends Command {
		public String command() {return "ban";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "ban {user} - bans the user";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (type != EType.Channel) return;
			if (!canUseOp(bot,type,channel,sender)) return;
			
			String[] spl = message.split(" ");
			if (spl.length != 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			} else bot.ban(channel,bot.getUser(spl[1]).getHostmask());
		}
	}
	public class CmdKickban extends Command {
		public String command() {return "kickban";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "kickban {user} [factoid/message] - kickbans the user";
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
				spl[0] = "ban"; message = StringTools.implode(spl,0,1," ");
				cmdBan.doCommand(bot,type,callback,channel,sender,message);
				spl[0] = "kick"; message = StringTools.implode(spl," ");
				cmdKick.doCommand(bot,type,callback,channel,sender,message);
			}
		}
	}
	public class CmdQuiet extends Command {
		public String command() {return "quiet";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "quiet {user} - quiets the user";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (type != EType.Channel) return;
			if (!canUseOp(bot,type,channel,sender)) return;
			
			String[] spl = message.split(" ");
			if (spl.length != 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			} else bot.sendRawLine("MODE "+channel.getName()+" +q "+bot.getUser(spl[1]).getHostmask());
		}
	}
	public class CmdOp extends Command {
		public String command() {return "op";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "op {user} - ops the user";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (type != EType.Channel) return;
			if (!canUseOp(bot,type,channel,sender)) return;
			
			String[] spl = message.split(" ");
			if (spl.length != 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			} else bot.sendRawLine("MODE "+channel.getName()+" +o "+spl[1]);
		}
	}
	public class CmdDeop extends Command {
		public String command() {return "deop";}
		public String help(PircBotX bot, EType type, Channel channel, User sender) {
			return "deop {user} - deops the user";
		}

		public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
			if (type != EType.Channel) return;
			if (!canUseOp(bot,type,channel,sender)) return;
			
			String[] spl = message.split(" ");
			if (spl.length != 2) {
				callback.type = EType.Notice;
				callback.append(help(bot,type,channel,sender));
				return;
			} else bot.sendRawLine("MODE "+channel.getName()+" -o "+spl[1]);
		}
	}
}
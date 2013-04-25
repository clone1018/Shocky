import org.pircbotx.User;
import pl.shockah.Config;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
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
		public String help(Parameters params) {
			return "kick {user} [factoid/message] - kicks the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			} else {
				String kick = params.tokens.nextToken();
				if (params.tokenCount == 1) {
					params.bot.kick(params.channel,params.bot.getUser(kick),kick);
				} else if (params.tokenCount >= 2) {
					String message = params.getParams(0);
					Config cfg = Data.forChannel(params.channel);
					if (!cfg.exists("factoid-char"))
						cfg = Data.config;
					
					if (cfg.exists("factoid-char")) {
						String chars = cfg.getString("factoid-char");
						for (int i = 0; i < chars.length(); i++) if (chars.charAt(i) == message.charAt(0)) {
							String factoid = message.substring(1);
							IFactoid module = (IFactoid)Module.getModule("factoid");
							factoid = module.runFactoid(null,params.bot,params.channel,params.sender,factoid);
							params.bot.kick(params.channel,params.bot.getUser(kick),factoid == null || factoid.isEmpty() ? kick : factoid);
							return;
						}
					}
					params.bot.kick(params.channel,params.bot.getUser(kick),message);
				}
			}
		}
	}
	public class CmdBan extends Command {
		public String command() {return "ban";}
		public String help(Parameters params) {
			return "ban {user} - bans the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount != 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			} else {
				String username = params.tokens.nextToken();
				User user = params.bot.getUser(username);
				if (user != null)
				params.bot.ban(params.channel,user.getHostmask());
			}
		}
	}
	public class CmdKickban extends Command {
		public String command() {return "kickban";}
		public String help(Parameters params) {
			return "kickban {user} [factoid/message] - kickbans the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			} else {
				cmdBan.doCommand(params, callback);
				cmdKick.doCommand(params, callback);
			}
		}
	}
	public class CmdQuiet extends Command {
		public String command() {return "quiet";}
		public String help(Parameters params) {
			return "quiet {user} - quiets the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount != 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			} else {
				String username = params.tokens.nextToken();
				User user = params.bot.getUser(username);
				if (user != null)
					params.bot.sendRawLine("MODE "+params.channel.getName()+" +q "+user.getHostmask());
			}
		}
	}
	public class CmdOp extends Command {
		public String command() {return "op";}
		public String help(Parameters params) {
			return "op {user} - ops the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount != 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			} else {
				String username = params.tokens.nextToken();
				User user = params.bot.getUser(username);
				if (user != null)
					params.bot.op(params.channel, user);
			}
		}
	}
	public class CmdDeop extends Command {
		public String command() {return "deop";}
		public String help(Parameters params) {
			return "deop {user} - deops the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount != 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			} else {
				String username = params.tokens.nextToken();
				User user = params.bot.getUser(username);
				if (user != null)
					params.bot.deOp(params.channel, user);
			}
		}
	}
}
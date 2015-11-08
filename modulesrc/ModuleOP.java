import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.pircbotx.User;

import pl.shockah.Config;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.IFactoid;

public class ModuleOP extends Module implements Comparator<Pair<String,String>> {
	protected CmdKick cmdKick;
	protected CmdBan cmdBan;
	protected Command cmdKickban, cmdQuiet, cmdUnquiet, cmdOp, cmdDeop, cmdVoice, cmdDevoice, cmdVoiceDeop;
	
	public String name() {return "op";}
	public void onEnable(File dir) {
		Command.addCommands(this,
				cmdKick = new CmdKick(),
				cmdBan = new CmdBan(),cmdKickban = new CmdKickban(),
				cmdQuiet = new CmdQuiet(),cmdUnquiet = new CmdUnquiet(),
				cmdOp = new CmdOp(),cmdDeop = new CmdDeop(),
				cmdVoice = new CmdVoice(),cmdDevoice = new CmdDevoice(),
				cmdVoiceDeop = new CmdVoiceDeop()
		);
	}
	public void onDisable() {
		Command.removeCommands(cmdKick,cmdBan,cmdKickban,cmdQuiet,cmdOp,cmdDeop);
	}
	
	public void setModes(Parameters params, List<Pair<String,String>> users) {
		if (users.isEmpty())
			return;
		if (users.size() > 1)
			Collections.sort(users, this);
		if (users.size() > 4) {
			for (int i = 0; i < users.size(); i+=4) {
				int len = users.size()-i;
				if (len > 4)
					len = 4;
				setModes(params,users.subList(i, i+len));
			}
		} else {
			StringBuilder modesb = new StringBuilder(8);
			StringBuilder usersb = new StringBuilder();
			Character mode = null;
			for (int i = 0; i < users.size(); ++i) {
				Pair<String,String> pair = users.get(i);
				char c = pair.getLeft().charAt(0);
				if (mode == null || mode != c) {
					mode = c;
					modesb.append(c);
				}
				modesb.append(pair.getLeft().charAt(1));
				usersb.append(' ').append(pair.getRight());
			}
			String channel = params.channel.getName();
			StringBuilder sb = new StringBuilder(6+channel.length()+modesb.length()+usersb.length());
			sb.append("MODE ").append(channel).append(' ').append(modesb).append(usersb);
			params.bot.sendRawLine(sb.toString());
		}
	}
	
	public List<Pair<String,String>> prepSingleMode(Parameters params, String mode, boolean useMask) {
		String username = params.nextParam();
		User user = params.bot.getUser(username);
		return Collections.singletonList(Pair.of(mode, useMask ? user.getHostmask() : user.getNick()));
	}
	
	public List<Pair<String,String>> prepModes(Parameters params, String mode, boolean useMask) {
		List<Pair<String,String>> list = new ArrayList<Pair<String,String>>();
		while (params.hasMoreParams()) {
			String username = params.nextParam();
			User user = params.bot.getUser(username);
			list.add(Pair.of(mode, useMask ? user.getHostmask() : user.getNick()));
		}
		return list;
	}
	
	@Override
	public int compare(Pair<String, String> o1, Pair<String, String> o2) {
		char char1 = o1.getLeft().charAt(0);
		char char2 = o2.getLeft().charAt(0);
		if (char1 < char2)
			return -1;
		else if (char1 > char2)
			return 1;
		int i = o1.getRight().compareToIgnoreCase(o2.getRight());
		if (i != 0)
			return i;
		char1 = o1.getLeft().charAt(1);
		char2 = o2.getLeft().charAt(1);
		if (char1 < char2)
			return -1;
		else if (char1 > char2)
			return 1;
		return 0;
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
				String kick = params.nextParam();
				if (params.tokenCount == 1) {
					params.bot.kick(params.channel,params.bot.getUser(kick),StringTools.deleteWhitespace(kick));
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
							try {
								factoid = module.runFactoid(null,params.bot,params.channel,params.sender,factoid);
							} catch (Exception e) {
								e.printStackTrace();
							}
							params.bot.kick(params.channel,params.bot.getUser(kick),StringTools.deleteWhitespace(factoid == null || factoid.isEmpty() ? kick : factoid));
							return;
						}
					}
					params.bot.kick(params.channel,params.bot.getUser(kick),StringTools.deleteWhitespace(message));
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
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"+b",true));
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
			}
			cmdKick.doCommand(params, callback);
			params.resetParams();
			setModes(params,prepSingleMode(params,"+b",true));
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
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"+q",true));
		}
	}
	public class CmdUnquiet extends Command {
		public String command() {return "unquiet";}
		public String help(Parameters params) {
			return "unquiet {user} - unquiets the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"-q",true));
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
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"+o",false));
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
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"-o",false));
		}
	}
	public class CmdVoice extends Command {
		public String command() {return "voice";}
		public String help(Parameters params) {
			return "voice {user} - voices the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"+v",false));
		}
	}
	public class CmdDevoice extends Command {
		public String command() {return "devoice";}
		public String help(Parameters params) {
			return "devoice {user} - devoices the user";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			setModes(params,prepModes(params,"-v",false));
		}
	}
	public class CmdVoiceDeop extends Command {
		public String command() {return "vdo";}
		public String help(Parameters params) {
			return "vdo {user} - deops the user and then voices";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkOp();
			
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			List<Pair<String,String>> list = new ArrayList<Pair<String,String>>();
			list.addAll(prepModes(params,"-o",false));
			params.resetParams();
			list.addAll(prepModes(params,"+v",false));
			setModes(params,list);
		}
	}
}
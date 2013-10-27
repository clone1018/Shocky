import java.io.File;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;

import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleSay extends Module {
	protected Command cmdSay, cmdAction;
	
	public String name() {return "say";}
	public void onEnable(File dir) {
		Command.addCommands(this, cmdSay = new CmdSay(),cmdAction = new CmdAction());
	}
	public void onDisable() {
		Command.removeCommands(cmdSay,cmdAction);
	}
	
	public void doCommand(Command cmd, Parameters params, CommandCallback callback) {
		
	}
	
	public class CmdSay extends Command {
		public String command() {return "say";}
		public String help(Parameters params) {
			return "[r:controller] say [channel] {phrase} - makes the bot say {phrase}";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			params.checkController();
			
			if (params.hasMoreParams()) {
				String c = params.nextParam();
				if (!c.startsWith("#"))
					callback.append(params.input);
				else {
					Channel channel = Shocky.getChannel(c);
					if (channel == null)
						callback.append(params.input);
					else {
						PircBotX bot = channel.getBot();
						Shocky.sendChannel(bot, channel, params.getParams(0));
					}
				}
			}
			else {
				callback.type = EType.Notice;
				callback.append(help(params));
			}
		}
	}
	public class CmdAction extends Command {
		public String command() {return "action";}
		public String help(Parameters params) {
			return "[r:controller] action [channel] {action} - /me {action}";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkController();
			
			if (params.hasMoreParams()) {
				String c = params.nextParam();
				if (!c.startsWith("#"))
					Shocky.sendAction(params.bot, params.channel, params.input);
				else {
					Channel chan = Shocky.getChannel(c);
					if (chan == null)
						Shocky.sendAction(params.bot, params.channel, params.input);
					else {
						PircBotX bot = chan.getBot();
						Shocky.sendAction(bot, chan, params.getParams(0));
					}
				}
			}
			else {
				callback.type = EType.Notice;
				callback.append(help(params));
			}
		}
	}
}
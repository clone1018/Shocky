import java.io.File;

import org.pircbotx.Channel;

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
	
	private boolean send(Command.EType type, Parameters params, CommandCallback callback) {
		if (!params.hasMoreParams())
			return false;
		String chan = params.nextParam();
		String msg = params.input;
		Channel channel = params.channel;
		if (!chan.startsWith("#")) {
			if (params.type != Command.EType.Channel)
			{
				callback.append(msg);
				return true;
			}
		}
		else {
			channel = Shocky.getChannel(chan);
			msg = params.getParams(0);
		}
		if (channel == null || !channel.isOp(params.sender))
			return false;
		Shocky.send(channel.getBot(), type, channel, null, msg);
		return true;
	}
	
	public class CmdSay extends Command {
		public String command() {return "say";}
		public String help(Parameters params) {
			return "[r:op] say [channel] {phrase} - makes the bot say {phrase}";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (!send(EType.Channel, params, callback)) {
				callback.type = EType.Notice;
				callback.append(help(params));
			}
		}
	}
	public class CmdAction extends Command {
		public String command() {return "action";}
		public String help(Parameters params) {
			return "[r:op] action [channel] {action} - /me {action}";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (!send(EType.Action, params, callback)) {
				callback.type = EType.Notice;
				callback.append(help(params));
			}
		}
	}
}
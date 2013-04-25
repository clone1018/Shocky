import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleSay extends Module {
	protected Command cmdSay, cmdAction;
	
	public String name() {return "say";}
	public void onEnable() {
		Command.addCommands(this, cmdSay = new CmdSay(),cmdAction = new CmdAction());
	}
	public void onDisable() {
		Command.removeCommands(cmdSay,cmdAction);
	}
	
	public class CmdSay extends Command {
		public String command() {return "say";}
		public String help(Parameters params) {
			return "[r:controller] say {phrase} - makes the bot say {phrase}";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			params.checkController();
			
			if (!params.input.isEmpty())
				callback.append(params.input);
			else {
				callback.type = EType.Notice;
				callback.append(help(params));
			}
		}
	}
	public class CmdAction extends Command {
		public String command() {return "action";}
		public String help(Parameters params) {
			return "[r:controller] action {action} - /me {action}";
		}
		
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.type != EType.Channel) return;
			params.checkController();
			callback.type = EType.Notice;
			
			if (!params.input.isEmpty())
				params.bot.sendAction(params.channel,params.input);
			else
				callback.append(help(params));
		}
	}
}
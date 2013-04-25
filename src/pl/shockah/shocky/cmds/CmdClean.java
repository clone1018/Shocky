package pl.shockah.shocky.cmds;

import pl.shockah.shocky.Module;

public class CmdClean extends Command {
	public String command() {return "clean";}
	public String help(Parameters params) {
		return "[r:controller] clean - cleans up certain modules";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		callback.type = EType.Notice;
		for (Module module : Module.getModules())
			module.onCleanup(params.bot, callback, params.sender);
	}
}
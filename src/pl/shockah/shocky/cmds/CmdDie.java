package pl.shockah.shocky.cmds;

import pl.shockah.shocky.Shocky;

public class CmdDie extends Command {
	public String command() {return "die";}
	public String help(Parameters params) {
		return "[r:controller] die - shutdowns the bot";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		
		if (params.tokenCount == 0)
			Shocky.die();
		else Shocky.die(params.getParams(1));
	}
}
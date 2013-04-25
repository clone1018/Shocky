package pl.shockah.shocky.cmds;

import java.util.concurrent.TimeUnit;

import pl.shockah.shocky.Shocky;

public class CmdSave extends Command {
	public String command() {return "save";}
	public String help(Parameters params) {
		return "[r:controller] save - saves the data";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		callback.type = EType.Notice;
		Shocky.dataSave();
		callback.append("Saved. Next periodic save in ");
		long delay = Shocky.nextSave(TimeUnit.MINUTES);
		callback.append(delay);
		callback.append(" minute");
		if (delay != 1)
			callback.append('s');
		callback.append('.');
	}
}
package pl.shockah.shocky.cmds;

import pl.shockah.Config;
import pl.shockah.shocky.Data;

public class CmdGet extends Command {
	public String command() {return "get";}
	public String help(Parameters params) {
		return "[r:controller] get {key} - gets a bot option value";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		callback.type = EType.Notice;
		if (params.tokenCount < 1 || params.tokenCount > 2) {
			callback.append(help(params));
			return;
		}
		
		String key = params.nextParam();
		boolean global = false;
		if (key.equals(".")) {
			global = true;
			key = params.nextParam();
		}

		if (global) params.checkController();
		else params.checkAny();
		
		Config config;
		if (global)
			config = Data.config;
		else {
			if (key != null && Data.protectedKeys.contains(key)) {
				callback.append("Key ").append(key).append(" is protected");
				return;
			}
			config = Data.forChannel(params.channel);
		}
		
		if (key != null) {
			callback.append(key).append(": ").append(config.getString(key));
			return;
		}
	}
}
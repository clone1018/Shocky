package pl.shockah.shocky.cmds;

import pl.shockah.Config;
import pl.shockah.shocky.Data;

public class CmdSet extends Command {
	public String command() {return "set";}
	public String help(Parameters params) {
		return "[r:controller] set {key} {value} - sets a bot option value";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		callback.type = EType.Notice;
		if (!params.hasMoreParams()) {
			callback.append(help(params));
			return;
		}
		
		String key = params.nextParam();
		boolean global = false;
		if (key.equals(".")) {
			global = true;
			if (!params.hasMoreParams()) {
				callback.append(help(params));
				return;
			}
			key = params.nextParam();
		}
		String value = params.getParams(0);

		if (global) params.checkController();
		else params.checkAny();
		
		Config config;
		if (global)
			config = Data.config;
		else {
			if (key != null && Data.protectedKeys.contains(key)) {
				callback.append("Key "+key+" is protected");
				return;
			}
			config = Data.forChannel(params.channel);
		}
		
		if (key != null && !value.isEmpty()) {
			config.set(key,value);
			callback.append("Set ").append(key).append(" to ").append(value);
			return;
		} else if (!global && key != null) {
			value = config.getString(key);
			config.remove(key);
			callback.append("Unset ").append(key).append(". Was ").append(value);
			return;
		}
		
		callback.append(help(params));
	}
}
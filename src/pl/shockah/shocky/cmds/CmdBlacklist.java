package pl.shockah.shocky.cmds;

import pl.shockah.shocky.Data;

public class CmdBlacklist extends Command {
	public String command() {return "blacklist";}
	public String help(Parameters params) {
		StringBuilder sb = new StringBuilder();
		sb.append("[r:controller] blacklist - list blacklisted nicks\n");
		sb.append("[r:controller] blacklist add {nick} - adds a new entry\n");
		sb.append("[r:controller] blacklist remove {nick} - removes an entry");
		return sb.toString();
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		callback.type = EType.Notice;
		
		if (params.tokenCount == 0) {
			StringBuilder sb = new StringBuilder();
			for (String blacklisted : Data.blacklistNicks) {
				if (sb.length() != 0) sb.append(", ");
				sb.append(blacklisted);
			}
			callback.append(sb);
			return;
		}
		
		if (params.tokenCount == 2) {
			String method = params.nextParam().toLowerCase();
			String target = params.nextParam().toLowerCase();
			if (method.equals("add")) {
				if (Data.blacklistNicks.contains(target)) {
					callback.append(target).append(" is already in blacklist");
				} else {
					Data.blacklistNicks.add(target);
					callback.append("Added");
				}
				return;
			} else if (method.equals("remove")) {
				if (!Data.blacklistNicks.contains(target)) {
					callback.append(target).append(" isn't in blacklist");
				} else {
					Data.blacklistNicks.remove(target);
					callback.append("Removed");
				}
				return;
			}
		}
		
		callback.append(help(params));
	}
}
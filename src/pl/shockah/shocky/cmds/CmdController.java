package pl.shockah.shocky.cmds;

import org.pircbotx.User;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public class CmdController extends Command {
	public String command() {return "controller";}
	public String help(Parameters params) {
		StringBuilder sb = new StringBuilder();
		sb.append("[r:controller] controller - list controllers of the bot\n");
		sb.append("[r:controller] controller add {nick/login} - adds a new controller\n");
		sb.append("[r:controller] controller remove {nick/login} - removes a controller");
		return sb.toString();
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		callback.type = EType.Notice;
		
		if (params.tokenCount == 0) {
			StringBuilder sb = new StringBuilder();
			for (String controller : Data.controllers) {
				if (sb.length() != 0) sb.append(", ");
				sb.append(controller);
			}
			callback.append(sb);
			return;
		}
		
		if (params.tokenCount == 2) {
			String method = params.nextParam().toLowerCase();
			String username = params.nextParam();
			User user = Shocky.getUser(username);
			if (!Data.controllers.contains(username) && user != null) {
				if (Shocky.getLogin(user) == null) {
					callback.append(username+" isn't identified");
					return;
				}
				username = Shocky.getLogin(user);
			}
			
			if (method.equals("add")) {
				if (Data.controllers.contains(username)) {
					callback.append(username+" is already a controller");
				} else {
					Data.controllers.add(username);
					callback.append("Added");
				}
				return;
			} else if (method.equals("remove")) {
				if (!Data.controllers.contains(username)) {
					callback.append(username+" isn't a controller");
				} else {
					Data.controllers.remove(username);
					callback.append("Removed");
				}
				return;
			}
		}
		
		callback.append(help(params));
	}
}
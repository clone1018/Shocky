package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public class CmdController extends Command {
	public String command() {return "controller";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		StringBuilder sb = new StringBuilder();
		sb.append("[r:controller] controller - list controllers of the bot\n");
		sb.append("[r:controller] controller add {nick/login} - adds a new controller\n");
		sb.append("[r:controller] controller remove {nick/login} - removes a controller");
		return sb.toString();
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command()) || cmd.equals("ctrl");}
	
	public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		String[] args = message.split(" ");
		
		if (args.length == 1) {
			StringBuilder sb = new StringBuilder();
			for (String controller : Data.controllers) {
				if (sb.length() != 0) sb.append(", ");
				sb.append(controller);
			}
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,sb.toString());
			return;
		}
		
		if (args.length == 3) {
			String s = args[2];
			User u = Shocky.getUser(args[2]);
			if (!Data.controllers.contains(s) && u != null) {
				if (Shocky.getLogin(u) == null) {
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,s+" isn't identified");
					return;
				}
				s = Shocky.getLogin(u);
			}
			
			if (args[1].toLowerCase().equals("add")) {
				if (Data.controllers.contains(s)) {
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,s+" is already a controller");
				} else {
					Data.controllers.add(s);
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,"Added");
				}
				return;
			} else if (args[1].toLowerCase().equals("remove")) {
				if (!Data.controllers.contains(s)) {
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,s+" isn't a controller");
				} else {
					Data.controllers.remove(s);
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,"Removed");
				}
				return;
			}
		}
		
		Shocky.sendNotice(bot,sender,help(bot,type,channel,sender));
	}
}
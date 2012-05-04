package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;

public class CmdBlacklist extends Command {
	public String command() {return "blacklist";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		StringBuilder sb = new StringBuilder();
		sb.append("[r:controller] blacklist - list blacklisted nicks\n");
		sb.append("[r:controller] blacklist add {nick} - adds a new entry\n");
		sb.append("[r:controller] blacklist remove {nick} - removes an entry");
		return sb.toString();
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
		if (!canUseController(bot,type,sender)) return;
		String[] args = message.split(" ");
		
		if (args.length == 1) {
			StringBuilder sb = new StringBuilder();
			for (String blacklisted : Data.getBlacklistNicks()) {
				if (sb.length() != 0) sb.append(", ");
				sb.append(blacklisted);
			}
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,sb.toString());
			return;
		}
		
		if (args.length == 3) {
			String s = args[2].toLowerCase();
			if (args[1].toLowerCase().equals("add")) {
				if (Data.getBlacklistNicks().contains(s)) {
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,s+" is already in blacklist");
				} else {
					Data.getBlacklistNicks().add(s);
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,"Added");
				}
				return;
			} else if (args[1].toLowerCase().equals("remove")) {
				if (!Data.getBlacklistNicks().contains(s)) {
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,s+" isn't in blacklist");
				} else {
					Data.getBlacklistNicks().remove(s);
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,"Removed");
				}
				return;
			}
		}
		
		Shocky.sendNotice(bot,sender,help(bot,type,channel,sender));
	}
}
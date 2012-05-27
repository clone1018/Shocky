package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.shocky.MultiChannel;

public class CmdPart extends Command {
	public String command() {return "part";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
		StringBuilder sb = new StringBuilder();
		
		if (type == EType.Channel) sb.append("[r:op|controller] part - makes the bot part current channel\n");
		sb.append("[r:op|controller] part {channel} - makes the bot part channel\n");
		if (isController(bot,type,sender)) sb.append("[r:controller] part all - makes the bot part all channels");
		
		return sb.toString();
	}
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		if (args.length == 1) {
			if (type == EType.Channel) {
				if (!canUseAny(bot,type,channel,sender)) return;
				try {
					MultiChannel.part(channel.getName());
				} catch (Exception e) {}
				return;
			}
		} else if (args[1].equals("all")) {
			if (!canUseController(bot,type,sender)) return;
			
			try {
				MultiChannel.part(new String[0]);
			} catch (Exception e) {}
			return;
		} else if (args.length == 2) {
			Channel c = MultiChannel.get(args[1]);
			if (c == null) {
				callback.append("Not in channel "+args[1]);
				return;
			}
			
			if (!canUseAny(bot,type,c,sender)) return;
			try {
				MultiChannel.part(args[1]);
			} catch (Exception e) {}
			return;
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}
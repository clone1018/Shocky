package pl.shockah.shocky.cmds;

import org.pircbotx.Channel;
import pl.shockah.shocky.MultiChannel;

public class CmdPart extends Command {
	public String command() {return "part";}
	public String help(Parameters params) {
		StringBuilder sb = new StringBuilder();
		
		if (params.type == EType.Channel)
			sb.append("[r:op|controller] part - makes the bot part current channel\n");
		sb.append("[r:op|controller] part {channel} - makes the bot part channel\n");
		if (params.isController())
			sb.append("[r:controller] part all - makes the bot part all channels");
		
		return sb.toString();
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		callback.type = EType.Notice;
		if (params.tokenCount == 0) {
			if (params.type == EType.Channel) {
				params.checkAny();
				try {
					MultiChannel.part(params.channel.getName());
				} catch (Exception e) {e.printStackTrace();}
				return;
			}
		} else if (params.tokenCount >= 1) {
			params.checkController();
			String channel = params.nextParam();
			
			if (channel.equalsIgnoreCase("all")) {
				try {
					MultiChannel.part(new String[0]);
				} catch (Exception e) {e.printStackTrace();}
				return;
			} else {
				Channel c = MultiChannel.get(channel);
				if (c == null) {
					callback.append("Not in channel ").append(channel);
					return;
				}
				try {
					MultiChannel.part(channel);
				} catch (Exception e) {e.printStackTrace();}
				return;
			}
		}
		
		callback.append(help(params));
	}
}
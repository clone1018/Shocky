package pl.shockah.shocky;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import pl.shockah.Reflection;

public class MultiChannel {
	protected static String channelPrefixes;
	
	public static Channel get(String name) {
		name = name.toLowerCase();
		for (PircBotX bot : Shocky.getBots()) {
			if (bot.channelExists(name)) return bot.getChannel(name);
		}
		return null;
	}
	
	public static void join(String channel) throws Exception {
		channel = channel.toLowerCase();
		for (PircBotX bot : Shocky.getBots()) {
			if (bot.channelExists(channel)) throw new Exception("Already in channel "+channel);
		}
		for (PircBotX bot : Shocky.getBots()) {
			if (bot.getChannelsNames().size() < Data.config.getInt("main-maxchannels")) {
				bot.joinChannel(channel);
				if (!Data.getChannels().contains(channel)) Data.getChannels().add(channel);
				return;
			}
		}
		
		try {
			PircBotX bot = Shocky.getBotManager().createBot(Data.config.getString("main-server"));
			bot.setVersion(Data.config.getString("main-version"));
			bot.connect(Data.config.getString("main-server"));
			if (!Data.config.getString("main-nickservpass").isEmpty()) bot.identify(Data.config.getString("main-nickservpass"));
			channelPrefixes = Reflection.getPrivateValue(PircBotX.class,"_channelPrefixes",bot);
			join(channel);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public static void part(String channel) throws Exception {
		if (channel != null) channel = channel.toLowerCase();
		for (PircBotX bot : Shocky.getBots()) {
			if (channel == null) {
				for (Channel c : bot.getChannels()) bot.partChannel(c);
			} else if (bot.getChannelsNames().contains(channel)) {
				bot.partChannel(bot.getChannel(channel));
				Data.getChannels().remove(channel);
				return;
			}
		}
		if (channel == null) Data.getChannels().clear();
		throw new Exception("Not in channel "+channel);
	}
}